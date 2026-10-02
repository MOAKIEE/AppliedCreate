package com.loliball.appliedcreate.energy

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.AllItems
import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel
import com.simibubi.create.foundation.block.IBE
import appeng.api.implementations.items.IMemoryCard
import appeng.api.implementations.items.MemoryCardMessages
import appeng.api.ids.AEComponents
import appeng.items.tools.MemoryCardItem
import appeng.menu.MenuOpener
import appeng.menu.locator.MenuLocators
import appeng.util.InteractionUtil
import appeng.util.SettingsFrom
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
/**
 * ME Gearbox Block — directional kinetic block with shaft.
 * Dual-network block: joins AE2 ME network and Create kinetic network.
 *
 * Right-click opens AE2-style GUI for configuration.
 * Wrench right-click rotates the block (default Create behaviour via IWrenchable).
 * Shift+Wrench picks up/drops the block (default Create behaviour via IWrenchable).
 * Shaft extends along the FACING axis (both directions).
 * AE2 cable connects on perpendicular sides.
 */
class MEGearboxBlock : DirectionalKineticBlock(
    Properties.of()
        .strength(3.5f)
        .noOcclusion()
), IBE<MEGearboxBlockEntity>, ICogWheel, IWrenchable {

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

    override fun getDrops(state: BlockState, builder: LootParams.Builder): MutableList<ItemStack> {
        val drops = super.getDrops(state, builder)
        val gearbox = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) as? MEGearboxBlockEntity
            ?: return drops
        if (gearbox.storedStress() > 0) {
            // An unaccepted refund remains real stored stress. Preserve it on wrench/break drops.
            val data = CompoundTag()
            data.putString("id", "appliedcreate:me_gearbox")
            gearbox.writeSettingsAndBuffer(data)
            drops.filter { it.`is`(asItem()) }.forEach {
                it.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(data.copy()))
            }
        }
        return drops
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

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS

        val be = level.getBlockEntity(pos) ?: return InteractionResult.PASS
        MenuOpener.open(AppliedCreate.ME_GEARBOX_MENU.get(), player, MenuLocators.forBlockEntity(be))

        return InteractionResult.SUCCESS
    }

    override fun useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): ItemInteractionResult {
        if (AllItems.WRENCH.isIn(stack)) {
            val context = UseOnContext(level, player, hand, stack, hit)
            val result = if (player.isShiftKeyDown) {
                onSneakWrenched(state, context)
            } else {
                onWrenched(state, context)
            }
            return if (result == InteractionResult.SUCCESS) {
                ItemInteractionResult.sidedSuccess(level.isClientSide)
            } else {
                ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
            }
        }

        val be = level.getBlockEntity(pos) as? MEGearboxBlockEntity
            ?: return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION

        if (stack.item is IMemoryCard) {
            val memoryCard = stack.item as IMemoryCard
            if (level.isClientSide) {
                return ItemInteractionResult.sidedSuccess(true)
            }

            if (InteractionUtil.isInAlternateUseMode(player)) {
                val builder = net.minecraft.core.component.DataComponentMap.builder()
                be.exportSettings(SettingsFrom.MEMORY_CARD, builder, player)
                val settings = builder.build()

                if (!settings.isEmpty) {
                    MemoryCardItem.clearCard(stack)
                    stack.applyComponents(settings)
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED)
                }
            } else {
                val savedName = stack.get(AEComponents.EXPORTED_SETTINGS_SOURCE)
                val beName = AppliedCreate.ME_GEARBOX_BLOCK.get().name

                if (savedName != null && savedName == beName) {
                    be.importSettings(SettingsFrom.MEMORY_CARD, stack.components, player)
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED)
                } else {
                    MemoryCardItem.importGenericSettingsAndNotify(be, stack.components, player)
                }
            }
            return ItemInteractionResult.sidedSuccess(false)
        }

        if (level.isClientSide) return ItemInteractionResult.SUCCESS

        MenuOpener.open(AppliedCreate.ME_GEARBOX_MENU.get(), player, MenuLocators.forBlockEntity(be))

        return ItemInteractionResult.SUCCESS
    }

}
