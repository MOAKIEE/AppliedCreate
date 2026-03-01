package com.loliball.appliedcreate.energy

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class KineticEnergyAcceptorRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<KineticEnergyAcceptorBlockEntity>(context) {

    override fun renderSafe(
        be: KineticEnergyAcceptorBlockEntity, partialTicks: Float, ms: PoseStack,
        buffer: MultiBufferSource, light: Int, overlay: Int
    ) {
        val axis = getRotationAxisOf(be)
        val shaft = CachedBuffers.block(shaft(axis))
        val angle = getAngleForBe(be, be.blockPos, axis)
        kineticRotationTransform(shaft, be, axis, angle, light)
        shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }

    override fun shouldRenderOffScreen(be: KineticEnergyAcceptorBlockEntity): Boolean = false
}
