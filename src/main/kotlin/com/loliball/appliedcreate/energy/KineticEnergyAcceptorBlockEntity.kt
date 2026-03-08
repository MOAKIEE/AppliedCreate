package com.loliball.appliedcreate.energy

import appeng.api.networking.energy.IPassiveEnergyGenerator
import appeng.api.orientation.BlockOrientation
import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.foundation.utility.CreateLang
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import java.util.EnumSet
import kotlin.math.abs

/**
 * Kinetic Energy Acceptor — receives Create rotational kinetic energy (SU) and converts
 * it into AE2 network energy (AE/t) via IPassiveEnergyGenerator.
 *
 * Conversion formula: AE/t = AE_PER_256_RPM * (|RPM| / 256)
 * Stress impact: MAX_STRESS_SU / 256f
 *
 * Grid exposed on sides perpendicular to the shaft axis (cable connects to sides, not shaft ends).
 */
class KineticEnergyAcceptorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : NetworkedKineticBlockEntity(type, pos, state) {

    companion object {
        /** AE/t generated at 256 RPM */
        const val AE_PER_256_RPM = 640.0
        /** Stress impact (SU) at 256 RPM */
        const val MAX_STRESS_SU = 16384
    }

    private var aeValue: Double = 0.0

    init {
        mainNode.setVisualRepresentation(AppliedCreate.KINETIC_ENERGY_ACCEPTOR_BLOCK.asItem())
        mainNode.setIdlePowerUsage(0.0)

        val passiveGenerator = object : IPassiveEnergyGenerator {
            override fun getRate(): Double = aeValue

            override fun setSuppressed(suppressed: Boolean) {}

            override fun isSuppressed(): Boolean {
                return this@KineticEnergyAcceptorBlockEntity.level == null
                    || this@KineticEnergyAcceptorBlockEntity.level!!.isClientSide
                    || abs(speed) == 0f
            }
        }

        mainNode.addService(IPassiveEnergyGenerator::class.java, passiveGenerator)
    }

    // ── Grid connectivity: expose on sides perpendicular to shaft ──

    override fun getGridConnectableSides(orientation: BlockOrientation): Set<Direction> {
        val shaft = blockState.getValue(DirectionalKineticBlock.FACING)
        return EnumSet.complementOf(EnumSet.of(shaft, shaft.opposite))
    }

    override fun initialize() {
        super.initialize()
        exposeSides()
    }

    // ── Stress ──

    override fun calculateStressApplied(): Float {
        val impact = MAX_STRESS_SU / 256f
        this.lastStressApplied = impact
        return impact
    }

    // ── Tick ──

    override fun tick() {
        super.tick()

        val newAE = AE_PER_256_RPM * (abs(speed.toInt()) / 256.0)
        if (newAE.compareTo(aeValue) != 0) {
            aeValue = newAE
            mainNode.ifPresent { grid, node -> grid.tickManager.wakeDevice(node) }
        }
    }

    // ── Goggle Tooltip ──

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
}
