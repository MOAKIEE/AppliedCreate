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
import net.minecraft.core.Direction
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.TickTask
import net.minecraft.server.level.ServerLevel

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

    /** Whether the initial kinetic reconciliation has been performed for this tunnel.
     *  This is a one-shot guard to prevent repeated teardown/rebuild cycles
     *  if AE2 toggles active/inactive multiple times during startup. */
    private var didInitialReconcile = false

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
        didInitialReconcile = false

        // Do NOT register virtual edges here — Create's first-tick attachKinetics()
        // would traverse them and corrupt stress accounting. Registration is deferred
        // until the AE2 grid comes online (isActive=true in onMainNodeStateChanged).
    }

    override fun removeFromWorld() {
        unregisterKineticBridge()
        super.removeFromWorld()
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        super.onMainNodeStateChanged(reason)
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        if (!initialLoadComplete) {
            if (isActive) {
                // Grid is now fully online for the first time.
                // CRITICAL: Do NOT register virtual edges immediately!
                // During world load, this fires BEFORE Create's block entities have
                // their first tick (attachKinetics). If we register edges now, those
                // blocks' first-tick BFS will traverse our virtual edges, merge into
                // a giant network with maxStress=0 (source not accounted for), and
                // trigger stress overload.
                //
                // Additionally, TickTask(tickCount+1) runs immediately in the same
                // tick when MinecraftServer.haveTime() is true (common during load).
                //
                // Fix: defer BOTH edge registration AND propagation to an actual
                // future tick using the scheduleForNextTick bounce pattern. By then,
                // all Create blocks have completed their first-tick attachKinetics()
                // and formed stable local networks.
                initialLoadComplete = true
                scheduleDeferredInitialRegistration()
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

        if (!initialLoadComplete) {
            // During boot, do nothing — edges will be registered when isActive becomes true
            return
        }

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
            scheduleRetry()
            return
        }

        val kineticPos = blockEntity.blockPos.relative(this.side)

        val be = level.getBlockEntity(kineticPos)
        if (be !is KineticBlockEntity) {
            return
        }

        // Skip if already registered with same positions
        if (registeredInputPos == inputPos && registeredKineticPos == kineticPos) {
            return
        }

        KineticBridgeRegistry.register(inputPos, kineticPos)
        registeredInputPos = inputPos
        registeredKineticPos = kineticPos
        retryAttemptsRemaining = 0
    }

    private fun unregisterKineticBridge() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return

        val inputPos = registeredInputPos ?: return
        val kineticPos = registeredKineticPos ?: return

        val be = level.getBlockEntity(kineticPos) as? KineticBlockEntity
        if (be != null && be.getTheoreticalSpeed() != 0f) {
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

        if (oldInputPos == newInputPos && oldKineticPos == newKineticPos) {
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
     * Schedule deferred initial registration + propagation for after world load.
     * Called from onMainNodeStateChanged when isActive first becomes true.
     *
     * Uses the scheduleForNextTick bounce pattern to guarantee execution in an
     * actual future server tick, not the current tick via haveTime(). This ensures
     * all Create block entities have completed their first-tick attachKinetics()
     * and formed stable local kinetic networks before we inject virtual edges.
     */
    private fun scheduleDeferredInitialRegistration() {
        val level = blockEntity.level as? ServerLevel ?: return
        val server = level.server
        val scheduledAtTick = server.tickCount
        val isInput = !this.isOutput
        scheduleForNextTick(server, scheduledAtTick) {
            if (blockEntity.isRemoved) return@scheduleForNextTick
            registerKineticBridge()
            // Only the input tunnel schedules propagation after registration
            if (isInput) {
                scheduleKineticPropagation()
            }
        }
    }

    /**
     * Schedule kinetic propagation for the next server tick.
     * Called after first-time bridge registration (when AE2 grid comes online).
     *
     * On initial world load, output-side kinetic blocks have NBT-restored state
     * (speed, source, network membership). If we naively call handleAdded on all
     * endpoints, Create's propagateNewSource BFS finds conflicting networks and
     * triggers stress overload.
     *
     * Fix: tear down output-side kinetic state first (handleRemoved + removeSource),
     * then propagate from input side only. Create's BFS flows through virtual edges
     * to the now-clean output blocks and integrates them correctly.
     *
     * Uses the scheduleForNextTick bounce pattern to guarantee actual tick advancement.
     */
    private fun scheduleKineticPropagation() {
        val level = blockEntity.level as? ServerLevel ?: return
        val server = level.server
        val inputPos = registeredInputPos ?: return
        val isInitialLoad = !didInitialReconcile
        val scheduledAtTick = server.tickCount
        scheduleForNextTick(server, scheduledAtTick) {
            if (blockEntity.isRemoved) return@scheduleForNextTick
            if (registeredInputPos != inputPos) return@scheduleForNextTick // changed since scheduled
            val endpoints = KineticBridgeRegistry.getEndpoints(inputPos)

            if (isInitialLoad) {
                didInitialReconcile = true
                // Identify input-side vs output-side kinetic endpoints.
                // Input-side endpoint is adjacent to the input tunnel (Manhattan distance 1).
                var inputEndpointPos: BlockPos? = null
                var inputEndpointBE: KineticBlockEntity? = null
                val outputEndpointEntries = mutableListOf<Pair<BlockPos, KineticBlockEntity>>()

                for (endpointPos in endpoints) {
                    val be = level.getBlockEntity(endpointPos) as? KineticBlockEntity ?: continue
                    val dx = if (endpointPos.x > inputPos.x) endpointPos.x - inputPos.x else inputPos.x - endpointPos.x
                    val dy = if (endpointPos.y > inputPos.y) endpointPos.y - inputPos.y else inputPos.y - endpointPos.y
                    val dz = if (endpointPos.z > inputPos.z) endpointPos.z - inputPos.z else inputPos.z - endpointPos.z
                    val dist = dx + dy + dz
                    if (dist == 1 && inputEndpointPos == null) {
                        inputEndpointPos = endpointPos
                        inputEndpointBE = be
                    } else {
                        outputEndpointEntries.add(endpointPos to be)
                    }
                }

                // Phase 1: Recursively tear down ALL output-side kinetic blocks.
                // The previous 1-hop approach only cleared immediate neighbors, leaving
                // deeper blocks (e.g. 25 MechanicalCrafters in a grid) with NBT-restored
                // kinetic state. When our Phase 2 BFS reached them, they still belonged
                // to a conflicting network → stress duplication → overload.
                //
                // Additionally, blocks that haven't had their first tick() yet still have
                // updateSpeed=true. If we don't clear this flag, their subsequent tick()
                // calls attachKinetics() → handleAdded() AGAIN after our Phase 2 already
                // set them up, causing a second propagateNewSource pass → double stress.
                // This race condition is why the bug was PROBABILISTIC.
                //
                // Fix: BFS from each output endpoint through physical adjacency, calling
                // detachKinetics() and setting updateSpeed=false on every reachable block.
                val endpointPositions = endpoints.toSet()
                val tornDown = mutableSetOf<BlockPos>()
                // Don't tear down the input-side endpoint
                if (inputEndpointPos != null) tornDown.add(inputEndpointPos)
                
                for ((startPos, _) in outputEndpointEntries) {
                    // BFS from this output endpoint through physical neighbors
                    val frontier = ArrayDeque<BlockPos>()
                    frontier.add(startPos)
                    while (frontier.isNotEmpty()) {
                        val pos = frontier.removeFirst()
                        if (!tornDown.add(pos)) continue
                        val be = level.getBlockEntity(pos) as? KineticBlockEntity ?: continue
                        be.detachKinetics()  // handleRemoved + network removal, zeros speed
                        be.removeSource()    // clear source reference
                        be.updateSpeed = false // CRITICAL: prevent first-tick re-attachKinetics race
                        // Enqueue physical neighbors (6 directions only, no virtual edges)
                        for (dir in Direction.entries) {
                            val neighborPos = pos.relative(dir)
                            if (neighborPos in tornDown) continue
                            // Only follow into kinetic blocks, don't cross virtual edges
                            if (neighborPos in endpointPositions && neighborPos != pos) {
                                // This is another endpoint — tear it down but don't BFS further
                                // (it will be handled by its own BFS start or is the input endpoint)
                                continue
                            }
                            val neighborBE = level.getBlockEntity(neighborPos) as? KineticBlockEntity
                            if (neighborBE != null) {
                                frontier.add(neighborPos)
                            }
                        }
                    }
                }

                // Phase 2: Propagate from input-side endpoint only.
                // Create's BFS will traverse virtual edges to reach the now-clean output blocks.
                if (inputEndpointBE != null && inputEndpointPos != null) {
                    RotationPropagator.handleAdded(level, inputEndpointPos!!, inputEndpointBE!!)

                    // Phase 3: Zero stale unloaded counters on all affected KineticNetworks.
                    // ROOT CAUSE FIX: KineticNetwork.remove() does NOT adjust unloadedStress/
                    // unloadedCapacity. After Phase 1 tore down output blocks (calling
                    // detachKinetics → handleRemoved → setNetwork(null) → network.remove()),
                    // the old network's unloadedStress retained phantom stress from the removed
                    // blocks. Phase 2's handleAdded() → propagateNewSource() → add() puts blocks
                    // into networks but doesn't touch unloaded counters either.
                    // Result: calculateStress() = unloadedStress + sum(member.stress) includes
                    // phantom unloadedStress → stress doubled/tripled → overload.
                    //
                    // Fix: After Phase 2 completes and all blocks are in their correct networks,
                    // zero unloaded counters via initFromTE(0,0,0) and recalculate purely from
                    // actual loaded members. This is safe because by this deferred tick, ALL
                    // blocks have been loaded and initialized — there are no truly unloaded members.
                    val processedNetworkIds = mutableSetOf<Long>()
                    for (epPos in endpoints) {
                        val epBE = level.getBlockEntity(epPos) as? KineticBlockEntity ?: continue
                        if (epBE.hasNetwork()) {
                            val network = epBE.getOrCreateNetwork()
                            if (processedNetworkIds.add(network.id)) {
                                network.initFromTE(0f, 0f, 0)
                                network.updateNetwork()
                            }
                        }
                    }
                }
            } else {
                // Runtime re-propagation (not initial load) — propagate on all endpoints
                for (endpointPos in endpoints) {
                    val be = level.getBlockEntity(endpointPos) as? KineticBlockEntity ?: continue
                    RotationPropagator.handleAdded(level, endpointPos, be)
                }
            }
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
        val scheduledAtTick = server.tickCount
        scheduleForNextTick(server, scheduledAtTick) {
            doRetryAttempt(attemptsLeft)
        }
    }

    /**
     * Enqueue a TickTask that keeps bouncing until the server tick actually advances
     * past [scheduledAtTick], preventing callbacks from executing in the same tick
     * when the server has spare time (haveTime() == true).
     *
     * MinecraftServer.shouldRun() returns true for TickTask(tickCount+1) when
     * haveTime() is true, which is common during world load. This bounce pattern
     * guarantees the callback runs in a genuinely different server tick.
     */
    private fun scheduleForNextTick(server: net.minecraft.server.MinecraftServer, scheduledAtTick: Int, callback: () -> Unit) {
        server.tell(TickTask(scheduledAtTick + 1) {
            if (server.tickCount <= scheduledAtTick) {
                // Still the same tick — re-enqueue
                scheduleForNextTick(server, scheduledAtTick, callback)
                return@TickTask
            }
            callback()
        })
    }

    private fun doRetryAttempt(attemptsLeft: Int) {
        if (registeredInputPos != null) {
            return
        }
        if (blockEntity.isRemoved) return
        val inputPos = findInputTunnelPos()
        if (inputPos != null) {
            registerKineticBridge()
        } else if (attemptsLeft > 0) {
            scheduleRetry()
        }
    }

}
