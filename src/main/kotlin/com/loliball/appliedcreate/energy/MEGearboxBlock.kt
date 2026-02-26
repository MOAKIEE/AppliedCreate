package com.loliball.appliedcreate.energy

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

/**
 * ME Gearbox Block — directional kinetic block with shaft.
 * Dual-network block: joins AE2 ME network and Create kinetic network.
 *
 * Right-click (without item) toggles between EXPORT and IMPORT mode.
 * Shaft extends along the FACING axis (both directions).
 * AE2 cable connects on perpendicular sides.
 */
class MEGearboxBlock : DirectionalKineticBlock(
    Properties.of()
        .strength(3.5f)
        .noOcclusion()
), IBE<MEGearboxBlockEntity> {

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).axis
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face.axis == state.getValue(FACING).axis
    }

    override fun getBlockEntityClass(): Class<MEGearboxBlockEntity> {
        return MEGearboxBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out MEGearboxBlockEntity> {
        @Suppress("UNCHECKED_CAST")
        return AppliedCreate.ME_GEARBOX_BE.get() as BlockEntityType<out MEGearboxBlockEntity>
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return MEGearboxBlockEntity(AppliedCreate.ME_GEARBOX_BE.get(), pos, state)
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

    /**
     * Right-click without item toggles mode between EXPORT and IMPORT.
     */
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS

        val be = level.getBlockEntity(pos) as? MEGearboxBlockEntity ?: return InteractionResult.PASS
        be.toggleMode()

        val modeName = if (be.mode == MEGearboxBlockEntity.Mode.EXPORT) "Export (ME → Kinetic)" else "Import (Kinetic → ME)"
        player.displayClientMessage(Component.literal("Mode: $modeName"), true)

        return InteractionResult.SUCCESS
    }
}
