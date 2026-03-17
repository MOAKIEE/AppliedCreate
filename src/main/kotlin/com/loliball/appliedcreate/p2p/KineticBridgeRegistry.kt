package com.loliball.appliedcreate.p2p

import com.simibubi.create.content.kinetics.RotationPropagator
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Global registry for virtual kinetic edges bridged through AE2 P2P tunnels.
 *
 * Replaces the former StressP2PNetwork + companion block system.
 * Instead of placing physical companion blocks, we register virtual edges between
 * kinetic blocks adjacent to paired P2P tunnels. The RotationPropagatorMixin
 * uses this registry to make Create's propagation BFS traverse these virtual edges.
 *
 * Data model:
 * - [edges]: localKineticPos → Set<remoteKineticPos> (bidirectional bridge links)
 * - [tunnelEndpoints]: inputTunnelPos → Set<adjacentKineticPos> (for bulk updates when P2P config changes)
 *
 * IMPORTANT: All mutation methods that interact with RotationPropagator (registerEndpoint / unregisterEndpoint)
 * must be called from the server thread only.
 */
object KineticBridgeRegistry {

    private val LOGGER = LoggerFactory.getLogger("AppliedCreate/KineticBridge")

    /** Set to true when server is stopping — guards against RotationPropagator calls during shutdown */
    @Volatile
    var serverStopping = false
        private set

    /** localKineticPos → Set<remoteKineticPos> — virtual edges visible to RotationPropagator */
    private val edges = ConcurrentHashMap<BlockPos, MutableSet<BlockPos>>()

    /** inputTunnelPos → Set<adjacentKineticPos> — all kinetic endpoints keyed by P2P input */
    private val tunnelEndpoints = ConcurrentHashMap<BlockPos, MutableSet<BlockPos>>()

    // ── Query methods (called by RotationPropagatorMixin during BFS) ──

    /**
     * Get all remote positions that a given kinetic block is bridged to.
     * Called by RotationPropagatorMixin to add virtual neighbors.
     */
    fun getRemotePositions(kineticPos: BlockPos): Set<BlockPos>? {
        return edges[kineticPos]
    }

    /**
     * Check if two kinetic block positions are connected by a virtual bridge edge.
     * Called by RotationPropagatorMixin to validate connections.
     */
    fun areBridged(posA: BlockPos, posB: BlockPos): Boolean {
        return edges[posA]?.contains(posB) == true
    }

    /**
     * Get all kinetic endpoint positions for a given P2P tunnel group.
     */
    fun getEndpoints(inputTunnelPos: BlockPos): Set<BlockPos> {
        return tunnelEndpoints[inputTunnelPos] ?: emptySet()
    }

    // ── Mutation methods (server thread only, interact with RotationPropagator) ──

    /**
     * Register a kinetic block as an endpoint for a P2P tunnel group.
     * Creates virtual edges to all existing endpoints in the group,
     * then triggers Create re-propagation so the kinetic network sees the new edges.
     *
     * @param level The server level
     * @param inputTunnelPos The input tunnel's BlockPos (group key)
     * @param kineticPos The kinetic block position adjacent to the tunnel
     */
    fun registerEndpoint(level: ServerLevel, inputTunnelPos: BlockPos, kineticPos: BlockPos) {
        val endpoints = tunnelEndpoints.getOrPut(inputTunnelPos) { ConcurrentHashMap.newKeySet() }

        // Get existing endpoints BEFORE adding self
        val existingPartners = endpoints.filter { it != kineticPos }

        // Add self to endpoint set
        if (!endpoints.add(kineticPos)) {
            // Already registered — but edges may have been removed during a previous unregister cycle.
            // Rebuild edges if needed.
            val currentEdges = edges[kineticPos]
            val allPartnersPresent = existingPartners.all { currentEdges?.contains(it) == true }
            if (allPartnersPresent && existingPartners.isNotEmpty()) {
                LOGGER.info("[DIAG] registerEndpoint: tunnel={}, kinetic={}, already fully registered with {} partners, skipping",
                    inputTunnelPos, kineticPos, existingPartners.size)
                return
            }
        }

        // Create bidirectional edges with all existing endpoints
        for (partnerPos in existingPartners) {
            addEdge(kineticPos, partnerPos)
            addEdge(partnerPos, kineticPos)
        }

        LOGGER.info("[DIAG] registerEndpoint: tunnel={}, kinetic={}, edges to {} partners: {}",
            inputTunnelPos, kineticPos, existingPartners.size, existingPartners)

        // Trigger Create re-propagation AFTER edges exist
        if (existingPartners.isNotEmpty() && !serverStopping) {
            triggerRepropagation(level, inputTunnelPos)
        }
    }

