package com.loliball.appliedcreate.p2p

import appeng.api.parts.IPartItem
import appeng.api.networking.IGridNodeListener
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
import net.minecraft.world.level.BlockGetter

/**
 * Stress P2P Tunnel Part — transmits Create rotational stress through AE2 P2P tunnels.
 *
 * Uses a **debounced reconcile pattern**: all AE2 lifecycle events ([onMainNodeStateChanged],
 * [onTunnelNetworkChange]) schedule a deferred [reconcileNow] on the next server tick.
 * This avoids cascading partial-state issues when multiple events fire within a single tick.
 *
 * [reconcileNow] computes desired state, diffs against registered state, and calls
 * [KineticBridgeRegistry.registerEndpoint]/[KineticBridgeRegistry.unregisterEndpoint] atomically.
 * Exception: [removeFromWorld] unregisters immediately (no next tick to wait for).
 */
class StressP2PTunnelPart(partItem: IPartItem<*>) : P2PTunnelPart<StressP2PTunnelPart>(partItem) {

    private var registeredInputPos: BlockPos? = null
    private var registeredKineticPos: BlockPos? = null
    private var initialLoadComplete = false
    private var reconcileScheduled = false
    private var retryCount = 0

    companion object {
        /** Max retries when kinetic block not yet placed adjacent to an active tunnel. */
        private const val MAX_KINETIC_RETRIES = 20
        /** Ticks between retries (ramps: attempt * RETRY_INTERVAL_TICKS). */
        private const val RETRY_INTERVAL_TICKS = 5

        private val MODELS = P2PModels(
            ResourceLocation(AppliedCreate.MOD_ID, "part/p2p/p2p_tunnel_stress")
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

    // ── Lifecycle events ──

    override fun addToWorld() {
        super.addToWorld()
        initialLoadComplete = false
        reconcileScheduled = false
    }

    override fun removeFromWorld() {
        // Immediate unregister — part is being destroyed, no debounce.
        unregisterIfRegistered()
        super.removeFromWorld()
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        super.onMainNodeStateChanged(reason)
        val level = blockEntity.level ?: return
        if (level.isClientSide) return
        if (KineticBridgeRegistry.serverStopping) return

        if (!initialLoadComplete) {
            if (isActive) {
                initialLoadComplete = true
                // Schedule reconcile for next tick — lets all tunnels in the group finish their initial boot
                scheduleReconcile()
            }
            return
        }

        scheduleReconcile()
    }

    override fun onTunnelNetworkChange() {
        val level = blockEntity.level ?: return
        if (level.isClientSide) return
        if (KineticBridgeRegistry.serverStopping) return

        if (!initialLoadComplete) return

        scheduleReconcile()

        // Also schedule reconcile on all outputs — their inputPos may have changed
        for (output in getOutputs()) {
            output.scheduleReconcile()
        }
    }

    override fun onNeighborChanged(level: BlockGetter, pos: BlockPos, neighbor: BlockPos) {
        super.onNeighborChanged(level, pos, neighbor)
        if (level is ServerLevel && initialLoadComplete && !KineticBridgeRegistry.serverStopping) {
            val kineticPos = blockEntity.blockPos.relative(this.side)
            if (neighbor == kineticPos) {
                scheduleReconcile()
            }
        }
    }

    // ── Debounced reconcile ──

    /**
     * Schedule a reconcile for the next server tick. Multiple calls within the same tick
     * are coalesced into a single reconcile (debounce).
     */
    internal fun scheduleReconcile() {
        if (reconcileScheduled) return
        if (KineticBridgeRegistry.serverStopping) return
        val level = blockEntity.level as? ServerLevel ?: return
        val server = level.server

        reconcileScheduled = true
        retryCount = 0
        val scheduledAtTick = server.tickCount

        scheduleForNextTick(server, scheduledAtTick) {
            reconcileNow()
        }
    }

    private fun reconcileNow() {
        reconcileScheduled = false

        if (blockEntity.isRemoved) {
            unregisterIfRegistered()
            return
        }

        val level = blockEntity.level as? ServerLevel ?: return
        if (KineticBridgeRegistry.serverStopping) return

        val desiredActive = isActive
        val desiredInputPos = if (desiredActive) findInputTunnelPos() else null
        val desiredKineticPos: BlockPos?

        if (desiredActive && desiredInputPos != null) {
            val kPos = blockEntity.blockPos.relative(side)
            val be = level.getBlockEntity(kPos)
            desiredKineticPos = if (be is KineticBlockEntity) kPos else null
        } else {
            desiredKineticPos = null
        }

        val currentInputPos = registeredInputPos
        val currentKineticPos = registeredKineticPos

        if (currentInputPos != null && currentKineticPos != null) {
            if (currentInputPos != desiredInputPos || currentKineticPos != desiredKineticPos) {
                KineticBridgeRegistry.unregisterEndpoint(level, currentInputPos, currentKineticPos)
                registeredInputPos = null
                registeredKineticPos = null
            } else {
                return
            }
        }

        if (desiredInputPos != null && desiredKineticPos != null) {
            KineticBridgeRegistry.registerEndpoint(level, desiredInputPos, desiredKineticPos)
            registeredInputPos = desiredInputPos
            registeredKineticPos = desiredKineticPos
            retryCount = 0
        } else if (desiredActive && desiredInputPos != null && desiredKineticPos == null) {
            // Tunnel is active and attuned, but kinetic block not yet placed adjacent.
            // Retry with linear backoff until the block appears or we exhaust retries.
            if (retryCount < MAX_KINETIC_RETRIES) {
                retryCount++
                val delayTicks = retryCount * RETRY_INTERVAL_TICKS
                val server = level.server
                val baseTick = server.tickCount
                scheduleForNextTick(server, baseTick + delayTicks - 1) {
                    reconcileNow()
                }
            }
        } else {
            retryCount = 0
        }
    }

    // ── Helpers ──

    private fun unregisterIfRegistered() {
        val inputPos = registeredInputPos ?: return
        val kineticPos = registeredKineticPos ?: return
        val level = blockEntity.level as? ServerLevel

        if (level != null && !KineticBridgeRegistry.serverStopping) {
            KineticBridgeRegistry.unregisterEndpoint(level, inputPos, kineticPos)
        } else {
            // KineticBridgeRegistry.clear() handles full cleanup on server stop
        }

        registeredInputPos = null
        registeredKineticPos = null
    }

    private fun findInputTunnelPos(): BlockPos? {
        if (!this.isOutput) {
            return blockEntity.blockPos
        }
        return this.input?.blockEntity?.blockPos
    }

    /**
     * Schedule a callback for the next server tick.
     * If the TickTask fires on the same tick it was scheduled (can happen with tick task ordering),
     * reschedule until we're actually on the next tick.
     */
    private fun scheduleForNextTick(server: net.minecraft.server.MinecraftServer, scheduledAtTick: Int, callback: () -> Unit) {
        server.tell(TickTask(scheduledAtTick + 1) {
            if (server.tickCount <= scheduledAtTick) {
                scheduleForNextTick(server, scheduledAtTick, callback)
                return@TickTask
            }
            callback()
        })
    }
}
