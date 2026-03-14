package com.loliball.appliedcreate.energy

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.content.equipment.wrench.WrenchItem
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
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
), IBE<MEGearboxBlockEntity>, IWrenchable {

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
        if (stack.item !is WrenchItem) return InteractionResult.PASS

        // Handle wrench rotation directly here so it doesn't fall through
        // to default block interaction (which would do nothing useful).
        val context = UseOnContext(level, player, hand, stack, hit)
        if (player.isShiftKeyDown) {
            // Shift+Wrench: toggle input/output mode
            if (level.isClientSide) return InteractionResult.SUCCESS
            val be = level.getBlockEntity(pos) as? MEGearboxBlockEntity ?: return InteractionResult.PASS
            be.toggleMode()
            val modeKey = if (be.mode == MEGearboxBlockEntity.Mode.EXPORT) "appliedcreate.me_gearbox.mode.export" else "appliedcreate.me_gearbox.mode.import"
            player.displayClientMessage(Component.translatable("appliedcreate.me_gearbox.mode").append(Component.translatable(modeKey)), true)
            return InteractionResult.SUCCESS
        } else {
            // Wrench: rotate the block (default IWrenchable behavior)
            return onWrenched(state, context)
        }
    }
}
