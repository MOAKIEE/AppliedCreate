package com.loliball.appliedcreate.block

import com.loliball.appliedcreate.block.entity.MechanicalCraftEncoderBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.network.NetworkHooks

class MechanicalCraftEncoderBlock : Block(
    Properties.of()
        .mapColor(MapColor.METAL)
        .strength(1.5f, 6.0f)
        .sound(SoundType.METAL)
        .requiresCorrectToolForDrops()
), EntityBlock {

    companion object {
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return MechanicalCraftEncoderBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        return BlockEntityTicker { tickLevel, tickPos, tickState, blockEntity ->
            if (blockEntity is MechanicalCraftEncoderBlockEntity) {
                blockEntity.serverTick(tickLevel, tickPos, tickState)
            }
        }
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
        if (!level.isClientSide && player is ServerPlayer) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is MechanicalCraftEncoderBlockEntity) {
                NetworkHooks.openScreen(player, blockEntity, pos)
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    @Suppress("DEPRECATION")
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moving: Boolean) {
        if (!state.`is`(newState.block)) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is MechanicalCraftEncoderBlockEntity) {
                blockEntity.dropContents(level, pos)
            }
            super.onRemove(state, level, pos, newState, moving)
        }
    }

    @Suppress("DEPRECATION")
    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        neighborBlock: Block,
        neighborPos: BlockPos,
        moving: Boolean
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, moving)
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is MechanicalCraftEncoderBlockEntity) {
                blockEntity.onNeighborChanged()
            }
        }
    }
}
