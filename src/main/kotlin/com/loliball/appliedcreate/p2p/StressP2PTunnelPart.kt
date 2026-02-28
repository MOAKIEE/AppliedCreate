package com.loliball.appliedcreate.p2p

import appeng.api.parts.IPartItem
import appeng.api.parts.IPartModel
import appeng.items.parts.PartModels
import appeng.parts.p2p.P2PModels
import appeng.parts.p2p.P2PTunnelPart
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.kinetic.StressP2PCompanionBlock
import com.loliball.appliedcreate.kinetic.StressP2PCompanionBlockEntity
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory
/**
 * Stress P2P Tunnel Part — bridges Create kinetic networks through AE2 ME networks.
 *
 * This P2P tunnel does NOT extend CapabilityP2PTunnelPart because Create's rotation system
 * is NOT capability-based — it uses adjacency-based RotationPropagator.
 *
 * Instead, StressP2PCompanion blocks sit adjacent to the cable bus containing this
 * P2P part. They participate in Create's custom connection system to bridge kinetic
 * networks through the tunnel.
 *
 * Flow: Kinetic Network → StressP2PCompanion → StressP2PTunnelPart → (P2P link) → StressP2PCompanion → Kinetic Network
 *
 * Companion blocks handle the kinetic bridging via Create's custom connection system.
 * This tunnel part just manages the P2P link and notifies companions of changes.
 */
class StressP2PTunnelPart(partItem: IPartItem<*>) : P2PTunnelPart<StressP2PTunnelPart>(partItem) {

    private val logger = LoggerFactory.getLogger("AppliedCreate/StressP2PTunnel")

    override fun getStaticModels(): IPartModel {
        return MODELS.getModel(this.isPowered, this.isActive)
    }

    // ── Auto-placement of companion block ──

    /**
     * Auto-place companion block when this P2P tunnel is added to the world.
     * The companion is placed one block out from the cable bus, facing back toward it.
     */
    override fun addToWorld() {
        super.addToWorld()
        tryPlaceCompanion()
    }

    /**
     * Auto-remove companion block when this P2P tunnel is removed from the world.
     */
    override fun removeFromWorld() {
        tryRemoveCompanion()
        super.removeFromWorld()
    }

    private fun tryPlaceCompanion() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return
        val cableBusPos = blockEntity.blockPos
        val companionPos = cableBusPos.relative(this.side)

        // Only place if the target position is air/replaceable
        val existingState = level.getBlockState(companionPos)
        if (!existingState.isAir && !existingState.canBeReplaced()) {
            logger.warn("[StressP2P@{}] Cannot auto-place companion at {} — block already present: {}",
                cableBusPos, companionPos, existingState.block)
            return
        }

        val companionBlock = AppliedCreate.STRESS_P2P_COMPANION_BLOCK.get()
        // FACING points toward the cable bus = opposite of the tunnel's outward side
        val companionState = companionBlock.defaultBlockState()
            .setValue(DirectionalKineticBlock.FACING, this.side.opposite)
        level.setBlock(companionPos, companionState, 3)
        logger.debug("[StressP2P@{}] Auto-placed companion at {} facing {}",
            cableBusPos, companionPos, this.side.opposite)
    }

    private fun tryRemoveCompanion() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return
        val cableBusPos = blockEntity.blockPos
        val companionPos = cableBusPos.relative(this.side)

        val state = level.getBlockState(companionPos)
        if (state.block is StressP2PCompanionBlock) {
            level.removeBlock(companionPos, false)
            logger.debug("[StressP2P@{}] Auto-removed companion at {}",
                cableBusPos, companionPos)
        }
    }

    // ── P2P Network Notifications ──

    /**
     * Notify all output-side companion blocks to update their registration.
     */
    private fun notifyOutputs() {
        for (output in getOutputs()) {
            output.notifyCompanion()
        }
    }

    /**
     * Notify the adjacent companion block to update its registration.
     */
    fun notifyCompanion() {
        val level = blockEntity.level ?: return
        val pos = blockEntity.blockPos
        val side = this.side
        val companionPos = pos.relative(side)
        val be = level.getBlockEntity(companionPos)

        if (be is StressP2PCompanionBlockEntity) {
            be.reloadKinetics()
        }
    }

    override fun onTunnelNetworkChange() {
        // Guard: don't trigger kinetic cascades during grid shutdown.
        // During save/unload, mainNode.destroy() fires this callback but the level may be
        // ticking block entity removal — triggering detachKinetics/attachKinetics here causes hangs.
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        // When P2P network changes (new outputs linked, etc.), notify all companions
        notifyCompanion() // Update own companion
        notifyOutputs()   // Update outputs' companions
    }

    companion object {
        private val MODELS = P2PModels(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "part/p2p/p2p_tunnel_stress")
        )

        @JvmStatic
        @PartModels
        fun getModels(): List<IPartModel> {
            return MODELS.models
        }
    }
}
