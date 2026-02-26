package com.loliball.appliedcreate.energy

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * Kinetic Energy Acceptor Block — directional kinetic block with shaft.
 * Receives rotation from Create network and converts to AE2 energy.
 *
 * Shaft extends along the FACING axis (both directions).
 * AE2 cable connects on perpendicular sides.
 */
class KineticEnergyAcceptorBlock : DirectionalKineticBlock(
    Properties.of()
        .strength(3.5f)
        .noOcclusion()
), IBE<KineticEnergyAcceptorBlockEntity> {

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).axis
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face.axis == state.getValue(FACING).axis
    }

    override fun getBlockEntityClass(): Class<KineticEnergyAcceptorBlockEntity> {
        return KineticEnergyAcceptorBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out KineticEnergyAcceptorBlockEntity> {
        @Suppress("UNCHECKED_CAST")
        return AppliedCreate.KINETIC_ENERGY_ACCEPTOR_BE.get() as BlockEntityType<out KineticEnergyAcceptorBlockEntity>
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return KineticEnergyAcceptorBlockEntity(AppliedCreate.KINETIC_ENERGY_ACCEPTOR_BE.get(), pos, state)
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
        return defaultBlockState().setValue(FACING, preferred)
    }
}
