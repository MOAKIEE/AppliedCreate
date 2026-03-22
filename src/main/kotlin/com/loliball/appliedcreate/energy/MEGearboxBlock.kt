package com.loliball.appliedcreate.energy

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.content.equipment.wrench.WrenchItem
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel
import com.simibubi.create.foundation.block.IBE
import appeng.api.implementations.items.IMemoryCard
import appeng.api.implementations.items.MemoryCardMessages
import appeng.items.tools.MemoryCardItem
import appeng.menu.MenuOpener
import appeng.menu.locator.MenuLocators
import appeng.util.InteractionUtil
import appeng.util.SettingsFrom
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class MEGearboxBlock : DirectionalKineticBlock(
    Properties.of().strength(3.5f).noOcclusion()
), IBE<MEGearboxBlockEntity>, ICogWheel, IWrenchable {

    override fun getRotationAxis(state: BlockState): Direction.Axis = state.getValue(FACING).axis

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean =
        face.axis == state.getValue(FACING).axis

    override fun getBlockEntityClass(): Class<MEGearboxBlockEntity> = MEGearboxBlockEntity::class.java

    override fun getBlockEntityType(): BlockEntityType<out MEGearboxBlockEntity> {
        @Suppress("UNCHECKED_CAST")
        return AppliedCreate.ME_GEARBOX_BE.get() as BlockEntityType<out MEGearboxBlockEntity>
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        MEGearboxBlockEntity(AppliedCreate.ME_GEARBOX_BE.get(), pos, state)

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val preferred = getPreferredFacing(context)
        if (preferred == null || (context.player != null && context.player!!.isShiftKeyDown)) {
            val nearest = context.nearestLookingDirection
            return defaultBlockState().setValue(FACING, if (context.player != null && context.player!!.isShiftKeyDown) nearest else nearest.opposite)
        }
        return defaultBlockState().setValue(FACING, preferred)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun use(
        state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hit: BlockHitResult
    ): InteractionResult {
        val stack = player.getItemInHand(hand)

        if (!stack.isEmpty && stack.item is IMemoryCard) {
            val memoryCard = stack.item as IMemoryCard
            val blockEntity = level.getBlockEntity(pos) as? MEGearboxBlockEntity
                ?: return InteractionResult.FAIL

            val name = this.descriptionId

            if (InteractionUtil.isInAlternateUseMode(player)) {
                val data = net.minecraft.nbt.CompoundTag()
                blockEntity.exportSettings(SettingsFrom.MEMORY_CARD, data, player)
                if (!data.isEmpty) {
                    memoryCard.setMemoryCardContents(stack, name, data)
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED)
                }
            } else {
                val storedName = memoryCard.getSettingsName(stack)
                val data = memoryCard.getData(stack)

                if (name == storedName) {
                    blockEntity.importSettings(SettingsFrom.MEMORY_CARD, data, player)
                    memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED)
                } else {
                    MemoryCardItem.importGenericSettingsAndNotify(blockEntity, data, player)
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide)
        }

        if (stack.item is WrenchItem) {
            val context = UseOnContext(level, player, hand, stack, hit)
            return if (player.isShiftKeyDown) {
                onSneakWrenched(state, context)
            } else {
                onWrenched(state, context)
            }
        }

        if (level.isClientSide) return InteractionResult.SUCCESS

        val be = level.getBlockEntity(pos) ?: return InteractionResult.PASS
        MenuOpener.open(AppliedCreate.ME_GEARBOX_MENU.get(), player, MenuLocators.forBlockEntity(be))

        return InteractionResult.SUCCESS
    }
}
