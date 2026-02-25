package com.loliball.appliedcreate.kinetic

import com.loliball.appliedcreate.p2p.StressP2PTunnelPart
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import com.loliball.appliedcreate.AppliedCreate

/**
 * Stress Acceptor Block — placed adjacent to a Stress P2P Tunnel (input side).
 *
 * This is a KineticBlock that acts as a stress consumer. It absorbs rotation from
 * the Create kinetic network and passes the speed + stress capacity to the adjacent
 * StressP2PTunnelPart for transfer through the ME network.
 *
 * The FACING direction points toward the cable containing the P2P tunnel.
 * The shaft accepts rotation from the opposite direction.
 */
class StressAcceptorBlock : DirectionalKineticBlock(
    Properties.of()
        .strength(3.5f)
        .noOcclusion()
), IBE<StressAcceptorBlockEntity> {

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).axis
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        // Shaft is on the side OPPOSITE to FACING (FACING points toward the P2P cable)
        return face == state.getValue(FACING).opposite
    }

    override fun getBlockEntityClass(): Class<StressAcceptorBlockEntity> {
        return StressAcceptorBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out StressAcceptorBlockEntity> {
        @Suppress("UNCHECKED_CAST")
        return AppliedCreate.STRESS_ACCEPTOR_BE.get() as BlockEntityType<out StressAcceptorBlockEntity>
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return StressAcceptorBlockEntity(AppliedCreate.STRESS_ACCEPTOR_BE.get(), pos, state)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val preferred = getPreferredFacing(context)
        if (preferred == null || (context.player != null && context.player!!.isShiftKeyDown)) {
            val nearest = context.nearestLookingDirection
            return defaultBlockState().setValue(
                FACING,
                if (context.player != null && context.player!!.isShiftKeyDown) nearest else nearest.opposite
            )
        }
        return defaultBlockState().setValue(FACING, preferred.opposite)
    }
}
