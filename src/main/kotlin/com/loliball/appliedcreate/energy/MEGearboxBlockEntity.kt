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
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.math.VecHelper
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.abs

/**
 * ME Gearbox — a dual-network block joining AE2 ME network and Create kinetic network.
 *
 * Two modes (toggled via shift+wrench):
 * - EXPORT mode: Extracts stress from AE2 ME storage → generates rotation in Create kinetic network
 *   (acts as GeneratingKineticBlockEntity, getGeneratedSpeed() returns configured RPM)
 * - IMPORT mode: Consumes rotation from Create kinetic network → inserts stress into AE2 ME storage
 *   (getGeneratedSpeed() returns 0, stress is consumed via calculateStressApplied())
 *
 * Stress multiplier (1x-16x) is adjustable via Create-style scroll wheel on perpendicular faces.
 * Multiplier scales stress capacity/impact AND ME transfer rate.
 *
 * The ME network side uses StorageHelper.poweredExtraction/poweredInsert with StressKey.INSTANCE.
 */
class MEGearboxBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : NetworkedGeneratingKineticBlockEntity(type, pos, state) {

    companion object {
        /** Default RPM for export mode */
        const val GENERATED_SPEED = 32
        /** Base stress capacity provided per RPM when exporting */
        const val BASE_STRESS_CAPACITY_PER_RPM = 64.0f
        /** Base stress impact per RPM when importing */
        const val BASE_STRESS_IMPACT_PER_RPM = 64.0f
        /** Base stress units transferred to/from ME per tick at 256 RPM */
        const val BASE_STRESS_TRANSFER_PER_256_RPM = 16384L
        /** Max stress multiplier */
        const val MAX_MULTIPLIER = 16
    }

    enum class Mode {
        EXPORT,  // ME → Kinetic (generates rotation)
        IMPORT   // Kinetic → ME (consumes rotation)
    }

    var mode: Mode = Mode.EXPORT
        private set

    /** Whether ME network actually has stress to supply in EXPORT mode */
    private var hasStressSupply = false

    /** Scroll-controlled stress multiplier (1x to 16x) */
    lateinit var stressMultiplier: ScrollValueBehaviour

    init {
        mainNode.setVisualRepresentation(AppliedCreate.ME_GEARBOX_ITEM.get())
        mainNode.setIdlePowerUsage(2.0)
        mainNode.setFlags(GridFlags.REQUIRE_CHANNEL)
    }

