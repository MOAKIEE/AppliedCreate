package com.loliball.appliedcreate.energy

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.utility.AnimationTickHolder
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction

class MEGearboxRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<MEGearboxBlockEntity>(context) {

    override fun renderSafe(
        be: MEGearboxBlockEntity, partialTicks: Float, ms: PoseStack,
        buffer: MultiBufferSource, light: Int, overlay: Int
    ) {
        val state = be.blockState
        val facing = state.getValue(DirectionalKineticBlock.FACING)
        val shaftAxis = facing.axis
        val speed = be.speed
        val time = AnimationTickHolder.getRenderTime()
        val vb = buffer.getBuffer(RenderType.solid())

        for (dir in Direction.entries) {
            if (dir.axis != shaftAxis) continue
            val shaftHalf = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, dir)
            val offset = getRotationOffsetForPosition(be, be.blockPos, shaftAxis)
            val angle = ((time * speed * 3f / 10f + offset) % 360f) / 180f * Math.PI.toFloat()
            kineticRotationTransform(shaftHalf, be, shaftAxis, angle, light)
            shaftHalf.renderInto(ms, vb)
        }

        for (dir in Direction.entries) {
            if (dir.axis == shaftAxis) continue
            val cogwheel = CachedBuffers.partialFacing(AllPartialModels.SHAFTLESS_COGWHEEL, state, dir)
            val axis = dir.axis
            val offset = getRotationOffsetForPosition(be, be.blockPos, axis)
            var angle = ((time * speed * 3f / 10f + offset) % 360f)
            if (dir.axisDirection == Direction.AxisDirection.NEGATIVE) angle = -angle
            angle = angle / 180f * Math.PI.toFloat()
            kineticRotationTransform(cogwheel, be, axis, angle, light)
            cogwheel.renderInto(ms, vb)
        }
    }

    override fun shouldRenderOffScreen(be: MEGearboxBlockEntity): Boolean = false
}
