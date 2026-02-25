package com.loliball.appliedcreate.p2p

import appeng.api.parts.IPartItem
import appeng.api.parts.IPartModel
import appeng.items.parts.PartModels
import appeng.parts.p2p.P2PModels
import appeng.parts.p2p.P2PTunnelPart
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.kinetic.StressAcceptorBlockEntity
import com.loliball.appliedcreate.kinetic.StressProviderBlockEntity
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation

/**
 * Stress P2P Tunnel Part — bridges Create kinetic networks through AE2 ME networks.
 *
 * This P2P tunnel does NOT extend CapabilityP2PTunnelPart because Create's rotation system
 * is NOT capability-based — it uses adjacency-based RotationPropagator.
 *
 * Instead, companion blocks (StressAcceptor on input side, StressProvider on output side)
 * sit adjacent to the cable bus containing this P2P part. They communicate speed and stress
 * capacity through this tunnel.
 *
 * Input flow: Kinetic Network → StressAcceptorBlockEntity → StressP2PTunnelPart (input) → ME Network
 * Output flow: ME Network → StressP2PTunnelPart (output) → StressProviderBlockEntity → Kinetic Network
 */
class StressP2PTunnelPart(partItem: IPartItem<*>) : P2PTunnelPart<StressP2PTunnelPart>(partItem) {

    // Cached values from the input-side companion block
    private var transferSpeed: Float = 0f
    private var transferStressCapacity: Float = 0f

    override fun getStaticModels(): IPartModel {
        return MODELS.getModel(this.isPowered, this.isActive)
    }

    override fun readFromNBT(data: CompoundTag, registries: HolderLookup.Provider) {
        super.readFromNBT(data, registries)
        transferSpeed = data.getFloat("TransferSpeed")
        transferStressCapacity = data.getFloat("TransferStressCapacity")
    }

    override fun writeToNBT(data: CompoundTag, registries: HolderLookup.Provider) {
        super.writeToNBT(data, registries)
        data.putFloat("TransferSpeed", transferSpeed)
        data.putFloat("TransferStressCapacity", transferStressCapacity)
    }

    /**
     * Called by the input-side StressAcceptorBlockEntity to update the values to transfer.
     */
    fun updateInputValues(speed: Float, stressCapacity: Float) {
        if (isOutput) return  // Only input side accepts updates
        this.transferSpeed = speed
        this.transferStressCapacity = stressCapacity
        host.markForSave()
        // Propagate to all output tunnels
        notifyOutputs()
    }

    /**
     * Called by output-side StressProviderBlockEntity to read the values.
     * Reads from the input tunnel of this P2P link.
     */
    fun getTransferSpeed(): Float {
        if (!isOutput) return transferSpeed  // Direct access if we ARE the input
        val input = input ?: return 0f
        return input.transferSpeed
    }

    fun getTransferStressCapacity(): Float {
        if (!isOutput) return transferStressCapacity
        val input = input ?: return 0f
        return input.transferStressCapacity
    }

    /**
     * Notify all output-side companion blocks to update their generated rotation.
     */
    private fun notifyOutputs() {
        for (output in getOutputs()) {
            output.notifyCompanion()
        }
    }

    /**
     * Notify the adjacent companion block (StressProvider) to update.
     */
    fun notifyCompanion() {
        val level = blockEntity.level ?: return
        val pos = blockEntity.blockPos
        val side = this.side
        val companionPos = pos.relative(side)
        val be = level.getBlockEntity(companionPos)
        if (be is StressProviderBlockEntity) {
            be.updateFromTunnel()
        }
    }

    /**
     * Find the adjacent StressAcceptorBlockEntity (for input side) or StressProviderBlockEntity (for output side).
     */
    fun findCompanionAcceptor(): StressAcceptorBlockEntity? {
        val level = blockEntity.level ?: return null
        val companionPos = blockEntity.blockPos.relative(this.side)
        val be = level.getBlockEntity(companionPos)
        return be as? StressAcceptorBlockEntity
    }

    override fun onTunnelNetworkChange() {
        // When P2P network changes (new outputs linked, etc.), notify output companions
        notifyOutputs()
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
