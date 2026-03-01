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

class StressP2PCompanionBlock : DirectionalKineticBlock(
    Properties.of()
        .strength(3.5f)
        .noOcclusion()
), IBE<StressP2PCompanionBlockEntity> {

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).axis
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face.axis == state.getValue(FACING).axis
    }

    override fun getBlockEntityClass(): Class<StressP2PCompanionBlockEntity> {
        return StressP2PCompanionBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out StressP2PCompanionBlockEntity> {
        @Suppress("UNCHECKED_CAST")
        return AppliedCreate.STRESS_P2P_COMPANION_BE.get() as BlockEntityType<out StressP2PCompanionBlockEntity>
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return StressP2PCompanionBlockEntity(AppliedCreate.STRESS_P2P_COMPANION_BE.get(), pos, state)
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
