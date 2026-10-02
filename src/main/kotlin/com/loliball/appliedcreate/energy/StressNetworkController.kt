package com.loliball.appliedcreate.energy

import com.simibubi.create.content.kinetics.KineticNetwork
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import java.util.Collections
import java.util.IdentityHashMap
import java.util.WeakHashMap
import kotlin.math.abs

/**
 * One allocator per physical kinetic network. No inventory writes happen in stress getters.
 * Exports are prepaid before publishing capacity; imports settle once at the end of the
 * server tick, after topology/loads have settled. Imported capacity never includes ME sources.
 */
object StressNetworkController {
    private class State {
        var updating = false
        var nativeCapacity = 0.0
        var ordinaryLoad = 0.0
    }

    private data class Snapshot(
        val level: ServerLevel,
        val ports: List<MEGearboxBlockEntity>,
        val generators: List<KineticBlockEntity>,
        val capacity: Double,
        val load: Double
    )

    private val states = WeakHashMap<KineticNetwork, State>()
    private val gearboxes = Collections.newSetFromMap(IdentityHashMap<MEGearboxBlockEntity, Boolean>())
    private val collectedTicks = IdentityHashMap<MinecraftServer, Int>()

    fun register(gearbox: MEGearboxBlockEntity) {
        if (gearbox.level is ServerLevel) gearboxes.add(gearbox)
    }

    fun unregister(gearbox: MEGearboxBlockEntity) {
        gearboxes.remove(gearbox)
        if (gearbox.level is ServerLevel && gearbox.hasNetwork()) update(gearbox.getOrCreateNetwork())
    }

    fun clear() {
        gearboxes.clear()
        states.clear()
        collectedTicks.clear()
    }

    private fun belongsTo(member: KineticBlockEntity, network: KineticNetwork): Boolean {
        val level = member.level as? ServerLevel ?: return false
        return !member.isRemoved && level.hasChunkAt(member.blockPos) &&
            level.getBlockEntity(member.blockPos) === member && member.hasNetwork() &&
            member.getOrCreateNetwork() === network
    }

    private fun snapshot(network: KineticNetwork): Snapshot? {
        val members = network.members.keys.toList().filter { belongsTo(it, network) }
        val level = members.firstOrNull()?.level as? ServerLevel ?: return null
        val ports = members.filterIsInstance<MEGearboxBlockEntity>().sortedBy { it.blockPos.asLong() }
        val generators = network.sources.keys.toList().filter {
            it !is MEGearboxBlockEntity && belongsTo(it, network)
        }
        // Unloaded aggregate capacity has no live producer and is not harvestable.
        val capacity = generators.sumOf {
            finitePositive((network.sources[it] ?: 0f).toDouble() * abs(it.generatedSpeed.toDouble()))
        }
        val load = members.filter { it !is MEGearboxBlockEntity }.sumOf {
            finitePositive((network.members[it] ?: 0f).toDouble() * abs(it.theoreticalSpeed.toDouble()))
        }
        return Snapshot(level, ports, generators, capacity, load)
    }

    private fun finitePositive(value: Double) = if (value.isFinite() && value > 0) value else 0.0

    /** Called by the mixin before Create publishes any changed stress/capacity totals. */
    @JvmStatic
    fun update(network: KineticNetwork): Boolean {
        val previous = states[network]
        if (previous?.updating == true) return true
        val snapshot = snapshot(network) ?: run {
            if (previous == null) return false
            previous.nativeCapacity = 0.0
            previous.ordinaryLoad = 0.0
            (network as StressNetworkAccess).`appliedcreate$publishStress`(0f, 0f)
            return true
        }
        if (snapshot.ports.isEmpty() && previous == null) return false
        val state = previous ?: State().also { states[network] = it }
        state.updating = true
        try {
            val tick = snapshot.level.server.tickCount.toLong()
            state.nativeCapacity = snapshot.capacity
            state.ordinaryLoad = snapshot.load
            val exports = snapshot.ports.filter { it.mode == MEGearboxBlockEntity.Mode.EXPORT && it.canTransfer() }
            val imports = snapshot.ports.filter { it.mode == MEGearboxBlockEntity.Mode.IMPORT && it.canTransfer() }
            val required = StressDistribution.deficit(snapshot.capacity, snapshot.load)
            val exactDeficit = (snapshot.load - snapshot.capacity).coerceAtLeast(0.0)
            val available = StressDistribution.surplus(snapshot.capacity, snapshot.load)

            // Shared ME inventories are accessed sequentially. Actual withdrawals reserve the
            // balance, so two ports cannot both promise the same last units in a cell.
            val sent = StressDistribution.allocate(required, exports.map { it.transferLimit() }.toLongArray()) {
                    index, assigned, requested ->
                (exports[index].reserveStress(tick, assigned + requested) - assigned).coerceIn(0, requested)
            }
            val sentTotal = sent.sum()
            val funded = sentTotal >= required && sentTotal.toDouble() >= exactDeficit
            if (funded) {
                exports.forEachIndexed { index, port ->
                    port.commitStress(tick, sent[index])
                }
            }

            // Planning adds only a load. Credit is issued exclusively by endTick().
            val received = StressDistribution.allocate(available, imports.map { it.transferLimit() }.toLongArray()) {
                    index, assigned, requested ->
                (imports[index].simulateImport(assigned + requested) - assigned).coerceIn(0, requested)
            }
            val exportAmounts = exports.zip(sent.toList()).toMap()
            // Storage is integral. Rounding payment up must not expose extra usable capacity.
            val physicalSent = sent.map { it.toDouble() }.toDoubleArray()
            var rounding = (sentTotal.toDouble() - exactDeficit).coerceAtLeast(0.0)
            for (index in physicalSent.indices.reversed()) {
                val remove = minOf(rounding, physicalSent[index])
                physicalSent[index] -= remove
                rounding -= remove
            }
            val exportCapacities = exports.zip(physicalSent.toList()).toMap()
            val importAmounts = imports.zip(received.toList()).toMap()
            snapshot.ports.forEach { port ->
                port.setAllocation(if (funded) exportAmounts[port] ?: 0 else 0, importAmounts[port] ?: 0,
                    if (funded) exportCapacities[port] ?: 0.0 else 0.0)
                network.members[port] = port.calculateStressApplied()
                if (network.sources.containsKey(port)) network.sources[port] = port.calculateAddedStressCapacity()
            }

            // Publish both values together. Never call updateCapacityFor/updateStressFor here:
            // each of those publishes an intermediate total and re-enters the allocator.
            (network as StressNetworkAccess).`appliedcreate$publishStress`(
                (if (funded) maxOf(snapshot.capacity, snapshot.load) else snapshot.capacity).toFloat(),
                (snapshot.load + received.sum()).toFloat()
            )
            return true
        } finally {
            state.updating = false
        }
    }

