package com.loliball.appliedcreate.block

import com.loliball.appliedcreate.block.entity.AndesitePatternProviderBlockEntity
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
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import appeng.api.implementations.items.IMemoryCard
import appeng.api.implementations.items.MemoryCardMessages
import appeng.block.crafting.PushDirection
import appeng.items.tools.MemoryCardItem
import appeng.menu.locator.MenuLocators
import appeng.util.InteractionUtil
import appeng.util.Platform
import appeng.util.SettingsFrom

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

    @Suppress("DEPRECATION")
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val heldItem = player.getItemInHand(hand)
        
        if (!heldItem.isEmpty && heldItem.item is IMemoryCard) {
            val memoryCard = heldItem.item as IMemoryCard
            val blockEntity = level.getBlockEntity(pos) as? AndesitePatternProviderBlockEntity
                ?: return InteractionResult.FAIL
            
            val name = this.descriptionId
            
            if (InteractionUtil.isInAlternateUseMode(player)) {
                val data = net.minecraft.nbt.CompoundTag()
                blockEntity.exportSettings(SettingsFrom.MEMORY_CARD, data, player)
                if (!data.isEmpty) {
                    memoryCard.setMemoryCardContents(heldItem, name, data)
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED)
                }
            } else {
                val storedName = memoryCard.getSettingsName(heldItem)
                val data = memoryCard.getData(heldItem)
                
                if (name == storedName) {
                    blockEntity.importSettings(SettingsFrom.MEMORY_CARD, data, player)
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED)
                } else {
                    MemoryCardItem.importGenericSettingsAndNotify(blockEntity, data, player)
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide)
        }
        
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return InteractionResult.PASS
        }

        if (!heldItem.isEmpty && InteractionUtil.canWrenchRotate(heldItem)) {
            setSide(level, pos, hit.direction)
            return InteractionResult.sidedSuccess(level.isClientSide)
        }

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
            val pushSide = currentState.getValue(PUSH_DIRECTION).direction

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
