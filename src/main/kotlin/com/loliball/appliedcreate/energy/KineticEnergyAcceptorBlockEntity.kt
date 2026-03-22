package com.loliball.appliedcreate.energy

import appeng.api.config.Actionable
import appeng.api.networking.energy.IPassiveEnergyGenerator
import appeng.api.orientation.BlockOrientation
import com.google.common.collect.ImmutableList
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.config.ACConfig
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.math.VecHelper
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.abs

class KineticEnergyAcceptorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : NetworkedKineticBlockEntity(type, pos, state) {

    companion object {
        fun getAePer256Rpm(): Double = ACConfig.SERVER.kineticAePer256Rpm.get()
        fun getBaseStressSu(): Int = ACConfig.SERVER.kineticBaseStressSu.get()
        fun getMaxMultiplier(): Int = ACConfig.SERVER.kineticMaxMultiplier.get()
    }

    private var aeValue: Double = 0.0

    lateinit var stressMultiplier: ScrollValueBehaviour

    init {
        mainNode.setVisualRepresentation(AppliedCreate.KINETIC_ENERGY_ACCEPTOR_BLOCK.asItem())
        mainNode.setIdlePowerUsage(0.0)
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        val label = Component.translatable("appliedcreate.kinetic_energy_acceptor.multiplier")
        stressMultiplier = AcceptorScrollValueBehaviour(label, this, AcceptorValueBoxTransform())
        stressMultiplier.between(0, getMaxMultiplier())
        stressMultiplier.withFormatter { v -> "${v}x" }
        stressMultiplier.value = 1
        stressMultiplier.withCallback { _ ->
            if (hasNetwork()) {
                val network = getOrCreateNetwork()
                network.updateStressFor(this, calculateStressApplied())
                network.updateStress()
            }
            notifyUpdate()
        }
        behaviours.add(stressMultiplier)
    }

    fun getMultiplier(): Int = if (::stressMultiplier.isInitialized) stressMultiplier.value else 1

    override fun getGridConnectableSides(orientation: BlockOrientation): Set<Direction> {
        val shaft = blockState.getValue(DirectionalKineticBlock.FACING)
        return EnumSet.complementOf(EnumSet.of(shaft, shaft.opposite))
    }

    override fun initialize() {
        super.initialize()
        exposeSides()
    }

    override fun calculateStressApplied(): Float {
        val impact = (getBaseStressSu() / 256f) * getMultiplier()
        this.lastStressApplied = impact
        return impact
    }

    override fun tick() {
        super.tick()

        val newAE = getAePer256Rpm() * getMultiplier() * (abs(speed.toInt()) / 256.0)
        if (newAE.compareTo(aeValue) != 0) {
            aeValue = newAE
            mainNode.ifPresent { grid, node -> grid.tickManager.wakeDevice(node) }
        }

        if (newAE != 0.0) {
            mainNode.grid?.energyService?.injectPower(newAE, Actionable.MODULATE)
        }
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        CreateLang.translate("gui.goggles.generator_stats")
            .forGoggles(tooltip)
        CreateLang.translate("tooltip.capacityProvided")
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip)
        CreateLang.number(aeValue)
            .text(" AE/t ")
            .style(ChatFormatting.GOLD)
            .space()
            .add(CreateLang.translate("gui.goggles.at_current_speed")
                .style(ChatFormatting.DARK_GRAY))
            .forGoggles(tooltip, 1)
        CreateLang.translate("gui.goggles.kinetic_stats")
            .forGoggles(tooltip)
        CreateLang.translate("tooltip.stressImpact")
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip)
        val stressTotal = lastStressApplied * abs(speed)
        CreateLang.number(stressTotal.toDouble())
            .translate("generic.unit.stress")
            .style(ChatFormatting.AQUA)
            .space()
            .add(CreateLang.translate("gui.goggles.at_current_speed")
                .style(ChatFormatting.DARK_GRAY))
            .forGoggles(tooltip, 1)
        return true
    }

    private class AcceptorScrollValueBehaviour(
        label: Component,
        be: SmartBlockEntity,
        slot: ValueBoxTransform
    ) : ScrollValueBehaviour(label, be, slot) {

        override fun createBoard(player: Player, hitResult: BlockHitResult): ValueSettingsBoard {
            return ValueSettingsBoard(
                label, max, 1,
                ImmutableList.of(label),
                ValueSettingsFormatter { settings -> CreateLang.number(settings.value().toDouble()).text("x").component() }
            )
        }
    }

    private inner class AcceptorValueBoxTransform : ValueBoxTransform.Sided() {

        override fun getSouthLocation(): Vec3 {
            return VecHelper.voxelSpace(8.0, 8.0, 15.5)
        }

        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            val shaft = state.getValue(DirectionalKineticBlock.FACING)
            return direction.axis != shaft.axis
        }

        override fun getScale(): Float = 0.5f
    }
}
