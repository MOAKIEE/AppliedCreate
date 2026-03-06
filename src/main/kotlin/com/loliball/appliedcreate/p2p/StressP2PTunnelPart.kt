package com.loliball.appliedcreate.p2p

import appeng.api.parts.IPartItem
import appeng.api.networking.IGridNodeListener
import appeng.api.parts.IPartModel
import appeng.items.parts.PartModels
import appeng.parts.p2p.P2PModels
import appeng.parts.p2p.P2PTunnelPart
import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.kinetics.RotationPropagator
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

    /** Whether this part has completed its initial world-load sequence.
     *  During world load, Create rebuilds kinetic networks from NBT on each block's
     *  first tick (attachKinetics → RotationPropagator.handleAdded). We must NOT
     *  register virtual edges until AFTER this completes, or Create's BFS will
     *  traverse our edges during its first-tick propagation and corrupt stress accounting.
     *  We delay edge registration until isActive first becomes true (AE2 grid online),
     *  which happens several ticks after world load — well after Create's first-tick. */
    private var initialLoadComplete = false

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
        initialLoadComplete = false
        logger.debug("[StressP2P@{}] >>> addToWorld (isOutput={})",
            blockEntity.blockPos, isOutput)
        // Do NOT register virtual edges here — Create's first-tick attachKinetics()
        // would traverse them and corrupt stress accounting. Registration is deferred
        // until the AE2 grid comes online (isActive=true in onMainNodeStateChanged).
    }

    override fun removeFromWorld() {
        logger.debug("[StressP2P@{}] >>> removeFromWorld (isOutput={})",
            blockEntity.blockPos, isOutput)
        unregisterKineticBridge()
        super.removeFromWorld()
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        super.onMainNodeStateChanged(reason)
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        logger.debug("[StressP2P@{}] >>> onMainNodeStateChanged reason={}, isActive={}, initialLoadComplete={}, isOutput={}",
            blockEntity.blockPos, reason, isActive, initialLoadComplete, isOutput)

        if (!initialLoadComplete) {
            if (isActive) {
                // Grid is now fully online for the first time.
                // By this point Create's first-tick attachKinetics() has already run
                // and kinetic networks are stable. Safe to register virtual edges now.
                initialLoadComplete = true
                logger.debug("[StressP2P@{}]   -> first activation, registering bridge + scheduling propagation",
                    blockEntity.blockPos)
                registerKineticBridge()
                // Schedule propagation for next tick to let all tunnels register first
                scheduleKineticPropagation()
            }
            // During boot, ignore ALL state changes until isActive becomes true
            return
        }

        // Runtime state changes (after initial boot)
        if (isActive) {
            registerKineticBridge()
        } else {
            unregisterKineticBridge()
        }
    }

    override fun onTunnelNetworkChange() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        logger.debug("[StressP2P@{}] >>> onTunnelNetworkChange (isOutput={}, initialLoadComplete={}, registeredInputPos={})",
            blockEntity.blockPos, isOutput, initialLoadComplete, registeredInputPos)

        if (!initialLoadComplete) {
            // During boot, do nothing — edges will be registered when isActive becomes true
            logger.debug("[StressP2P@{}]   -> during init load, skipping", blockEntity.blockPos)
            return
        }

        logger.debug("[StressP2P@{}]   -> runtime change, calling reRegisterBridge + outputs", blockEntity.blockPos)
        reRegisterBridge()

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
            logger.debug("[StressP2P@{}]   registerKineticBridge: inputPos=null, scheduling retry", blockEntity.blockPos)
            scheduleRetry()
            return
        }

        val kineticPos = blockEntity.blockPos.relative(this.side)

        val be = level.getBlockEntity(kineticPos)
        if (be !is KineticBlockEntity) {
            logger.debug("[StressP2P@{}]   registerKineticBridge: no KineticBE at {}", blockEntity.blockPos, kineticPos)
            return
        }

        // Skip if already registered with same positions
        if (registeredInputPos == inputPos && registeredKineticPos == kineticPos) {
            logger.debug("[StressP2P@{}]   registerKineticBridge: already registered (input={}, kinetic={})",
                blockEntity.blockPos, inputPos, kineticPos)
            return
        }

        KineticBridgeRegistry.register(inputPos, kineticPos)
        registeredInputPos = inputPos
        registeredKineticPos = kineticPos
        retryAttemptsRemaining = 0

        logger.debug("[StressP2P@{}]   registerKineticBridge: REGISTERED inputTunnel={}, kinetic={}",
            blockEntity.blockPos, inputPos, kineticPos)
    }

    private fun unregisterKineticBridge() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        val inputPos = registeredInputPos ?: return
        val kineticPos = registeredKineticPos ?: return

        logger.debug("[StressP2P@{}]   unregisterKineticBridge: input={}, kinetic={}",
            blockEntity.blockPos, inputPos, kineticPos)

        val be = level.getBlockEntity(kineticPos) as? KineticBlockEntity
        if (be != null && be.getTheoreticalSpeed() != 0f) {
            logger.debug("[StressP2P@{}]   Phase 1: handleRemoved (speed={})", blockEntity.blockPos, be.getTheoreticalSpeed())
            RotationPropagator.handleRemoved(level, kineticPos, be)
            if (be.hasSource()) {
                be.removeSource()
                be.sendData()
            }
        }

        KineticBridgeRegistry.unregister(inputPos, kineticPos)

        val remainingEndpoints = KineticBridgeRegistry.getEndpoints(inputPos)
        for (partnerPos in remainingEndpoints) {
            val partnerBE = level.getBlockEntity(partnerPos) as? KineticBlockEntity ?: continue
            RotationPropagator.handleAdded(level, partnerPos, partnerBE)
        }

        if (be != null) {
            RotationPropagator.handleAdded(level, kineticPos, be)
        }

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

        logger.debug("[StressP2P@{}]   reRegisterBridge: old=({},{}), new=({},{})",
            blockEntity.blockPos, oldInputPos, oldKineticPos, newInputPos, newKineticPos)

        if (oldInputPos == newInputPos && oldKineticPos == newKineticPos) {
            logger.debug("[StressP2P@{}]   reRegisterBridge: nothing changed, skip", blockEntity.blockPos)
            return
        }

        // Phase 1: Unregister old edges and propagate removal on old endpoints
        if (oldInputPos != null && oldKineticPos != null) {
            val oldEndpoints = KineticBridgeRegistry.getEndpoints(oldInputPos).toList()
            KineticBridgeRegistry.unregister(oldInputPos, oldKineticPos)
            // handleRemoved on the old kinetic pos
            val oldBE = level.getBlockEntity(oldKineticPos) as? KineticBlockEntity
            if (oldBE != null && oldBE.getTheoreticalSpeed() != 0f) {
                RotationPropagator.handleRemoved(level, oldKineticPos, oldBE)
            }
            // handleAdded on remaining endpoints to let them re-discover
            for (partnerPos in oldEndpoints) {
                if (partnerPos == oldKineticPos) continue
                val partnerBE = level.getBlockEntity(partnerPos) as? KineticBlockEntity ?: continue
                RotationPropagator.handleAdded(level, partnerPos, partnerBE)
            }
            if (oldBE != null) {
                RotationPropagator.handleAdded(level, oldKineticPos, oldBE)
            }
        }

        // Phase 2: Register new edges and propagate on new endpoints
        if (newInputPos != null) {
            val be = level.getBlockEntity(newKineticPos)
            if (be is KineticBlockEntity) {
                KineticBridgeRegistry.register(newInputPos, newKineticPos)
                registeredInputPos = newInputPos
                registeredKineticPos = newKineticPos
                // handleAdded on all endpoints in the new group
                val newEndpoints = KineticBridgeRegistry.getEndpoints(newInputPos)
                for (endpointPos in newEndpoints) {
                    val endpointBE = level.getBlockEntity(endpointPos) as? KineticBlockEntity ?: continue
                    RotationPropagator.handleAdded(level, endpointPos, endpointBE)
                }
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
     * Schedule kinetic propagation for the next server tick.
     * This is called after first-time bridge registration (when AE2 grid comes online)
     * to let Create discover the newly-connected virtual edges.
     * Deferred by one tick to ensure all tunnel parts in the group have registered.
     */
    private fun scheduleKineticPropagation() {
        val level = blockEntity.level as? ServerLevel ?: return
        val server = level.server
        val inputPos = registeredInputPos ?: return
        logger.debug("[StressP2P@{}] scheduleKineticPropagation for next tick (inputTunnel={})",
            blockEntity.blockPos, inputPos)
        server.tell(TickTask(server.tickCount + 1) {
            if (blockEntity.isRemoved) return@TickTask
            if (registeredInputPos != inputPos) return@TickTask // changed since scheduled
            val endpoints = KineticBridgeRegistry.getEndpoints(inputPos)
            logger.debug("[StressP2P@{}] propagation tick: {} endpoints for tunnel {}",
                blockEntity.blockPos, endpoints.size, inputPos)
            for (endpointPos in endpoints) {
                val be = level.getBlockEntity(endpointPos) as? KineticBlockEntity ?: continue
                RotationPropagator.handleAdded(level, endpointPos, be)
            }
        })
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
        logger.debug("[StressP2P@{}] scheduleRetry ({} attempts left)",
            blockEntity.blockPos, attemptsLeft)
        server.tell(TickTask(server.tickCount + 1) {
            if (registeredInputPos != null) {
                logger.debug("[StressP2P@{}] retry: already registered, skip", blockEntity.blockPos)
                return@TickTask
            }
            if (blockEntity.isRemoved) return@TickTask
            val inputPos = findInputTunnelPos()
            if (inputPos != null) {
                logger.debug("[StressP2P@{}] retry: grid ready, calling registerKineticBridge", blockEntity.blockPos)
                registerKineticBridge()
            } else if (attemptsLeft > 0) {
                scheduleRetry()
            } else {
                logger.debug("[StressP2P@{}] retry: giving up — grid never came online", blockEntity.blockPos)
            }
        })
    }

}
