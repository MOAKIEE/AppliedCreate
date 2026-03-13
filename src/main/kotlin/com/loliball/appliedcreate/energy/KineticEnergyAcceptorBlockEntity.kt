package com.loliball.appliedcreate.energy

import appeng.api.networking.energy.IPassiveEnergyGenerator
import appeng.api.orientation.BlockOrientation
import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.math.VecHelper
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.abs

class KineticEnergyAcceptorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : NetworkedKineticBlockEntity(type, pos, state) {

    companion object {
        const val AE_PER_256_RPM = 640.0
        const val BASE_STRESS_SU = 16384
        const val MAX_MULTIPLIER = 16
    }

    private var aeValue: Double = 0.0

    lateinit var stressMultiplier: ScrollValueBehaviour

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

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        stressMultiplier = ScrollValueBehaviour(
            Component.translatable("appliedcreate.kinetic_energy_acceptor.multiplier"),
            this,
            AcceptorValueBoxTransform()
        )
        stressMultiplier.between(1, MAX_MULTIPLIER)
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
        val impact = (BASE_STRESS_SU / 256f) * getMultiplier()
        this.lastStressApplied = impact
        return impact
    }

    override fun tick() {
        super.tick()

        val newAE = AE_PER_256_RPM * getMultiplier() * (abs(speed.toInt()) / 256.0)
        if (newAE.compareTo(aeValue) != 0) {
            aeValue = newAE
            mainNode.ifPresent { grid, node -> grid.tickManager.wakeDevice(node) }
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
