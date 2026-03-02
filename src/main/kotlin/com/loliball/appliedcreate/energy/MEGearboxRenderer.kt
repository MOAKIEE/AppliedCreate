package com.loliball.appliedcreate.energy

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

/**
 * Renderer for the ME Gearbox block.
 *
 * Follows Create's EncasedCogRenderer pattern:
 * - Renders a SHAFTLESS_COGWHEEL partial model spinning on the shaft axis
 * - Renders SHAFT_HALF on both ends of the FACING axis (front/back shaft stubs)
 *
 * NOTE: We bypass the Flywheel visualization check because this mod does not
 * register Flywheel visuals for custom block entities. If Flywheel is active,
 * the parent's renderSafe() would skip all software rendering, causing nothing
 * to appear. We always use software rendering instead.
 */
class MEGearboxRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<MEGearboxBlockEntity>(context) {

    override fun renderSafe(
        be: MEGearboxBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        // Always render in software mode — no Flywheel visual registered for this BE.
        // Replicate KineticBlockEntityRenderer.renderSafe() logic without the Flywheel guard.
        val state = getRenderedBlockState(be)
        val type = getRenderType(be, state)
        renderRotatingBuffer(be, getRotatedModel(be, state), ms, buffer.getBuffer(type), light)

        // Render shaft halves on both ends of the FACING axis (EncasedCogRenderer pattern)
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

    override fun getRotatedModel(be: MEGearboxBlockEntity, state: BlockState): SuperByteBuffer {
        val facing = state.getValue(DirectionalKineticBlock.FACING)
        return CachedBuffers.partialFacingVertical(
            AllPartialModels.SHAFTLESS_COGWHEEL,
            state,
            Direction.fromAxisAndDirection(facing.axis, Direction.AxisDirection.POSITIVE)
        )
    }

    override fun shouldRenderOffScreen(be: MEGearboxBlockEntity): Boolean = false
}
