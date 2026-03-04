package com.loliball.appliedcreate.energy

import appeng.api.config.Actionable
import appeng.api.networking.security.IActionSource
import appeng.api.orientation.BlockOrientation
import appeng.api.networking.GridFlags
import appeng.api.storage.StorageHelper
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.storage.StressKey
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import net.createmod.catnip.lang.LangBuilder
import net.createmod.catnip.lang.LangNumberFormat
import net.createmod.catnip.math.VecHelper
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.abs

class MEGearboxBlockEntity(
    type: BlockEntityType<*>, pos: BlockPos, state: BlockState
) : NetworkedGeneratingKineticBlockEntity(type, pos, state) {

    companion object {
        const val GENERATED_SPEED = 32
        const val BASE_STRESS_CAPACITY_PER_RPM = 64.0f
        const val BASE_STRESS_IMPACT_PER_RPM = 64.0f
        const val BASE_STRESS_TRANSFER_PER_256_RPM = 16384L
        const val MAX_MULTIPLIER = 16
    }

    enum class Mode { EXPORT, IMPORT }

    var mode: Mode = Mode.EXPORT
        private set
    private var hasStressSupply = false
    lateinit var stressMultiplier: ScrollValueBehaviour

    init {
        mainNode.setVisualRepresentation(AppliedCreate.ME_GEARBOX_ITEM.get())
        mainNode.setIdlePowerUsage(2.0)
        mainNode.setFlags(GridFlags.REQUIRE_CHANNEL)
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        stressMultiplier = ScrollValueBehaviour(
            Component.translatable("appliedcreate.me_gearbox.multiplier"), this, GearboxValueBoxTransform()
        )
        stressMultiplier.between(1, MAX_MULTIPLIER)
        stressMultiplier.value = 1
        stressMultiplier.withCallback { _ ->
            if (mode == Mode.EXPORT) {
                updateGeneratedRotation()
            } else if (mode == Mode.IMPORT && hasNetwork()) {
                // In IMPORT mode, updateGeneratedRotation() won't update stress
                // because getGeneratedSpeed() returns 0. Directly update the network.
                val network = getOrCreateNetwork()
                network.updateStressFor(this, calculateStressApplied())
                network.updateStress()
            }
            notifyUpdate()
        }
        behaviours.add(stressMultiplier)
    }

    fun getMultiplier(): Int = if (::stressMultiplier.isInitialized) stressMultiplier.value else 1

    fun toggleMode() {
        if (level == null || level!!.isClientSide) return
        if (mode == Mode.EXPORT) {
            mode = Mode.IMPORT; hasStressSupply = false; updateGeneratedRotation()
        } else {
            mode = Mode.EXPORT; hasStressSupply = false; updateGeneratedRotation()
        }
        notifyUpdate()
    }

    override fun getGridConnectableSides(orientation: BlockOrientation): Set<Direction> {
        val shaft = blockState.getValue(DirectionalKineticBlock.FACING)
        return EnumSet.complementOf(EnumSet.of(shaft, shaft.opposite))
    }

    override fun initialize() {
        super.initialize(); exposeSides()
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) updateGeneratedRotation()
    }

    override fun getGeneratedSpeed(): Float {
        if (mode == Mode.IMPORT) return 0f
        if (!hasStressSupply) return 0f
        val facing = blockState.getValue(DirectionalKineticBlock.FACING)
        return convertToDirection(GENERATED_SPEED.toFloat(), facing)
    }

    override fun calculateAddedStressCapacity(): Float {
        if (mode == Mode.EXPORT && hasStressSupply) return BASE_STRESS_CAPACITY_PER_RPM * getMultiplier() * abs(GENERATED_SPEED)
        return 0f
    }

    override fun calculateStressApplied(): Float {
        if (mode == Mode.IMPORT) {
            val impact = BASE_STRESS_IMPACT_PER_RPM * getMultiplier()
            this.lastStressApplied = impact; return impact
        }
        this.lastStressApplied = 0f; return 0f
    }

    private fun getTransferRate(rpm: Float): Long =
        (BASE_STRESS_TRANSFER_PER_256_RPM * getMultiplier() * (rpm / 256.0)).toLong()

    override fun tick() {
        super.tick()
        if (level == null || level!!.isClientSide) return
        val grid = mainNode.grid ?: return
        val storage = grid.storageService?.inventory ?: return
        val energy = grid.energyService ?: return
        val actionSource = IActionSource.ofMachine(mainNode::getNode)
        when (mode) {
            Mode.EXPORT -> tickExport(energy, storage, actionSource)
            Mode.IMPORT -> tickImport(energy, storage, actionSource)
        }
    }

    private fun tickExport(energy: appeng.api.networking.energy.IEnergySource, storage: appeng.api.storage.MEStorage, actionSource: IActionSource) {
        val rpm = abs(speed)
        val stressNeeded = if (rpm == 0f) getTransferRate(GENERATED_SPEED.toFloat()) else getTransferRate(rpm)
        if (stressNeeded <= 0) { if (hasStressSupply) { hasStressSupply = false; updateGeneratedRotation() }; return }
        val simulated = StorageHelper.poweredExtraction(energy, storage, StressKey.INSTANCE, stressNeeded, actionSource, Actionable.SIMULATE)
        val wasSupplied = hasStressSupply; hasStressSupply = simulated > 0
        if (hasStressSupply != wasSupplied) updateGeneratedRotation()
        if (hasStressSupply && rpm > 0f) StorageHelper.poweredExtraction(energy, storage, StressKey.INSTANCE, stressNeeded, actionSource, Actionable.MODULATE)
    }

    private fun tickImport(energy: appeng.api.networking.energy.IEnergySource, storage: appeng.api.storage.MEStorage, actionSource: IActionSource) {
        val rpm = abs(speed); if (rpm == 0f) return
        val stressToInsert = getTransferRate(rpm); if (stressToInsert <= 0) return
        StorageHelper.poweredInsert(energy, storage, StressKey.INSTANCE, stressToInsert, actionSource, Actionable.MODULATE)
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket); compound.putString("GearboxMode", mode.name)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        mode = try { Mode.valueOf(compound.getString("GearboxMode")) } catch (e: IllegalArgumentException) { Mode.EXPORT }
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        val modeKey = if (mode == Mode.EXPORT) "appliedcreate.me_gearbox.mode.export" else "appliedcreate.me_gearbox.mode.import"
        LangBuilder("create").text("").add(Component.translatable("appliedcreate.me_gearbox.title")).style(ChatFormatting.GOLD).forGoggles(tooltip)
        LangBuilder("create").text("").add(Component.translatable("appliedcreate.me_gearbox.mode")).style(ChatFormatting.GRAY)
            .add(LangBuilder("create").text("").add(Component.translatable(modeKey)).style(ChatFormatting.AQUA)).forGoggles(tooltip, 1)
        LangBuilder("create").text("").add(Component.translatable("appliedcreate.me_gearbox.multiplier")).style(ChatFormatting.GRAY)
            .add(LangBuilder("create").text(" ${getMultiplier()}x").style(ChatFormatting.WHITE)).forGoggles(tooltip, 1)
        if (mode == Mode.EXPORT) {
            val capacity = calculateAddedStressCapacity(); val stressTotal = abs(capacity * speed)
            LangBuilder("create").translate("gui.goggles.generator_stats").forGoggles(tooltip)
            LangBuilder("create").translate("tooltip.capacityProvided").style(ChatFormatting.GRAY).forGoggles(tooltip)
            LangBuilder("create").text(LangNumberFormat.format(stressTotal.toDouble())).translate("generic.unit.stress").style(ChatFormatting.AQUA).space()
                .add(LangBuilder("create").translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1)
        } else {
            val stressTotal = lastStressApplied * abs(speed)
            LangBuilder("create").translate("gui.goggles.kinetic_stats").forGoggles(tooltip)
            LangBuilder("create").translate("tooltip.stressImpact").style(ChatFormatting.GRAY).forGoggles(tooltip)
            LangBuilder("create").text(LangNumberFormat.format(stressTotal.toDouble())).translate("generic.unit.stress").style(ChatFormatting.AQUA).space()
                .add(LangBuilder("create").translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1)
        }
        val rpm = abs(speed); val transferRate = getTransferRate(rpm)
        val sign = if (mode == Mode.IMPORT) "+" else "-"
        LangBuilder("create").text("").add(Component.translatable("appliedcreate.me_gearbox.transfer")).style(ChatFormatting.GRAY)
            .add(LangBuilder("create").text("$sign").add(LangBuilder("create").text(LangNumberFormat.format(transferRate.toDouble())))
                .add(Component.translatable("appliedcreate.me_gearbox.transfer.unit")).style(ChatFormatting.GOLD))
            .forGoggles(tooltip, 1)
        return true
    }

    private inner class GearboxValueBoxTransform : ValueBoxTransform.Sided() {
        override fun getSouthLocation(): Vec3 = VecHelper.voxelSpace(8.0, 8.0, 15.5)
        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            val shaft = state.getValue(DirectionalKineticBlock.FACING)
            return direction.axis != shaft.axis
        }
        override fun getScale(): Float = 0.5f
    }
}
