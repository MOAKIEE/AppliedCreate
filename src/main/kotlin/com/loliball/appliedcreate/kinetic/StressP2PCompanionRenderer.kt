package com.loliball.appliedcreate.kinetic

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.state.BlockState

/**
 * Renderer for the Stress P2P Companion — renders a rotating shaft along the FACING axis.
 * Pattern follows Create's ShaftRenderer: override getRenderedBlockState to return the shaft block state.
 */
class StressP2PCompanionRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<StressP2PCompanionBlockEntity>(context) {

    override fun getRenderedBlockState(be: StressP2PCompanionBlockEntity): BlockState {
        return shaft(getRotationAxisOf(be))
    }

    override fun shouldRenderOffScreen(be: StressP2PCompanionBlockEntity): Boolean = false
}