    /**
     * Unregister a kinetic block from its P2P tunnel group.
     * Tears down the kinetic network WHILE edges still exist (so Create's BFS can traverse them),
     * then removes edges, then re-propagates remaining endpoints.
     *
     * @param level The server level
     * @param inputTunnelPos The input tunnel's BlockPos (group key)
     * @param kineticPos The kinetic block position to unregister
     */
    fun unregisterEndpoint(level: ServerLevel, inputTunnelPos: BlockPos, kineticPos: BlockPos) {
        val endpoints = tunnelEndpoints[inputTunnelPos] ?: return
        if (!endpoints.contains(kineticPos)) return

        val partners = endpoints.filter { it != kineticPos }

        LOGGER.info("[DIAG] unregisterEndpoint: tunnel={}, kinetic={}, {} partners: {}",
            inputTunnelPos, kineticPos, partners.size, partners)

        // Phase 1: Tear down WHILE edges still exist
        // This lets Create's handleRemoved BFS traverse the bridge to zero the far side
        if (partners.isNotEmpty() && !serverStopping) {
            val be = level.getBlockEntity(kineticPos) as? KineticBlockEntity
            if (be != null && be.getTheoreticalSpeed() != 0f) {
                LOGGER.info("[DIAG] unregisterEndpoint: handleRemoved on kinetic={} (speed={})",
                    kineticPos, be.getTheoreticalSpeed())
                RotationPropagator.handleRemoved(level, kineticPos, be)
                if (be.hasSource()) {
                    be.removeSource()
                    be.sendData()
                }
            }
            // Also tear down partner side to ensure clean state
            for (partnerPos in partners) {
                val partnerBE = level.getBlockEntity(partnerPos) as? KineticBlockEntity ?: continue
                if (partnerBE.getTheoreticalSpeed() != 0f) {
                    LOGGER.info("[DIAG] unregisterEndpoint: handleRemoved on partner={} (speed={})",
                        partnerPos, partnerBE.getTheoreticalSpeed())
                    RotationPropagator.handleRemoved(level, partnerPos, partnerBE)
                    if (partnerBE.hasSource()) {
                        partnerBE.removeSource()
                        partnerBE.sendData()
                    }
                }
            }
        }

        // Phase 2: Remove edges
        val removedEdges = edges.remove(kineticPos) ?: emptySet()
        for (partnerPos in removedEdges) {
            edges[partnerPos]?.remove(kineticPos)
        }

        // Remove from endpoint set
        endpoints.remove(kineticPos)
        if (endpoints.isEmpty()) {
            tunnelEndpoints.remove(inputTunnelPos)
        }

        LOGGER.info("[DIAG] unregisterEndpoint: removed {} edges for kinetic={}", removedEdges.size, kineticPos)

        // Phase 3: Re-propagate remaining endpoints + the removed endpoint's local network
        if (!serverStopping) {
            // Re-add the removed kinetic block so it can find its local physical neighbors
            val be = level.getBlockEntity(kineticPos) as? KineticBlockEntity
            if (be != null) {
                RotationPropagator.handleAdded(level, kineticPos, be)
            }
            // Re-propagate remaining partners
            for (partnerPos in partners) {
                val partnerBE = level.getBlockEntity(partnerPos) as? KineticBlockEntity ?: continue
                RotationPropagator.handleAdded(level, partnerPos, partnerBE)
            }
        }
    }

    /**
     * Trigger kinetic re-propagation on all endpoints in a tunnel group.
     * Uses a two-phase approach:
     * Phase 1: handleRemoved on ALL endpoints (tear down existing networks)
     * Phase 2: handleAdded starting with source blocks (rebuild via BFS)
     */
    fun triggerRepropagation(level: Level, inputTunnelPos: BlockPos) {
        val endpoints = tunnelEndpoints[inputTunnelPos] ?: return
        val snapshot = endpoints.toList()
        LOGGER.info("[DIAG] triggerRepropagation: tunnel={}, {} endpoints: {}", inputTunnelPos, snapshot.size, snapshot)

        val kineticEntries = snapshot.mapNotNull { pos ->
            val be = level.getBlockEntity(pos) as? KineticBlockEntity ?: return@mapNotNull null
            pos to be
        }

        if (kineticEntries.isEmpty()) return

        // Phase 1: Remove all endpoints from their kinetic networks
        for ((pos, be) in kineticEntries) {
            LOGGER.info("[KineticBridge] Phase 1 — removing kinetic network at {}", pos)
            RotationPropagator.handleRemoved(level, pos, be)
        }

        // Phase 2: Re-add. Prioritize source blocks first.
        val sources = kineticEntries.filter { (_, be) -> be.isSource }
        val nonSources = kineticEntries.filter { (_, be) -> !be.isSource }

        for ((pos, be) in sources + nonSources) {
            RotationPropagator.handleAdded(level, pos, be)
        }
    }

    // ── Lifecycle ──

    fun clear() {
        serverStopping = true
        edges.clear()
        tunnelEndpoints.clear()
    }

    fun resetShutdownFlag() {
        serverStopping = false
    }

    // ── Internal ──

    private fun addEdge(from: BlockPos, to: BlockPos) {
        edges.getOrPut(from) { ConcurrentHashMap.newKeySet() }.add(to)
    }
}
