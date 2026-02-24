package com.loliball.appliedcreate.block

import com.loliball.appliedcreate.block.entity.AndesitePatternProviderBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import appeng.block.crafting.PushDirection
import appeng.menu.locator.MenuLocators
import appeng.util.InteractionUtil
import appeng.util.Platform

class AndesitePatternProviderBlock : Block(
    Properties.of()
        .mapColor(MapColor.STONE)
        .strength(1.5f, 6.0f)
        .sound(SoundType.STONE)
        .requiresCorrectToolForDrops()
), EntityBlock {

    init {
        registerDefaultState(defaultBlockState().setValue(PUSH_DIRECTION, PushDirection.ALL))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(PUSH_DIRECTION)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return AndesitePatternProviderBlockEntity(pos, state)
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): ItemInteractionResult {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        }

        if (!stack.isEmpty && InteractionUtil.canWrenchRotate(stack)) {
            setSide(level, pos, hit.direction)
            return ItemInteractionResult.sidedSuccess(level.isClientSide)
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is AndesitePatternProviderBlockEntity) {
                blockEntity.openMenu(player, MenuLocators.forBlockEntity(blockEntity))
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        val be = level.getBlockEntity(pos)
        if (be is AndesitePatternProviderBlockEntity) {
            be.logic.updateRedstoneState()
        }
    }

    @Suppress("DEPRECATION")
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moving: Boolean) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is AndesitePatternProviderBlockEntity) {
                val drops = mutableListOf<net.minecraft.world.item.ItemStack>()
                blockEntity.addAdditionalDrops(level, pos, drops)
                drops.forEach { net.minecraft.world.Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), it) }
            }
            super.onRemove(state, level, pos, newState, moving)
        }
    }

    companion object {
        @JvmField
        val PUSH_DIRECTION: EnumProperty<PushDirection> = EnumProperty.create("push_direction", PushDirection::class.java)

        fun setSide(level: Level, pos: BlockPos, facing: Direction) {
            val currentState = level.getBlockState(pos)
            val pushSide = currentState.getValue(PUSH_DIRECTION).getDirection()

            val newPushDirection = when {
                pushSide == facing.opposite -> PushDirection.fromDirection(facing)
                pushSide == facing -> PushDirection.ALL
                pushSide == null -> PushDirection.fromDirection(facing.opposite)
                else -> PushDirection.fromDirection(Platform.rotateAround(pushSide, facing))
            }

            level.setBlockAndUpdate(pos, currentState.setValue(PUSH_DIRECTION, newPushDirection))
        }
    }
}