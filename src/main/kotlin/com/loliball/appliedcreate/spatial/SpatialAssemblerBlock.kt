package com.loliball.appliedcreate.spatial

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.item.ItemHelper
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class SpatialAssemblerBlock(properties: Properties) : Block(properties), IBE<SpatialAssemblerBlockEntity> {

    init {
        registerDefaultState(defaultBlockState())
    }

    override fun getShape(state: BlockState, worldIn: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return SHAPE
    }

    override fun useWithoutItem(
        state: BlockState, worldIn: Level, pos: BlockPos,
        player: Player, hit: BlockHitResult
    ): InteractionResult {
        if (worldIn.isClientSide) {
            return InteractionResult.SUCCESS
        }
        withBlockEntityDo(worldIn, pos) { be ->
            (player as ServerPlayer).openMenu(be) { buf -> be.sendToMenu(buf) }
        }
        return InteractionResult.SUCCESS
    }

    override fun neighborChanged(
        state: BlockState, worldIn: Level, pos: BlockPos,
        blockIn: Block, fromPos: BlockPos, isMoving: Boolean
    ) {
        withBlockEntityDo(worldIn, pos) { be -> be.updateRedstoneState() }
    }

    override fun onRemove(state: BlockState, worldIn: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!state.hasBlockEntity() || state.block == newState.block) {
            return
        }
        withBlockEntityDo(worldIn, pos) { be ->
            ItemHelper.dropContents(worldIn, pos, be.cellInventory)
        }
        worldIn.removeBlockEntity(pos)
    }

    override fun getBlockEntityClass(): Class<SpatialAssemblerBlockEntity> {
        return SpatialAssemblerBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SpatialAssemblerBlockEntity> {
        return AppliedCreate.SPATIAL_ASSEMBLER_BE.get()
    }

    companion object {
        val SHAPE: VoxelShape = box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
    }
}
