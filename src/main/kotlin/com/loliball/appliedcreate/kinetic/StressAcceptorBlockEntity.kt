package com.loliball.appliedcreate.kinetic

import com.loliball.appliedcreate.p2p.StressP2PTunnelPart
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import appeng.api.networking.IInWorldGridNodeHost

/**
 * Stress Acceptor Block Entity — the input companion for Stress P2P.
 *
 * This KineticBlockEntity consumes rotation from the Create kinetic network.
 * Every tick, it reads its current speed and stress capacity and pushes them
 * to the adjacent StressP2PTunnelPart (input side).
 *
 * Stress impact: configurable, represents the load this acceptor places on the kinetic network.
 * The impact should mirror the total stress being consumed by all output providers.
 */
class StressAcceptorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : KineticBlockEntity(type, pos, state) {

    private var lastPushedSpeed: Float = 0f
    private var lastPushedCapacity: Float = 0f

    override fun tick() {
        super.tick()
        if (level == null || level!!.isClientSide) return

        val currentSpeed = speed
        val currentCapacity = calculateAddedStressCapacity()

        // Only update the tunnel when values change
        if (currentSpeed != lastPushedSpeed || currentCapacity != lastPushedCapacity) {
            lastPushedSpeed = currentSpeed
            lastPushedCapacity = currentCapacity
            pushToTunnel(currentSpeed, currentCapacity)
        }
    }

    /**
     * Find the adjacent Stress P2P tunnel part and push our kinetic values to it.
     */
    private fun pushToTunnel(speed: Float, capacity: Float) {
        val facing = blockState.getValue(
            com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING
        )
        val tunnelPos = worldPosition.relative(facing)
        val level = this.level ?: return

        val be = level.getBlockEntity(tunnelPos) ?: return
        // Cable bus block entities implement IInWorldGridNodeHost
        if (be !is IInWorldGridNodeHost) return

        // Try to find the P2P part on the side facing us
        val cableBus = be as? appeng.api.parts.IPartHost ?: return
        val part = cableBus.getPart(facing.opposite)
        if (part is StressP2PTunnelPart && !part.isOutput) {
            part.updateInputValues(speed, capacity)
        }
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)
        // When our speed changes, immediately push to tunnel
        val capacity = calculateAddedStressCapacity()
        lastPushedSpeed = speed
        lastPushedCapacity = capacity
        pushToTunnel(speed, capacity)
    }
}
