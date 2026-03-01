package com.loliball.appliedcreate.energy

import appeng.api.networking.energy.IPassiveEnergyGenerator
import appeng.api.orientation.BlockOrientation
import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import net.createmod.catnip.utility.lang.LangBuilder
import net.createmod.catnip.utility.lang.LangNumberFormat
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import java.util.EnumSet
import kotlin.math.abs

class KineticEnergyAcceptorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : NetworkedKineticBlockEntity(type, pos, state) {

    companion object {
        const val AE_PER_256_RPM = 640.0
        const val MAX_STRESS_SU = 16384
    }

    private var aeValue: Double = 0.0

    init {
        mainNode.setVisualRepresentation(AppliedCreate.KINETIC_ENERGY_ACCEPTOR_ITEM.get())
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

    override fun getGridConnectableSides(orientation: BlockOrientation): Set<Direction> {
        val shaft = blockState.getValue(DirectionalKineticBlock.FACING)
        return EnumSet.complementOf(EnumSet.of(shaft, shaft.opposite))
    }

    override fun initialize() {
        super.initialize()
        exposeSides()
    }

    override fun calculateStressApplied(): Float {
        val impact = MAX_STRESS_SU / 256f
        this.lastStressApplied = impact
        return impact
    }

    override fun tick() {
        super.tick()
        val newAE = AE_PER_256_RPM * (abs(speed.toInt()) / 256.0)
        if (newAE.compareTo(aeValue) != 0) {
            aeValue = newAE
            mainNode.ifPresent { grid, node -> grid.tickManager.wakeDevice(node) }
        }
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        LangBuilder("create").translate("gui.goggles.generator_stats").forGoggles(tooltip)
        LangBuilder("create").translate("tooltip.capacityProvided").style(ChatFormatting.GRAY).forGoggles(tooltip)
        LangBuilder("create").text(LangNumberFormat.format(aeValue))
            .text(" AE/t ").style(ChatFormatting.GOLD).space()
            .add(LangBuilder("create").translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY))
            .forGoggles(tooltip, 1)
        LangBuilder("create").translate("gui.goggles.kinetic_stats").forGoggles(tooltip)
        LangBuilder("create").translate("tooltip.stressImpact").style(ChatFormatting.GRAY).forGoggles(tooltip)
        val stressTotal = lastStressApplied * abs(speed)
        LangBuilder("create").text(LangNumberFormat.format(stressTotal.toDouble()))
            .translate("generic.unit.stress").style(ChatFormatting.AQUA).space()
            .add(LangBuilder("create").translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY))
            .forGoggles(tooltip, 1)
        return true
    }
}
