package com.loliball.appliedcreate.patternprovider

import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.patternprovider.AndesitePatternProviderBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.Containers
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.network.chat.Component
import appeng.api.ids.AEItemIds
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
import appeng.api.ids.AEComponents
import appeng.block.crafting.PushDirection
import appeng.items.tools.MemoryCardItem
import appeng.menu.locator.MenuLocators
import appeng.util.InteractionUtil
import appeng.util.Platform
import appeng.util.SettingsFrom
import net.minecraft.core.component.DataComponentMap

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
        // Handle memory card
        if (stack.item is IMemoryCard) {
            val memoryCard = stack.item as IMemoryCard
            if (level.isClientSide) {
                return ItemInteractionResult.sidedSuccess(true)
            }

            val blockEntity = level.getBlockEntity(pos) as? AndesitePatternProviderBlockEntity
            
            if (InteractionUtil.isInAlternateUseMode(player)) {
                val builder = DataComponentMap.builder()
                val pushDirection = state.getValue(PUSH_DIRECTION)
                builder.set(AEComponents.EXPORTED_SETTINGS_SOURCE, Component.literal("andesite_pattern_provider"))
                val data = mutableMapOf<String, String>()
                data["pushDirection"] = pushDirection.name
                
                if (blockEntity != null) {
                    val patternsNbt = ListTag()
                    val patternInv = blockEntity.logic.getPatternInv()
                    for (i in 0 until patternInv.size()) {
                        val patternStack = patternInv.getStackInSlot(i)
                        if (!patternStack.isEmpty) {
                            val patternData = CompoundTag()
                            patternData.putInt("slot", i)
                            patternData.put("item", patternStack.save(level.registryAccess()))
                            patternsNbt.add(patternData)
                        }
                    }
                    if (patternsNbt.isNotEmpty()) {
                        data["patterns"] = patternsNbt.toString()
                    }
                }
                
                builder.set(AEComponents.EXPORTED_SETTINGS, data)

                MemoryCardItem.clearCard(stack)
                stack.applyComponents(builder.build())
                memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED)
            } else {
                val savedName = stack.get(AEComponents.EXPORTED_SETTINGS_SOURCE)
                val beName = Component.literal("andesite_pattern_provider")

                if (savedName != null && savedName == beName) {
                    val data = stack.get(AEComponents.EXPORTED_SETTINGS)
                    if (data != null) {
                        data["pushDirection"]?.let { dirName ->
                            try {
                                val newDirection = PushDirection.valueOf(dirName)
                                level.setBlockAndUpdate(pos, state.setValue(PUSH_DIRECTION, newDirection))
                            } catch (_: IllegalArgumentException) {
                            }
                        }
                        
                        if (blockEntity != null && data.containsKey("patterns")) {
                            loadPatternsFromMemoryCard(blockEntity, data, player, level)
                        }
                    }
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED)
                } else {
                    memoryCard.notifyUser(player, MemoryCardMessages.INVALID_MACHINE)
                }
            }
            return ItemInteractionResult.sidedSuccess(false)
        }

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
                val drops = mutableListOf<ItemStack>()
                blockEntity.addAdditionalDrops(level, pos, drops)
                drops.forEach { Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), it) }
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

        private fun loadPatternsFromMemoryCard(
            blockEntity: AndesitePatternProviderBlockEntity,
            data: Map<String, String>,
            player: Player,
            level: Level
        ) {
            val patternsStr = data["patterns"] ?: return
            val patternsNbt = try {
                net.minecraft.nbt.TagParser.parseTag(patternsStr)
            } catch (_: Exception) {
                return
            }
            if (patternsNbt !is ListTag) return

            val blankPatternItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AEItemIds.BLANK_PATTERN)
            val patternInv = blockEntity.logic.getPatternInv()

            for (i in 0 until patternsNbt.size()) {
                val patternData = patternsNbt.getCompound(i)
                val slot = patternData.getInt("slot")
                if (slot < 0 || slot >= patternInv.size()) continue

                if (!patternInv.getStackInSlot(slot).isEmpty) continue

                val itemNbt = patternData.getCompound("item")
                val patternStack = ItemStack.parse(level.registryAccess(), itemNbt).orElse(ItemStack.EMPTY)
                if (patternStack.isEmpty) continue

                val blankSlot = findBlankPatternSlot(player, blankPatternItem)
                if (blankSlot == -1) continue

                player.inventory.removeItem(blankSlot, 1)
                patternInv.setItemDirect(slot, patternStack)
            }
        }

        private fun findBlankPatternSlot(player: Player, blankPatternItem: net.minecraft.world.item.Item): Int {
            for (i in 0 until player.inventory.containerSize) {
                val stack = player.inventory.getItem(i)
                if (stack.item == blankPatternItem && stack.count > 0) {
                    return i
                }
            }
            return -1
        }
    }
}