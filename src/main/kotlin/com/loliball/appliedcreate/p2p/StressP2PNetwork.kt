package com.loliball.appliedcreate.p2p

import net.minecraft.core.BlockPos
import java.util.concurrent.ConcurrentHashMap

/**
 * Global registry tracking paired Stress P2P companion blocks.
 *
 * Maps the P2P input tunnel's BlockPos to the set of companion block positions
 * (both StressAcceptor on the input side and StressProvider on the output side).
 *
 * This enables Create's custom kinetic connection system (isCustomConnection /
 * addPropagationLocations / propagateRotationTo) to bridge kinetic networks
 * through the AE2 ME network.
 */
object StressP2PNetwork {
    // inputTunnelPos -> set of companion BlockPos (acceptors + providers)
    private val connections = ConcurrentHashMap<BlockPos, MutableSet<BlockPos>>()

    fun register(inputTunnelPos: BlockPos, companionPos: BlockPos) {
        connections.getOrPut(inputTunnelPos) { ConcurrentHashMap.newKeySet() }.add(companionPos)
    }

    fun unregister(inputTunnelPos: BlockPos, companionPos: BlockPos) {
        connections[inputTunnelPos]?.remove(companionPos)
        if (connections[inputTunnelPos]?.isEmpty() == true) {
            connections.remove(inputTunnelPos)
        }
    }

    fun getPartners(inputTunnelPos: BlockPos, selfPos: BlockPos): List<BlockPos> {
        return connections[inputTunnelPos]?.filter { it != selfPos } ?: emptyList()
    }

    fun clear() {
        connections.clear()
    }
}
