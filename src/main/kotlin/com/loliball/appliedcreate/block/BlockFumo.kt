package com.loliball.appliedcreate.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * A decorative Fumo block - author tribute plushies.
 * Based on ExtendedAE's BlockFishbig implementation.
 */
class BlockFumo : Block(
    Properties.of()
        .mapColor(MapColor.NONE)
        .sound(SoundType.WOOL)
        .strength(0.5f)
        .noOcclusion()
) {
    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING, WATERLOGGED)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val fluidState = context.level.getFluidState(context.clickedPos)
        return this.defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
            .setValue(WATERLOGGED, fluidState.type == Fluids.WATER)
    }

    override fun isCollisionShapeFullBlock(state: BlockState, level: BlockGetter, pos: BlockPos) = false

    override fun getLightBlock(state: BlockState, level: BlockGetter, pos: BlockPos) = 2

    override fun getShadeBrightness(state: BlockState, level: BlockGetter, pos: BlockPos) = 1.0f

    override fun propagatesSkylightDown(state: BlockState, level: BlockGetter, pos: BlockPos) = true

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED))
            Fluids.WATER.getSource(false)
        else
            super.getFluidState(state)
    }

    override fun updateShape(
        state: BlockState,
        facing: Direction,
        facingState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        facingPos: BlockPos
    ): BlockState {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return getShapeForFacing(state.getValue(FACING))
    }

    override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return getShapeForFacing(state.getValue(FACING))
    }

    /**
     * Returns the VoxelShape for the given facing direction.
     */
    private fun getShapeForFacing(facing: Direction): VoxelShape {
        return when (facing) {
            Direction.SOUTH -> SHAPE_SOUTH
            Direction.WEST -> SHAPE_WEST
            Direction.EAST -> SHAPE_EAST
            else -> SHAPE_NORTH
        }
    }

    companion object {
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED

        // Actual model bounds (in 1/16 block units):
        // X: ~3.9 to ~12.2, Y: ~0 to ~13.6, Z: ~4 to ~14.7
        // These shapes are for when the model faces NORTH (default model orientation)
        private val SHAPE_NORTH: VoxelShape = box(3.9, 0.0, 4.0, 12.2, 13.6, 14.7)
        private val SHAPE_SOUTH: VoxelShape = box(3.8, 0.0, 1.3, 12.1, 13.6, 12.0)
        private val SHAPE_WEST: VoxelShape = box(4.0, 0.0, 3.8, 14.7, 13.6, 12.2)
        private val SHAPE_EAST: VoxelShape = box(1.3, 0.0, 3.8, 12.0, 13.6, 12.2)
    }
}