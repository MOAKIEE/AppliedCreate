package com.loliball.appliedcreate.kinetic

import com.loliball.appliedcreate.p2p.StressP2PNetwork
import com.loliball.appliedcreate.p2p.StressP2PTunnelPart
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.apache.logging.log4j.LogManager

/**
 * Stress P2P Companion Block Entity — unified kinetic bridge for AE2 P2P tunnels.
 *
 * Replaces the former StressAcceptorBlockEntity and StressProviderBlockEntity.
 * This block is placed adjacent to a Stress P2P Tunnel (input OR output side) and
 * uses Create's custom connection system to bridge kinetic networks through AE2 P2P tunnels.
 *
 * All companions sharing the same P2P input tunnel position form a virtual kinetic network.
 * Rotation propagates at 1:1 ratio between all companions keyed to the same input tunnel.
 *
 * Architecture: Pure KineticBlockEntity — NO AE2 grid node, NO ME stress storage conversion.
 * Following CreateEnderTransmission's EnergyTransmitterBlockEntity pattern.
 *
 * Stress impact is 0 — this block passively bridges rotation without adding load.
 */
class StressP2PCompanionBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : KineticBlockEntity(type, pos, state) {

    companion object {
        private val LOGGER = LogManager.getLogger("appliedcreate/StressP2PCompanion")
    }

    // ── P2P Companion (Create Custom Connection) ──

    /** The input tunnel's position, used as the network key in StressP2PNetwork */
    private var registeredInputPos: BlockPos? = null

    /** Tick counter for early retry of P2P registration (tunnel may not be loaded yet) */
    private var initRetryTicks = 0

    /** Guard flag to prevent kinetic cascades during block removal/unload */
    private var isRemoving = false

    /** Guard flag to prevent recursive reloadKinetics cascades */
    private var isReloading = false

    fun getRegisteredInputPos(): BlockPos? = registeredInputPos

    override fun isCustomConnection(other: KineticBlockEntity, state: BlockState, otherState: BlockState): Boolean {
        if (other is StressP2PCompanionBlockEntity) {
            val myKey = registeredInputPos ?: return false
            val otherKey = other.getRegisteredInputPos() ?: return false
            return myKey == otherKey
        }
        return false
    }

    override fun addPropagationLocations(
        block: IRotate,
        state: BlockState,
        neighbours: MutableList<BlockPos>
    ): MutableList<BlockPos> {
        val key = registeredInputPos
        if (key != null) {
            val partners = StressP2PNetwork.getPartners(key, worldPosition)
            for (partnerPos in partners) {
                if (!neighbours.contains(partnerPos)) {
                    neighbours.add(partnerPos)
                }
            }
            if (partners.isNotEmpty()) {
                LOGGER.info("[Companion@{}] addPropagationLocations: key={}, partners={}",
                    worldPosition, key, partners)
            }
        }
        return super.addPropagationLocations(block, state, neighbours)
    }

    override fun propagateRotationTo(
        target: KineticBlockEntity,
        stateFrom: BlockState,
        stateTo: BlockState,
        diff: BlockPos,
        connectedViaAxes: Boolean,
        connectedViaCogs: Boolean
    ): Float {
        if (target is StressP2PCompanionBlockEntity) {
            val myKey = registeredInputPos ?: return 0f
            val otherKey = target.getRegisteredInputPos() ?: return 0f
            if (myKey == otherKey) {
                LOGGER.info("[Companion@{}] propagateRotationTo {} -> speed modifier 1.0",
                    worldPosition, target.blockPos)
                return 1f
            }
        }
        return 0f
    }

    // ── Lifecycle ──

    override fun initialize() {
        super.initialize()
        if (level != null && !level!!.isClientSide) {
            registerWithNetwork()
            LOGGER.info("[Companion@{}] initialize: registeredInputPos={}",
                worldPosition, registeredInputPos)
            if (registeredInputPos != null) {
                // Re-trigger propagation so partners discover this new connection
                attachKinetics()
            } else {
                // Tunnel may not be loaded/linked yet — schedule early retries
                initRetryTicks = 10
            }
        }
    }

