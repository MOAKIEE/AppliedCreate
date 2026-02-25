package com.loliball.appliedcreate.p2p

import appeng.api.parts.IPartItem
import appeng.api.parts.IPartModel
import appeng.items.parts.PartModels
import appeng.parts.p2p.P2PModels
import appeng.parts.p2p.P2PTunnelPart
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.kinetic.StressAcceptorBlockEntity
import com.loliball.appliedcreate.kinetic.StressProviderBlockEntity
import net.minecraft.resources.ResourceLocation

/**
 * Stress P2P Tunnel Part — bridges Create kinetic networks through AE2 ME networks.
 *
 * This P2P tunnel does NOT extend CapabilityP2PTunnelPart because Create's rotation system
 * is NOT capability-based — it uses adjacency-based RotationPropagator.
 *
 * Instead, companion blocks (StressAcceptor on input side, StressProvider on output side)
 * sit adjacent to the cable bus containing this P2P part. They communicate speed
 * through this tunnel.
 *
 * Input flow: Kinetic Network → StressAcceptorBlockEntity → StressP2PTunnelPart (input) → ME Network
 * Output flow: ME Network → StressP2PTunnelPart (output) → StressProviderBlockEntity → Kinetic Network
 *
 * Companion blocks handle the kinetic bridging via Create's custom connection system.
 * This tunnel part just manages the P2P link and notifies companions of changes.
 */
class StressP2PTunnelPart(partItem: IPartItem<*>) : P2PTunnelPart<StressP2PTunnelPart>(partItem) {

    override fun getStaticModels(): IPartModel {
        return MODELS.getModel(this.isPowered, this.isActive)
    }

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
        
        if (be is StressProviderBlockEntity) {
            be.reloadKinetics()
        } else if (be is StressAcceptorBlockEntity) {
            be.updateRegistration()
        }
    }


    override fun onTunnelNetworkChange() {
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
