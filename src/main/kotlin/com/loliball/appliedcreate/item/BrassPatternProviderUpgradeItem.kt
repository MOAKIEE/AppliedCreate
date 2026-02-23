package com.loliball.appliedcreate.item

import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.block.entity.AndesitePatternProviderBlockEntity
import com.loliball.appliedcreate.block.entity.BrassPatternProviderBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level

class BrassPatternProviderUpgradeItem : Item(Properties().stacksTo(16)) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val player = context.player ?: return InteractionResult.PASS

        if (level.isClientSide) return InteractionResult.sidedSuccess(true)

        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity !is AndesitePatternProviderBlockEntity) return InteractionResult.PASS

        val savedTag = CompoundTag()
        blockEntity.saveAdditional(savedTag)

        val blockState = level.getBlockState(pos)
        val facing = blockState.getValue(
            com.loliball.appliedcreate.block.AndesitePatternProviderBlock.FACING
        )

        level.removeBlockEntity(pos)
        level.setBlock(
            pos,
            AppliedCreate.BRASS_PATTERN_PROVIDER_BLOCK.get().defaultBlockState()
                .setValue(com.loliball.appliedcreate.block.BrassPatternProviderBlock.FACING, facing),
            3
        )

        val newBe = level.getBlockEntity(pos)
        if (newBe is BrassPatternProviderBlockEntity) {
            newBe.loadTag(savedTag)
            newBe.setChanged()
        }

        if (!player.isCreative) {
            context.itemInHand.shrink(1)
        }

        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.0f)

        return InteractionResult.SUCCESS
    }
}
