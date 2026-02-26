package com.loliball.appliedcreate.p2p

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import java.util.concurrent.ConcurrentHashMap

/**
 * Global registry tracking paired Stress P2P companion blocks.
 *
 * Maps the P2P input tunnel's BlockPos to the set of companion KineticBlockEntity instances
 * (both StressAcceptor on the input side and StressProvider on the output sides).
 *
 * This enables Create's custom kinetic connection system (isCustomConnection /
 * addPropagationLocations / propagateRotationTo) to bridge kinetic networks
 * through the AE2 ME network.
 *
 * Following CreateEnderTransmission's pattern: all companions keyed by the same
 * input tunnel position form a virtual kinetic network.
 */
object StressP2PNetwork {
    // inputTunnelPos -> set of companion BlockPos
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

    /**
     * Get all partner positions for a given input tunnel key, excluding self.
     */
    fun getPartners(inputTunnelPos: BlockPos, selfPos: BlockPos): List<BlockPos> {
        return connections[inputTunnelPos]?.filter { it != selfPos } ?: emptyList()
    }

    /**
     * Get all partner KineticBlockEntity instances for a given input tunnel key, excluding self.
     * Resolves BlockPos to actual block entities from the level.
     */
    fun getPartnerEntities(inputTunnelPos: BlockPos, selfPos: BlockPos, level: Level): List<KineticBlockEntity> {
        return getPartners(inputTunnelPos, selfPos).mapNotNull { pos ->
            level.getBlockEntity(pos) as? KineticBlockEntity
        }
    }

    fun clear() {
        connections.clear()
    }
}
