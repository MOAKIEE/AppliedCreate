package com.loliball.appliedcreate.p2p

import appeng.api.parts.IPartItem
import appeng.api.parts.IPartModel
import appeng.items.parts.PartModels
import appeng.parts.p2p.P2PModels
import appeng.parts.p2p.P2PTunnelPart
import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel
import org.slf4j.LoggerFactory

/**
 * Stress P2P Tunnel Part — bridges Create kinetic networks through AE2 ME networks.
 *
 * This P2P tunnel does NOT extend CapabilityP2PTunnelPart because Create's rotation system
 * is NOT capability-based — it uses adjacency-based RotationPropagator.
 *
 * Instead of using physical companion blocks, this tunnel part registers virtual edges
 * in [KineticBridgeRegistry]. The [com.loliball.appliedcreate.mixin.RotationPropagatorMixin]
 * makes Create's propagation BFS traverse these virtual edges, bridging kinetic networks
 * through the P2P link without any intermediary blocks.
 *
 * Flow: Kinetic Block → [adjacent to cable bus] → RotationPropagatorMixin → [virtual edge] → Kinetic Block
 */
class StressP2PTunnelPart(partItem: IPartItem<*>) : P2PTunnelPart<StressP2PTunnelPart>(partItem) {

    private val logger = LoggerFactory.getLogger("AppliedCreate/StressP2PTunnel")

    /** The input tunnel pos this part is registered under in KineticBridgeRegistry */
    private var registeredInputPos: BlockPos? = null

    /** The kinetic block pos registered in KineticBridgeRegistry */
    private var registeredKineticPos: BlockPos? = null

    /** Number of deferred registration retry attempts remaining */
    private var retryAttemptsRemaining = 0

    companion object {
        /** Max number of tick-deferred retries for registration when grid isn't ready */
        private const val MAX_RETRY_ATTEMPTS = 5

        private val MODELS = P2PModels(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "part/p2p/p2p_tunnel_stress")
        )