    private fun networks(server: MinecraftServer): List<KineticNetwork> {
        val result = Collections.newSetFromMap(IdentityHashMap<KineticNetwork, Boolean>())
        gearboxes.toList().forEach { port ->
            if (port.level?.server === server && !port.isRemoved && port.hasNetwork()) {
                result.add(port.getOrCreateNetwork())
            }
        }
        return result.toList()
    }

    /** Refresh paid capacity before block entities do work, including when inventory ran out. */
    fun beginTick(server: MinecraftServer) {
        networks(server).forEach { update(it) }
    }

    /** A source can back at most one collection in this server tick, even across a reentrant split. */
    fun endTick(server: MinecraftServer) {
        if (collectedTicks.put(server, server.tickCount) == server.tickCount) return
        val usedGenerators = Collections.newSetFromMap(IdentityHashMap<KineticBlockEntity, Boolean>())
        networks(server).forEach { network ->
            update(network)
            val snapshot = snapshot(network) ?: return@forEach
            val imports = snapshot.ports.filter {
                it.mode == MEGearboxBlockEntity.Mode.IMPORT && it.canTransfer() && !it.isOverStressed
            }
            val capacity = snapshot.generators.filter { usedGenerators.add(it) }.sumOf {
                finitePositive((network.sources[it] ?: 0f).toDouble() * abs(it.generatedSpeed.toDouble()))
            }
            val available = StressDistribution.surplus(capacity, snapshot.load)
            // Actual credit may never exceed the load already published for that port.
            val received = StressDistribution.allocate(available, imports.map {
                minOf(it.transferLimit(), it.allocatedStress())
            }.toLongArray()) {
                    index, _, requested -> imports[index].collectStress(requested)
            }
            imports.forEachIndexed { index, port ->
                port.setAllocation(0, received[index])
                network.members[port] = port.calculateStressApplied()
                port.finishCollection(received[index])
            }
            // Rejected insertion is not consumption. Keep the published load equal to what
            // was actually stored, rather than the earlier simulation's estimate.
            val supplied = snapshot.ports.sumOf {
                if (it.mode == MEGearboxBlockEntity.Mode.EXPORT) it.allocatedCapacity() else 0.0
            }
            (network as StressNetworkAccess).`appliedcreate$publishStress`(
                (snapshot.capacity + supplied).toFloat(), (snapshot.load + received.sum()).toFloat())
        }
        gearboxes.toList().filter { it.level?.server === server && !it.isRemoved }.forEach {
            it.returnUnusedStress()
        }
    }

    /** Never restore a previous tick's paid ME capacity as an unloaded, free generator. */
    @JvmStatic
    fun writePersistentNetwork(member: KineticBlockEntity, tag: CompoundTag) {
        if (member.level !is ServerLevel || !member.hasNetwork() || !tag.contains("Network")) return
        val state = states[member.getOrCreateNetwork()] ?: return
        val saved = tag.getCompound("Network")
        saved.putFloat("Capacity", state.nativeCapacity.toFloat())
        saved.putFloat("Stress", state.ordinaryLoad.toFloat())
        if (member is MEGearboxBlockEntity) {
            saved.remove("AddedCapacity")
            saved.remove("AddedStress")
        }
    }
}
