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
 * Supports cross-dimension bridging: each endpoint stores its [ServerLevel] so the
 * mixin can resolve block entities from the correct dimension even when Create's
 * RotationPropagator BFS uses the source block's level.
 *
 * Data model:
 * - [edges]: localKineticPos → Set<remoteKineticPos> (bidirectional bridge links)
 * - [tunnelEndpoints]: inputTunnelPos → Set<adjacentKineticPos> (for bulk updates when P2P config changes)
 * - [positionLevels]: kineticPos → ServerLevel (dimension mapping for cross-dim lookups)
 */
object KineticBridgeRegistry {

    private val LOGGER = LoggerFactory.getLogger("AppliedCreate/KineticBridge")

    /** localKineticPos → Set<remoteKineticPos> — virtual edges visible to RotationPropagator */
    private val edges = ConcurrentHashMap<BlockPos, MutableSet<BlockPos>>()

    /** inputTunnelPos → Set<adjacentKineticPos> — all kinetic endpoints keyed by P2P input */
    private val tunnelEndpoints = ConcurrentHashMap<BlockPos, MutableSet<BlockPos>>()

    /** kineticPos → ServerLevel — maps each registered kinetic endpoint to its dimension */
    private val positionLevels = ConcurrentHashMap<BlockPos, ServerLevel>()

    /**
     * Register a kinetic block position as an endpoint for a P2P tunnel group.
     * This creates virtual edges between this position and all other endpoints
     * in the same tunnel group (keyed by inputTunnelPos).
     *
     * @param inputTunnelPos The input tunnel's BlockPos (used as the group key)
     * @param kineticPos The position of the kinetic block adjacent to the tunnel
     * @param level The ServerLevel this kinetic block is in (for cross-dimension support)
     */
    fun register(inputTunnelPos: BlockPos, kineticPos: BlockPos, level: ServerLevel) {
        val endpoints = tunnelEndpoints.getOrPut(inputTunnelPos) { ConcurrentHashMap.newKeySet() }

        // Get existing endpoints BEFORE adding self
        val existingEndpoints = endpoints.filter { it != kineticPos }

        // Add self to endpoint set and store level mapping
        endpoints.add(kineticPos)
        positionLevels[kineticPos] = level

        // Create bidirectional edges with all existing endpoints
        for (existingPos in existingEndpoints) {
            addEdge(kineticPos, existingPos)
            addEdge(existingPos, kineticPos)
        }

        LOGGER.debug("[KineticBridge] Register: tunnel={}, kinetic={}, dim={}, edges to {} partners",
            inputTunnelPos, kineticPos, level.dimension().location(), existingEndpoints.size)
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

        // Remove from endpoint set and level mapping
        endpoints.remove(kineticPos)
        positionLevels.remove(kineticPos)
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
     * Get the ServerLevel for a registered kinetic endpoint position.
     * Returns null if the position is not registered in the bridge registry.
     *
     * Called by RotationPropagatorMixin to resolve cross-dimension block entities.
     */
    fun getLevelForPos(kineticPos: BlockPos): ServerLevel? {
        return positionLevels[kineticPos]
    }

    /**
     * Resolve the correct KineticBlockEntity for a bridged position.
     * If the position is a registered bridge endpoint, looks it up from the correct level.
     * Otherwise returns null (caller should fall back to normal resolution).
     *
     * This is the primary API for cross-dimension support in the mixin.
     */
    fun resolveBlockEntity(pos: BlockPos): KineticBlockEntity? {
        val level = positionLevels[pos] ?: return null
        return level.getBlockEntity(pos) as? KineticBlockEntity
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
     *
     * Uses a two-phase approach to avoid ordering issues:
     * Phase 1: handleRemoved on ALL endpoints (tear down existing networks)
     * Phase 2: handleAdded on all endpoints (rebuild via BFS)
     *
     * This prevents the scenario where processing endpoints one-by-one causes
     * a source-side rebuild that is immediately torn down when the output-side
     * endpoint is processed next.
     *
     * Supports cross-dimension endpoints: each endpoint is resolved from its own
     * registered ServerLevel rather than a single shared level.
     */
    fun triggerRepropagation(inputTunnelPos: BlockPos) {
        val endpoints = tunnelEndpoints[inputTunnelPos] ?: return
        val snapshot = endpoints.toList() // snapshot to avoid concurrent modification

        // Collect all kinetic block entities at endpoints, resolving from correct levels
        val kineticEntries = snapshot.mapNotNull { pos ->
            val level = positionLevels[pos] ?: return@mapNotNull null
            val be = level.getBlockEntity(pos) as? KineticBlockEntity ?: return@mapNotNull null
            Triple(pos, be, level)
        }

        if (kineticEntries.isEmpty()) return

        // Phase 1: Remove all endpoints from their kinetic networks
        for ((pos, be, level) in kineticEntries) {
            LOGGER.debug("[KineticBridge] Phase 1 — removing kinetic network at {} in {}",
                pos, level.dimension().location())
            RotationPropagator.handleRemoved(level, pos, be)
        }

        // Phase 2: Re-add all endpoints.
        // Prioritize source blocks (generators) first, then blocks that had a source.
        // The BFS in propagateNewSource will traverse virtual edges to reach all
        // connected endpoints automatically.
        val sources = kineticEntries.filter { (_, be, _) -> be.isSource }
        val withSource = kineticEntries.filter { (_, be, _) -> !be.isSource && be.hasSource() }
        val remaining = kineticEntries.filter { (_, be, _) -> !be.isSource && !be.hasSource() }

        for ((pos, be, level) in sources + withSource + remaining) {
            LOGGER.debug("[KineticBridge] Phase 2 — re-adding kinetic at {} in {} (isSource={}, hasSource={})",
                pos, level.dimension().location(), be.isSource, be.hasSource())
            RotationPropagator.handleAdded(level, pos, be)
        }
    }

    /**
     * Clear all registry data. Called on server shutdown / dimension unload.
     */
    fun clear() {
        edges.clear()
        tunnelEndpoints.clear()
        positionLevels.clear()
    }

    private fun addEdge(from: BlockPos, to: BlockPos) {
        edges.getOrPut(from) { ConcurrentHashMap.newKeySet() }.add(to)
    }
}
