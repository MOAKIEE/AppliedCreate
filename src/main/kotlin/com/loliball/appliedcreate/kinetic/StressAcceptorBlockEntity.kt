package com.loliball.appliedcreate.kinetic

import com.loliball.appliedcreate.p2p.StressP2PTunnelPart
import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import appeng.api.networking.IInWorldGridNodeHost

/**
 * Stress Acceptor Block Entity — the input companion for Stress P2P.
 *
 * This KineticBlockEntity absorbs rotation from the Create kinetic network.
 * Every tick, it reads its current speed and pushes it to the adjacent
 * StressP2PTunnelPart (input side) for transfer through the ME network.
 *
 * Stress impact is 0 (free consumer), so it does not add load to the input network.
 * The output-side StressProviderBlockEntity uses a fixed capacity to supply
 * stress to the output kinetic network.
 */
class StressAcceptorBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : KineticBlockEntity(type, pos, state) {
    constructor(pos: BlockPos, state: BlockState) : this(AppliedCreate.STRESS_ACCEPTOR_BE.get(), pos, state)

    private var lastPushedSpeed: Float = 0f

    override fun tick() {
        super.tick()
        if (level == null || level!!.isClientSide) return

        val currentSpeed = speed

        // Only update the tunnel when speed changes
        if (currentSpeed != lastPushedSpeed) {
            lastPushedSpeed = currentSpeed
            pushToTunnel(currentSpeed)
        }
    }

    /**
     * Find the adjacent Stress P2P tunnel part and push our speed to it.
     */
    private fun pushToTunnel(speed: Float) {
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
            part.updateInputSpeed(speed)
        }
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)
        // When our speed changes, immediately push to tunnel
        lastPushedSpeed = speed
        pushToTunnel(speed)
    }
}
