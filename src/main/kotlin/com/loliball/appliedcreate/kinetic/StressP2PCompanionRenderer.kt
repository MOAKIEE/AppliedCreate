package com.loliball.appliedcreate.kinetic

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState

class StressP2PCompanionRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<StressP2PCompanionBlockEntity>(context) {

    override fun renderSafe(
        be: StressP2PCompanionBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        val state = getRenderedBlockState(be)
        val type = getRenderType(be, state)
        renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(type), light)

        val blockState = be.blockState
        val facing = blockState.getValue(DirectionalKineticBlock.FACING)
        val shaftAxis = facing.axis
        val angle = getAngleForBe(be, be.blockPos, shaftAxis)
        val vb = buffer.getBuffer(RenderType.solid())

        for (dir in Direction.entries) {
            if (dir.axis != shaftAxis) continue

            val shaftHalf = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, blockState, dir)
            kineticRotationTransform(shaftHalf, be, shaftAxis, angle, light)
            shaftHalf.renderInto(ms, vb)
        }
    }

    override fun getRotatedModel(be: StressP2PCompanionBlockEntity, state: BlockState): SuperByteBuffer {
        val facing = state.getValue(DirectionalKineticBlock.FACING)
        return CachedBuffers.partialFacingVertical(
            AllPartialModels.SHAFTLESS_COGWHEEL,
            state,
            Direction.fromAxisAndDirection(facing.axis, Direction.AxisDirection.POSITIVE)
        )
    }

    override fun shouldRenderOffScreen(be: StressP2PCompanionBlockEntity): Boolean = false
}
