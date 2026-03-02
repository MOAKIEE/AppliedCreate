package com.loliball.appliedcreate.kinetic

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction

/**
 * Renderer for the Stress P2P Companion — renders SHAFT_HALF on each end of the FACING axis.
 * Uses the EncasedCogRenderer pattern to avoid z-fighting with casing geometry.
 * Bypasses Flywheel check (no Flywheel visual registered for this BE).
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
        // Bypass Flywheel — always render in software mode
        val blockState = be.blockState
        val facing = blockState.getValue(DirectionalKineticBlock.FACING)
        val shaftAxis = facing.axis
        val angle = getAngleForBe(be, be.blockPos, shaftAxis)
        val vb = buffer.getBuffer(RenderType.solid())

        // Render SHAFT_HALF on both ends of the facing axis (same as EncasedCogRenderer)
        for (dir in Direction.entries) {
            if (dir.axis != shaftAxis) continue

            val shaftHalf = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, blockState, dir)
            kineticRotationTransform(shaftHalf, be, shaftAxis, angle, light)
            shaftHalf.renderInto(ms, vb)
        }
    }

    override fun shouldRenderOffScreen(be: StressP2PCompanionBlockEntity): Boolean = false
}
