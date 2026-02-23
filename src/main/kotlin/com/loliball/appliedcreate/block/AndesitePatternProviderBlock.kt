package com.loliball.appliedcreate.block

import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.block.entity.AndesitePatternProviderBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import appeng.menu.locator.MenuLocators

class AndesitePatternProviderBlock : Block(
    Properties.of()
        .mapColor(MapColor.STONE)
        .strength(1.5f, 6.0f)
        .sound(SoundType.STONE)
        .requiresCorrectToolForDrops()
), EntityBlock {

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
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is AndesitePatternProviderBlockEntity) {
                blockEntity.openMenu(player, MenuLocators.forBlockEntity(blockEntity))
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
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
}
