package com.loliball.appliedcreate.kinetic

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.state.BlockState

/**
 * Renderer for the Stress P2P Companion — renders a rotating shaft along the FACING axis.
 * Bypasses Flywheel check (no Flywheel visual registered for this BE).
 */
class StressP2PCompanionRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<StressP2PCompanionBlockEntity>(context) {

    override fun getRenderedBlockState(be: StressP2PCompanionBlockEntity): BlockState {
        return shaft(getRotationAxisOf(be))
    }

    override fun renderSafe(
        be: StressP2PCompanionBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        // Bypass Flywheel — always render in software mode
        val state = getRenderedBlockState(be)
        val type = getRenderType(be, state)
        renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(type), light)
    }

    override fun shouldRenderOffScreen(be: StressP2PCompanionBlockEntity): Boolean = false
}
