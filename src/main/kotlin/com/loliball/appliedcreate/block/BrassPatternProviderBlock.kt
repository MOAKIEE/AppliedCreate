package com.loliball.appliedcreate.block

import com.loliball.appliedcreate.block.entity.BrassPatternProviderBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition

import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import appeng.block.crafting.PushDirection
import appeng.menu.locator.MenuLocators
import appeng.util.InteractionUtil


class BrassPatternProviderBlock : Block(
    Properties.of()
        .mapColor(MapColor.METAL)
        .strength(1.5f, 6.0f)
        .sound(SoundType.METAL)
        .requiresCorrectToolForDrops()
), EntityBlock {

    init {
        registerDefaultState(defaultBlockState().setValue(AndesitePatternProviderBlock.PUSH_DIRECTION, PushDirection.ALL))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(AndesitePatternProviderBlock.PUSH_DIRECTION)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return BrassPatternProviderBlockEntity(pos, state)
    }

    @Suppress("DEPRECATION")
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return InteractionResult.PASS
        }

        val heldItem = player.getItemInHand(hand)
        if (!heldItem.isEmpty && InteractionUtil.canWrenchRotate(heldItem)) {
            AndesitePatternProviderBlock.setSide(level, pos, hit.direction)
            return InteractionResult.sidedSuccess(level.isClientSide)
        }

        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is BrassPatternProviderBlockEntity) {
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
        if (be is BrassPatternProviderBlockEntity) {
            be.logic.updateRedstoneState()
        }
    }

    @Suppress("DEPRECATION")
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moving: Boolean) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is BrassPatternProviderBlockEntity) {
                val drops = mutableListOf<net.minecraft.world.item.ItemStack>()
                blockEntity.addAdditionalDrops(level, pos, drops)
                drops.forEach { net.minecraft.world.Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), it) }
            }
            super.onRemove(state, level, pos, newState, moving)
        }
    }

    companion object {
    }
}
