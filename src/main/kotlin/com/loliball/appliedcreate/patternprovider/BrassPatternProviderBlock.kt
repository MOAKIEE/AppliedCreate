package com.loliball.appliedcreate.patternprovider

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.Containers
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import appeng.api.implementations.items.IMemoryCard
import appeng.api.implementations.items.MemoryCardMessages
import appeng.api.ids.AEComponents
import appeng.block.crafting.PushDirection
import appeng.items.tools.MemoryCardItem
import appeng.menu.locator.MenuLocators
import appeng.util.InteractionUtil
import appeng.util.SettingsFrom
import net.minecraft.core.component.DataComponentMap

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

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): ItemInteractionResult {
        if (stack.item is IMemoryCard) {
            val memoryCard = stack.item as IMemoryCard
            if (level.isClientSide) {
                return ItemInteractionResult.sidedSuccess(true)
            }

            val blockEntity = level.getBlockEntity(pos) as? BrassPatternProviderBlockEntity
                ?: return ItemInteractionResult.FAIL

            if (InteractionUtil.isInAlternateUseMode(player)) {
                val builder = DataComponentMap.builder()
                blockEntity.exportSettings(SettingsFrom.MEMORY_CARD, builder, player)
                val settings = builder.build()
                if (!settings.isEmpty) {
                    MemoryCardItem.clearCard(stack)
                    stack.applyComponents(settings)
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED)
                }
            } else {
                val savedName = stack.get(AEComponents.EXPORTED_SETTINGS_SOURCE)
                if (savedName == name) {
                    blockEntity.importSettings(SettingsFrom.MEMORY_CARD, stack.components, player)
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED)
                } else {
                    MemoryCardItem.importGenericSettingsAndNotify(blockEntity, stack.components, player)
                }
            }
            return ItemInteractionResult.sidedSuccess(false)
        }

        if (InteractionUtil.isInAlternateUseMode(player)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        }

        if (!stack.isEmpty && InteractionUtil.canWrenchRotate(stack)) {
            AndesitePatternProviderBlock.setSide(level, pos, hit.direction)
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
                val drops = mutableListOf<ItemStack>()
                blockEntity.addAdditionalDrops(level, pos, drops)
                drops.forEach { Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), it) }
            }
            super.onRemove(state, level, pos, newState, moving)
        }
    }
}
