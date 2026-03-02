package com.loliball.appliedcreate.energy

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import dev.engine_room.flywheel.api.visualization.VisualizationManager
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
 * - The cogwheel is rendered by the parent class via getRotatedModel() (single SHAFTLESS_COGWHEEL on axis)
 * - SHAFT_HALF is rendered on both ends of the FACING axis (front/back shaft stubs)
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
        // Let the parent render the cogwheel via getRotatedModel()
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        // Render shaft halves on both ends of the FACING axis
        if (!VisualizationManager.supportsVisualization(be.level!!)) {
            val state = be.blockState
            val facing = state.getValue(DirectionalKineticBlock.FACING)
            val shaftAxis = facing.axis
            val angle = getAngleForBe(be, be.blockPos, shaftAxis)
            val vb = buffer.getBuffer(RenderType.solid())

            for (dir in Direction.entries) {
                if (dir.axis != shaftAxis) continue

                val shaftHalf = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, dir)
                kineticRotationTransform(shaftHalf, be, shaftAxis, angle, light)
                shaftHalf.renderInto(ms, vb)
            }
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
