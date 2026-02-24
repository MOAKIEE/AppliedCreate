package com.loliball.appliedcreate.item

import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.block.AndesitePatternProviderBlock
import com.loliball.appliedcreate.block.entity.AndesitePatternProviderBlockEntity
import com.loliball.appliedcreate.block.entity.BrassPatternProviderBlockEntity
import com.loliball.appliedcreate.part.AndesitePatternProviderPart
import com.loliball.appliedcreate.part.BrassPatternProviderPart
import appeng.api.parts.IPartHost
import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext

class BrassPatternProviderUpgradeItem : Item(Properties().stacksTo(16)) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        val player = context.player ?: return InteractionResult.PASS

        if (level.isClientSide) return InteractionResult.sidedSuccess(true)

        val blockEntity = level.getBlockEntity(pos)

        // Case 1: Block form — Andesite Pattern Provider block entity
        if (blockEntity is AndesitePatternProviderBlockEntity) {
            val savedTag = CompoundTag()
            blockEntity.saveAdditional(savedTag)

            // Preserve PUSH_DIRECTION from old block state
            val oldState = level.getBlockState(pos)
            val pushDir = oldState.getValue(AndesitePatternProviderBlock.PUSH_DIRECTION)

            level.removeBlockEntity(pos)
            level.setBlock(
                pos,
                AppliedCreate.BRASS_PATTERN_PROVIDER_BLOCK.get().defaultBlockState()
                    .setValue(AndesitePatternProviderBlock.PUSH_DIRECTION, pushDir),
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

        // Case 2: Cable subpart form — AndesitePatternProviderPart attached to cable
        if (blockEntity is IPartHost) {
            val host = blockEntity as IPartHost
            val hitVec = context.clickLocation
            val selected = host.selectPartWorld(hitVec)

            if (selected.part is AndesitePatternProviderPart) {
                val andesitePart = selected.part as AndesitePatternProviderPart
                val side = selected.side ?: return InteractionResult.PASS

                // Save part NBT data
                val savedTag = CompoundTag()
                andesitePart.writeToNBT(savedTag)

                // Remove old part
                host.removePart(andesitePart)

                // Add new brass part
                @Suppress("UNCHECKED_CAST")
                val brassPart = host.addPart(
                    AppliedCreate.BRASS_PATTERN_PROVIDER_PART_ITEM.get() as appeng.api.parts.IPartItem<BrassPatternProviderPart>,
                    side,
                    player
                )

                if (brassPart != null) {
                    // Restore saved data to new part
                    brassPart.readFromNBT(savedTag)
                    host.markForUpdate()
                    host.markForSave()
                }

                if (!player.isCreative) {
                    context.itemInHand.shrink(1)
                }

                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.0f)
                return InteractionResult.SUCCESS
            }
        }

        return InteractionResult.PASS
    }
}
