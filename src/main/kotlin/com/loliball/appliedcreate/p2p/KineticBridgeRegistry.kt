package com.loliball.appliedcreate.p2p

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
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
 */
object KineticBridgeRegistry {

    private val LOGGER = LoggerFactory.getLogger("AppliedCreate/KineticBridge")

    /** localKineticPos → Set<remoteKineticPos> — virtual edges visible to RotationPropagator */
    private val edges = ConcurrentHashMap<BlockPos, MutableSet<BlockPos>>()

    /** inputTunnelPos → Set<adjacentKineticPos> — all kinetic endpoints keyed by P2P input */
    private val tunnelEndpoints = ConcurrentHashMap<BlockPos, MutableSet<BlockPos>>()

    /**
     * Register a kinetic block position as an endpoint for a P2P tunnel group.
     * This creates virtual edges between this position and all other endpoints
     * in the same tunnel group (keyed by inputTunnelPos).
     *
     * @param inputTunnelPos The input tunnel's BlockPos (used as the group key)
     * @param kineticPos The position of the kinetic block adjacent to the tunnel
     */
    fun register(inputTunnelPos: BlockPos, kineticPos: BlockPos) {
        val endpoints = tunnelEndpoints.getOrPut(inputTunnelPos) { ConcurrentHashMap.newKeySet() }

        // Get existing endpoints BEFORE adding self
        val existingEndpoints = endpoints.filter { it != kineticPos }

        // Add self to endpoint set
        endpoints.add(kineticPos)

        // Create bidirectional edges with all existing endpoints
        for (existingPos in existingEndpoints) {
            addEdge(kineticPos, existingPos)
            addEdge(existingPos, kineticPos)
        }

        LOGGER.debug("[KineticBridge] Register: tunnel={}, kinetic={}, edges to {} partners",
            inputTunnelPos, kineticPos, existingEndpoints.size)
    }

    /**
     * Unregister a kinetic block position from its P2P tunnel group.
     * Removes all virtual edges involving this position.
     *
     * @param inputTunnelPos The input tunnel's BlockPos (group key)
     * @param kineticPos The position to unregister
     */
    fun unregister(inputTunnelPos: BlockPos, kineticPos: BlockPos) {
        val endpoints = tunnelEndpoints[inputTunnelPos] ?: return

        // Remove all edges involving this position
        val partners = edges.remove(kineticPos) ?: emptySet()
        for (partnerPos in partners) {
            edges[partnerPos]?.remove(kineticPos)
        }

        // Remove from endpoint set
        endpoints.remove(kineticPos)
        if (endpoints.isEmpty()) {
            tunnelEndpoints.remove(inputTunnelPos)
        }

        LOGGER.debug("[KineticBridge] Unregister: tunnel={}, kinetic={}, removed {} edges",
            inputTunnelPos, kineticPos, partners.size)
    }

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
     * Useful for triggering re-propagation when the P2P config changes.
     */
    fun getEndpoints(inputTunnelPos: BlockPos): Set<BlockPos> {
        return tunnelEndpoints[inputTunnelPos] ?: emptySet()
    }

    /**
     * Trigger kinetic re-propagation on all endpoints in a tunnel group.
     * Call this when P2P tunnel configuration changes (link/unlink, memory card, etc.)
     */
    fun triggerRepropagation(inputTunnelPos: BlockPos, level: Level) {
        val endpoints = tunnelEndpoints[inputTunnelPos] ?: return
        for (pos in endpoints.toList()) { // toList to avoid concurrent modification
            val be = level.getBlockEntity(pos) as? KineticBlockEntity ?: continue
            LOGGER.debug("[KineticBridge] Triggering re-propagation at {}", pos)
            be.detachKinetics()
            be.removeSource()
            be.attachKinetics()
        }
    }

    /**
     * Clear all registry data. Called on server shutdown / dimension unload.
     */
    fun clear() {
        edges.clear()
        tunnelEndpoints.clear()
    }

    private fun addEdge(from: BlockPos, to: BlockPos) {
        edges.getOrPut(from) { ConcurrentHashMap.newKeySet() }.add(to)
    }
}
