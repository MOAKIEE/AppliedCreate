package com.loliball.appliedcreate.kinetic

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

/**
 * Renderer for the Stress P2P Companion — renders a rotating shaft along the FACING axis.
 * Pattern follows Create's ShaftRenderer.
 */
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
        val axis = getRotationAxisOf(be)
        val shaft = CachedBuffers.block(shaft(axis))
        val angle = getAngleForBe(be, be.blockPos, axis)
        kineticRotationTransform(shaft, be, axis, angle, light)
        shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }

    override fun shouldRenderOffScreen(be: StressP2PCompanionBlockEntity): Boolean = false
}