        @JvmStatic
        @PartModels
        fun getModels(): List<IPartModel> {
            return MODELS.models
        }
    }

    override fun getStaticModels(): IPartModel {
        return MODELS.getModel(this.isPowered, this.isActive)
    }

    // ── Lifecycle: Register/unregister virtual kinetic edges ──

    override fun addToWorld() {
        super.addToWorld()
        registerKineticBridge()
    }

    override fun removeFromWorld() {
        unregisterKineticBridge()
        super.removeFromWorld()
    }

    override fun onTunnelNetworkChange() {
        // Guard: don't trigger kinetic cascades during grid shutdown.
        // During save/unload, mainNode.destroy() fires this callback but the level may be
        // ticking block entity removal — triggering detachKinetics/attachKinetics here causes hangs.
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        // When P2P network changes (new outputs linked, etc.), re-register all bridges
        reRegisterBridge()

        // Also re-register all outputs
        for (output in getOutputs()) {
            output.reRegisterBridge()
        }
    }

    // ── Bridge Registration ──

    private fun registerKineticBridge() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        val inputPos = findInputTunnelPos()
        if (inputPos == null) {
            // Grid not ready yet (common for output tunnels on world load).
            // Schedule a tick-deferred retry.
            scheduleRetry()
            return
        }

        val kineticPos = blockEntity.blockPos.relative(this.side)

        // Verify there's actually a kinetic block at the adjacent position
        val be = level.getBlockEntity(kineticPos)
        if (be !is KineticBlockEntity) {
            logger.debug("[StressP2P@{}] No kinetic block at {} — skipping registration",
                blockEntity.blockPos, kineticPos)
            return
        }

        KineticBridgeRegistry.register(inputPos, kineticPos, level as ServerLevel)
        registeredInputPos = inputPos
        registeredKineticPos = kineticPos
        retryAttemptsRemaining = 0

        logger.debug("[StressP2P@{}] Registered bridge: inputTunnel={}, kinetic={}",
            blockEntity.blockPos, inputPos, kineticPos)

        // Note: Do NOT trigger re-propagation here. During world load, kinetic networks
        // are already correctly restored from NBT — calling triggerRepropagation would tear
        // them down and cause stress overload on the output side. Runtime P2P changes are
        // handled by onTunnelNetworkChange → reRegisterBridge which does trigger re-propagation.
    }

    private fun unregisterKineticBridge() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        val inputPos = registeredInputPos ?: return
        val kineticPos = registeredKineticPos ?: return

        KineticBridgeRegistry.unregister(inputPos, kineticPos)

        logger.debug("[StressP2P@{}] Unregistered bridge: inputTunnel={}, kinetic={}",
            blockEntity.blockPos, inputPos, kineticPos)

        // Trigger re-propagation so partners update their networks
        KineticBridgeRegistry.triggerRepropagation(inputPos)

        registeredInputPos = null
        registeredKineticPos = null
    }

    /**
     * Re-register bridge: unregister old, register new.
     * Called when P2P tunnel network changes (memory card, etc.)
     */
    internal fun reRegisterBridge() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        val oldInputPos = registeredInputPos
        val oldKineticPos = registeredKineticPos
        val newInputPos = findInputTunnelPos()
        val newKineticPos = blockEntity.blockPos.relative(this.side)

        // Skip if nothing changed
        if (oldInputPos == newInputPos && oldKineticPos == newKineticPos) return

        // Unregister old
        if (oldInputPos != null && oldKineticPos != null) {
            KineticBridgeRegistry.unregister(oldInputPos, oldKineticPos)
            KineticBridgeRegistry.triggerRepropagation(oldInputPos)
        }

        // Register new (if kinetic block exists)
        if (newInputPos != null) {
            val be = level.getBlockEntity(newKineticPos)
            if (be is KineticBlockEntity) {
                KineticBridgeRegistry.register(newInputPos, newKineticPos, level as ServerLevel)
                registeredInputPos = newInputPos
                registeredKineticPos = newKineticPos
                KineticBridgeRegistry.triggerRepropagation(newInputPos)
                logger.debug("[StressP2P@{}] Re-registered bridge: inputTunnel={}, kinetic={}",
                    blockEntity.blockPos, newInputPos, newKineticPos)
                return
            }
        }

        registeredInputPos = null
        registeredKineticPos = null
    }

    /**
     * Find the P2P input tunnel's BlockPos.
     * For input tunnels: this IS the input — return own cable bus pos.
     * For output tunnels: follow the P2P link back to the input tunnel's pos.
     */
    private fun findInputTunnelPos(): BlockPos? {
        return if (!this.isOutput) {
            // This IS the input tunnel
            blockEntity.blockPos
        } else {
            // This is an output tunnel; follow P2P link to get input's position
            // NOTE: Requires ME grid to be online (input uses P2PService)
            this.input?.blockEntity?.blockPos
        }
    }

    /**
     * Schedule a tick-deferred retry for bridge registration.
     * This handles the case where output tunnels are added to world before
     * the ME grid is fully online (findInputTunnelPos returns null).
     */
    private fun scheduleRetry() {
        if (retryAttemptsRemaining <= 0) {
            retryAttemptsRemaining = MAX_RETRY_ATTEMPTS
        }
        val level = blockEntity.level as? ServerLevel ?: return
        val server = level.server
        retryAttemptsRemaining--
        val attemptsLeft = retryAttemptsRemaining
        logger.debug("[StressP2P@{}] Grid not ready, scheduling retry ({} attempts left)",
            blockEntity.blockPos, attemptsLeft)
        server.tell(TickTask(server.tickCount + 1) {
            // Verify part is still in world (may have been removed during the tick delay)
            if (registeredInputPos != null) return@TickTask  // Already registered by onTunnelNetworkChange
            if (blockEntity.isRemoved) return@TickTask
            val inputPos = findInputTunnelPos()
            if (inputPos != null) {
                registerKineticBridge()
            } else if (attemptsLeft > 0) {
                scheduleRetry()
            } else {
                logger.debug("[StressP2P@{}] Giving up registration retry — grid never came online",
                    blockEntity.blockPos)
            }
        })
    }

}
