package com.loliball.appliedcreate.kinetic

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import com.loliball.appliedcreate.AppliedCreate

/**
 * Stress Provider Block — placed adjacent to a Stress P2P Tunnel (output side).
 *
 * This is a KineticBlock that outputs rotation into the Create kinetic network.
 * It receives speed from the paired StressAcceptorBlockEntity on the input side
 * through the virtual connection managed by StressP2PNetwork.
 *
 * The FACING direction points toward the cable containing the P2P tunnel.
 * The shaft outputs rotation from BOTH directions on the facing axis.
 */
class StressProviderBlock : DirectionalKineticBlock(
    Properties.of()
        .strength(3.5f)
        .noOcclusion()
), IBE<StressProviderBlockEntity> {

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).axis
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        // Must return true for BOTH directions on the axis so Create's RotationPropagator
        // can discover the custom connection through the virtual kinetic network.
        // This matches CreateEnderTransmission's EnergyTransmitterBlock pattern.
        return face.axis == state.getValue(FACING).axis
    }

    override fun getBlockEntityClass(): Class<StressProviderBlockEntity> {
        return StressProviderBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out StressProviderBlockEntity> {
        @Suppress("UNCHECKED_CAST")
        return AppliedCreate.STRESS_PROVIDER_BE.get() as BlockEntityType<out StressProviderBlockEntity>
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return StressProviderBlockEntity(AppliedCreate.STRESS_PROVIDER_BE.get(), pos, state)
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
