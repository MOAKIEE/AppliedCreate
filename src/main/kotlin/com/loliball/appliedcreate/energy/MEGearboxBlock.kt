package com.loliball.appliedcreate.energy

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.item.ItemStack
import com.simibubi.create.content.equipment.wrench.WrenchItem

/**
 * ME Gearbox Block — directional kinetic block with shaft.
 * Dual-network block: joins AE2 ME network and Create kinetic network.
 *
 * Shift+Wrench right-click toggles between EXPORT and IMPORT mode.
 * Stress multiplier is adjusted via Create-style scroll wheel on perpendicular faces.
 * Shaft extends along the FACING axis (both directions).
 * AE2 cable connects on perpendicular sides.
 */
class MEGearboxBlock : DirectionalKineticBlock(
    Properties.of()
        .strength(3.5f)
        .noOcclusion()
), IBE<MEGearboxBlockEntity>, ICogWheel {

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
     * Shift+Wrench toggles mode between EXPORT and IMPORT.
     * Normal wrench (without shift) falls through to Create's default wrench behaviour.
     */
    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): ItemInteractionResult {
        if (stack.item !is WrenchItem) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        if (!player.isShiftKeyDown) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION

        // Shift+Wrench: toggle mode
        if (level.isClientSide) return ItemInteractionResult.SUCCESS

        val be = level.getBlockEntity(pos) as? MEGearboxBlockEntity ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        be.toggleMode()

        val modeKey = if (be.mode == MEGearboxBlockEntity.Mode.EXPORT) "appliedcreate.me_gearbox.mode.export" else "appliedcreate.me_gearbox.mode.import"
        player.displayClientMessage(Component.translatable("appliedcreate.me_gearbox.mode").append(Component.translatable(modeKey)), true)

        return ItemInteractionResult.SUCCESS
    }

}