    override fun tick() {
        super.tick()
        if (level == null || level!!.isClientSide) return

        // Early retry: check registration in the first few ticks after init
        if (initRetryTicks > 0) {
            initRetryTicks--
            val currentInputPos = findInputTunnelPos()
            if (currentInputPos != null && currentInputPos != registeredInputPos) {
                LOGGER.info("[Companion@{}] early retry found tunnel: {}",
                    worldPosition, currentInputPos)
                reloadKinetics()
                initRetryTicks = 0
            }
        }

        // Periodically check for tunnel changes (e.g., memory card relink)
        if (level!!.gameTime % 20 == 0L) {
            val currentInputPos = findInputTunnelPos()
            if (currentInputPos != registeredInputPos) {
                LOGGER.info("[Companion@{}] periodic check: tunnel changed {} -> {}",
                    worldPosition, registeredInputPos, currentInputPos)
                reloadKinetics()
            }
        }
    }

    override fun remove() {
        LOGGER.info("[Companion@{}] remove", worldPosition)
        isRemoving = true
        unregisterFromNetwork()
        super.remove()
    }

    override fun onChunkUnloaded() {
        isRemoving = true
        unregisterFromNetwork()
        super.onChunkUnloaded()
    }

    // ── P2P Network Registration ──

    /**
     * Called when the P2P tunnel notifies us of a network change.
     * Detaches and re-attaches kinetics to pick up new connections.
     */
    fun reloadKinetics() {
        if (level == null || level!!.isClientSide) return
        if (isRemoving || isReloading) return
        isReloading = true
        try {
            LOGGER.info("[Companion@{}] reloadKinetics: old={}",
                worldPosition, registeredInputPos)
            unregisterFromNetwork()
            if (hasNetwork()) getOrCreateNetwork().remove(this)
            detachKinetics()
            removeSource()
            registerWithNetwork()
            LOGGER.info("[Companion@{}] reloadKinetics: new={}, partners={}",
                worldPosition, registeredInputPos,
                registeredInputPos?.let { StressP2PNetwork.getPartners(it, worldPosition) })
            attachKinetics()
        } finally {
            isReloading = false
        }
    }

    private fun registerWithNetwork() {
        val inputPos = findInputTunnelPos()
        registeredInputPos = inputPos
        if (inputPos != null) {
            // Get existing partners BEFORE registering self
            val existingPartners = StressP2PNetwork.getPartners(inputPos, worldPosition)
            StressP2PNetwork.register(inputPos, worldPosition)
            // Notify existing partners to rediscover connections (so they see us immediately)
            val lvl = level ?: return
            for (partnerPos in existingPartners) {
                val be = lvl.getBlockEntity(partnerPos) as? StressP2PCompanionBlockEntity ?: continue
                LOGGER.info("[Companion@{}] notifying existing partner @{} to reload kinetics",
                    worldPosition, partnerPos)
                be.reloadKinetics()
            }
        }
    }

    private fun unregisterFromNetwork() {
        val key = registeredInputPos
        if (key != null) {
            StressP2PNetwork.unregister(key, worldPosition)
            registeredInputPos = null
        }
    }

    /**
     * Find the P2P input tunnel's BlockPos by looking at the adjacent cable bus.
     * FACING points toward the cable bus. The tunnel part sits on the opposite side.
     *
     * For input tunnels: the adjacent tunnel IS the input — return its pos directly.
     * For output tunnels: follow the P2P link back to the input tunnel's pos.
     */
    private fun findInputTunnelPos(): BlockPos? {
        val facing = blockState.getValue(DirectionalKineticBlock.FACING)
        val tunnelPos = worldPosition.relative(facing)
        val level = this.level ?: return null
        val be = level.getBlockEntity(tunnelPos) ?: return null
        val cableBus = be as? appeng.api.parts.IPartHost ?: return null
        val part = cableBus.getPart(facing.opposite) as? StressP2PTunnelPart ?: return null

        return if (!part.isOutput) {
            // Adjacent to the input tunnel directly
            part.blockEntity.blockPos
        } else {
            // Adjacent to an output tunnel; follow P2P link to get input's position
            // NOTE: This requires the ME grid to be online (part.input uses P2PService)
            part.input?.blockEntity?.blockPos
        }
    }
}