    // ── Behaviours ──

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        stressMultiplier = ScrollValueBehaviour(
            Component.translatable("appliedcreate.me_gearbox.multiplier"),
            this,
            GearboxValueBoxTransform()
        )
        stressMultiplier.between(1, MAX_MULTIPLIER)
        stressMultiplier.value = 1
        stressMultiplier.withCallback { _ ->
            if (mode == Mode.EXPORT) {
                updateGeneratedRotation()
            }
            notifyUpdate()
        }
        behaviours.add(stressMultiplier)
    }

    /** Get the current multiplier value */
    fun getMultiplier(): Int = if (::stressMultiplier.isInitialized) stressMultiplier.value else 1

    // ── Mode switching (shift+wrench) ──

    fun toggleMode() {
        if (level == null || level!!.isClientSide) return

        if (mode == Mode.EXPORT) {
            mode = Mode.IMPORT
            hasStressSupply = false
            updateGeneratedRotation() // getGeneratedSpeed() now returns 0
        } else {
            mode = Mode.EXPORT
            hasStressSupply = false  // will be re-evaluated on next tick
            updateGeneratedRotation() // getGeneratedSpeed() now returns configured RPM
        }
        notifyUpdate()
    }

    // ── Grid connectivity: expose on sides perpendicular to shaft ──

    override fun getGridConnectableSides(orientation: BlockOrientation): Set<Direction> {
        val shaft = blockState.getValue(DirectionalKineticBlock.FACING)
        return EnumSet.complementOf(EnumSet.of(shaft, shaft.opposite))
    }

    override fun initialize() {
        super.initialize()
        exposeSides()
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed()) {
            updateGeneratedRotation()
        }
    }

    // ── GeneratingKineticBlockEntity: getGeneratedSpeed ──

    override fun getGeneratedSpeed(): Float {
        if (mode == Mode.IMPORT) return 0f
        if (!hasStressSupply) return 0f
        // Export mode: generate rotation at fixed speed
        val facing = blockState.getValue(DirectionalKineticBlock.FACING)
        return convertToDirection(GENERATED_SPEED.toFloat(), facing)
    }

    // ── Stress ──

    override fun calculateAddedStressCapacity(): Float {
        if (mode == Mode.EXPORT && hasStressSupply) {
            // As a generator, provide stress capacity scaled by multiplier
            return BASE_STRESS_CAPACITY_PER_RPM * getMultiplier() * abs(GENERATED_SPEED)
        }
        return 0f
    }

    override fun calculateStressApplied(): Float {
        if (mode == Mode.IMPORT) {
            // As a consumer, apply stress impact scaled by multiplier
            val impact = BASE_STRESS_IMPACT_PER_RPM * getMultiplier()
            this.lastStressApplied = impact
            return impact
        }
        this.lastStressApplied = 0f
        return 0f
    }

    /** Effective transfer rate per tick, accounting for multiplier and RPM */
    private fun getTransferRate(rpm: Float): Long {
        return (BASE_STRESS_TRANSFER_PER_256_RPM * getMultiplier() * (rpm / 256.0)).toLong()
    }

    // ── Tick: Transfer stress between ME and kinetic networks ──

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

    private fun tickExport(
        energy: appeng.api.networking.energy.IEnergySource,
        storage: appeng.api.storage.MEStorage,
        actionSource: IActionSource
    ) {
        // Simulate extraction first to check if stress is available
        val rpm = abs(speed)
        val stressNeeded = if (rpm == 0f) {
            // When not spinning yet, check if we can extract at base RPM
            getTransferRate(GENERATED_SPEED.toFloat())
        } else {
            getTransferRate(rpm)
        }
        if (stressNeeded <= 0) {
            if (hasStressSupply) {
                hasStressSupply = false
                updateGeneratedRotation()
            }
            return
        }

        val simulated = StorageHelper.poweredExtraction(
            energy, storage, StressKey.INSTANCE, stressNeeded, actionSource, Actionable.SIMULATE
        )
        val wasSupplied = hasStressSupply
        hasStressSupply = simulated > 0

        if (hasStressSupply != wasSupplied) {
            updateGeneratedRotation()
        }

        if (hasStressSupply && rpm > 0f) {
            // Actually extract stress
            StorageHelper.poweredExtraction(
                energy, storage, StressKey.INSTANCE, stressNeeded, actionSource, Actionable.MODULATE
            )
        }
    }

    private fun tickImport(
        energy: appeng.api.networking.energy.IEnergySource,
        storage: appeng.api.storage.MEStorage,
        actionSource: IActionSource
    ) {
        // Consume kinetic stress and insert into ME storage
        val rpm = abs(speed)
        if (rpm == 0f) return

        val stressToInsert = getTransferRate(rpm)
        if (stressToInsert <= 0) return

        StorageHelper.poweredInsert(
            energy, storage, StressKey.INSTANCE, stressToInsert, actionSource, Actionable.MODULATE
        )
    }

    // ── NBT ──

    override fun write(compound: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
        super.write(compound, registries, clientPacket)
        compound.putString("GearboxMode", mode.name)
    }

    override fun read(compound: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
        super.read(compound, registries, clientPacket)
        mode = try {
            Mode.valueOf(compound.getString("GearboxMode"))
        } catch (e: IllegalArgumentException) {
            Mode.EXPORT
        }
    }

    // ── Goggle Tooltip ──

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        val modeKey = if (mode == Mode.EXPORT) "appliedcreate.me_gearbox.mode.export" else "appliedcreate.me_gearbox.mode.import"
        CreateLang.text("")
            .add(Component.translatable("appliedcreate.me_gearbox.title"))
            .style(ChatFormatting.GOLD)
            .forGoggles(tooltip)
        CreateLang.text("")
            .add(Component.translatable("appliedcreate.me_gearbox.mode"))
            .style(ChatFormatting.GRAY)
            .add(CreateLang.text("").add(Component.translatable(modeKey)).style(ChatFormatting.AQUA))
            .forGoggles(tooltip, 1)

        // Show multiplier
        CreateLang.text("")
            .add(Component.translatable("appliedcreate.me_gearbox.multiplier"))
            .style(ChatFormatting.GRAY)
            .add(CreateLang.text(" ${getMultiplier()}x").style(ChatFormatting.WHITE))
            .forGoggles(tooltip, 1)

        if (mode == Mode.EXPORT) {
            val capacity = calculateAddedStressCapacity()
            val stressTotal = abs(capacity * speed)
            CreateLang.translate("gui.goggles.generator_stats")
                .forGoggles(tooltip)
            CreateLang.translate("tooltip.capacityProvided")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip)
            CreateLang.number(stressTotal.toDouble())
                .translate("generic.unit.stress")
                .style(ChatFormatting.AQUA)
                .space()
                .add(CreateLang.translate("gui.goggles.at_current_speed")
                    .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1)
        } else {
            val stressTotal = lastStressApplied * abs(speed)
            CreateLang.translate("gui.goggles.kinetic_stats")
                .forGoggles(tooltip)
            CreateLang.translate("tooltip.stressImpact")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip)
            CreateLang.number(stressTotal.toDouble())
                .translate("generic.unit.stress")
                .style(ChatFormatting.AQUA)
                .space()
                .add(CreateLang.translate("gui.goggles.at_current_speed")
                    .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1)
        }

        // Show stress transfer rate with +/- sign
        val rpm = abs(speed)
        val transferRate = getTransferRate(rpm)
        val sign = if (mode == Mode.IMPORT) "+" else "-"
        CreateLang.text("")
            .add(Component.translatable("appliedcreate.me_gearbox.transfer"))
            .style(ChatFormatting.GRAY)
            .add(CreateLang.text("$sign")
                .add(CreateLang.number(transferRate.toDouble()))
                .add(Component.translatable("appliedcreate.me_gearbox.transfer.unit"))
                .style(ChatFormatting.GOLD))
            .forGoggles(tooltip, 1)

        return true
    }

    // ── Value Box Transform: show scroll value on perpendicular faces ──

    private inner class GearboxValueBoxTransform : ValueBoxTransform.Sided() {

        override fun getSouthLocation(): Vec3 {
            return VecHelper.voxelSpace(8.0, 8.0, 15.5)
        }

        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            // Only show on sides perpendicular to the shaft axis
            val shaft = state.getValue(DirectionalKineticBlock.FACING)
            return direction.axis != shaft.axis
        }

        override fun getScale(): Float = 0.5f
    }
}
