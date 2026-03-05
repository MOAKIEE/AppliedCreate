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

    /** Whether this part has completed its initial world-load registration.
     *  During world load, kinetic networks are restored from NBT — we must NOT
     *  trigger re-propagation until the next runtime change. */
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
        logger.info("[StressP2P@{}] >>> addToWorld (isOutput={}, initialLoadComplete={})",
            blockEntity.blockPos, isOutput, initialLoadComplete)
        registerKineticBridge()
    }

    override fun removeFromWorld() {
        logger.info("[StressP2P@{}] >>> removeFromWorld (isOutput={})",
            blockEntity.blockPos, isOutput)
        unregisterKineticBridge()
        super.removeFromWorld()
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        super.onMainNodeStateChanged(reason)
        logger.info("[StressP2P@{}] >>> onMainNodeStateChanged reason={}, isActive={}, initialLoadComplete={}, isOutput={}",
            blockEntity.blockPos, reason, isActive, initialLoadComplete, isOutput)

        if (!initialLoadComplete) {
            if (reason != IGridNodeListener.State.GRID_BOOT) {
                initialLoadComplete = true
                logger.info("[StressP2P@{}]   -> initialLoadComplete set to true, calling registerKineticBridge",
                    blockEntity.blockPos)
                registerKineticBridge()
            } else {
                logger.info("[StressP2P@{}]   -> skipped (GRID_BOOT during init)", blockEntity.blockPos)
            }
            return
        }

        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        if (isActive) {
            logger.info("[StressP2P@{}]   -> isActive=true, calling registerKineticBridge", blockEntity.blockPos)
            registerKineticBridge()
        } else {
            logger.info("[StressP2P@{}]   -> isActive=false, calling unregisterKineticBridge", blockEntity.blockPos)
            unregisterKineticBridge()
        }
    }

    override fun onTunnelNetworkChange() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        logger.info("[StressP2P@{}] >>> onTunnelNetworkChange (isOutput={}, initialLoadComplete={}, registeredInputPos={})",
            blockEntity.blockPos, isOutput, initialLoadComplete, registeredInputPos)

        if (!initialLoadComplete) {
            logger.info("[StressP2P@{}]   -> during init load, silent registerKineticBridge only", blockEntity.blockPos)
            registerKineticBridge()
            return
        }

        logger.info("[StressP2P@{}]   -> runtime change, calling reRegisterBridge + outputs", blockEntity.blockPos)
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
            logger.info("[StressP2P@{}]   registerKineticBridge: inputPos=null, scheduling retry", blockEntity.blockPos)
            scheduleRetry()
            return
        }

        val kineticPos = blockEntity.blockPos.relative(this.side)

        val be = level.getBlockEntity(kineticPos)
        if (be !is KineticBlockEntity) {
            logger.info("[StressP2P@{}]   registerKineticBridge: no KineticBE at {}", blockEntity.blockPos, kineticPos)
            return
        }

        // Skip if already registered with same positions
        if (registeredInputPos == inputPos && registeredKineticPos == kineticPos) {
            logger.info("[StressP2P@{}]   registerKineticBridge: already registered (input={}, kinetic={})",
                blockEntity.blockPos, inputPos, kineticPos)
            return
        }

        KineticBridgeRegistry.register(inputPos, kineticPos)
        registeredInputPos = inputPos
        registeredKineticPos = kineticPos
        retryAttemptsRemaining = 0

        logger.info("[StressP2P@{}]   registerKineticBridge: REGISTERED inputTunnel={}, kinetic={}",
            blockEntity.blockPos, inputPos, kineticPos)
    }

    private fun unregisterKineticBridge() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        val inputPos = registeredInputPos ?: return
        val kineticPos = registeredKineticPos ?: return

        logger.info("[StressP2P@{}]   unregisterKineticBridge: input={}, kinetic={}",
            blockEntity.blockPos, inputPos, kineticPos)

        val be = level.getBlockEntity(kineticPos) as? KineticBlockEntity
        if (be != null && be.getTheoreticalSpeed() != 0f) {
            logger.info("[StressP2P@{}]   Phase 1: handleRemoved (speed={})", blockEntity.blockPos, be.getTheoreticalSpeed())
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

        logger.info("[StressP2P@{}]   reRegisterBridge: old=({},{}), new=({},{})",
            blockEntity.blockPos, oldInputPos, oldKineticPos, newInputPos, newKineticPos)

        if (oldInputPos == newInputPos && oldKineticPos == newKineticPos) {
            logger.info("[StressP2P@{}]   reRegisterBridge: nothing changed, skip", blockEntity.blockPos)
            return
        }

        if (oldInputPos != null && oldKineticPos != null) {
            KineticBridgeRegistry.unregister(oldInputPos, oldKineticPos)
            logger.info("[StressP2P@{}]   reRegisterBridge: triggerRepropagation on old", blockEntity.blockPos)
            KineticBridgeRegistry.triggerRepropagation(oldInputPos, level)
        }

        if (newInputPos != null) {
            val be = level.getBlockEntity(newKineticPos)
            if (be is KineticBlockEntity) {
                KineticBridgeRegistry.register(newInputPos, newKineticPos)
                registeredInputPos = newInputPos
                registeredKineticPos = newKineticPos
                logger.info("[StressP2P@{}]   reRegisterBridge: triggerRepropagation on new", blockEntity.blockPos)
                KineticBridgeRegistry.triggerRepropagation(newInputPos, level)
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
        logger.info("[StressP2P@{}] scheduleRetry ({} attempts left)",
            blockEntity.blockPos, attemptsLeft)
        server.tell(TickTask(server.tickCount + 1) {
            if (registeredInputPos != null) {
                logger.info("[StressP2P@{}] retry: already registered, skip", blockEntity.blockPos)
                return@TickTask
            }
            if (blockEntity.isRemoved) return@TickTask
            val inputPos = findInputTunnelPos()
            if (inputPos != null) {
                logger.info("[StressP2P@{}] retry: grid ready, calling registerKineticBridge", blockEntity.blockPos)
                registerKineticBridge()
            } else if (attemptsLeft > 0) {
                scheduleRetry()
            } else {
                logger.info("[StressP2P@{}] retry: giving up — grid never came online", blockEntity.blockPos)
            }
        })
    }

}
