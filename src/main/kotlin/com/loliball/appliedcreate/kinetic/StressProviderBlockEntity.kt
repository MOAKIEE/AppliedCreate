package com.loliball.appliedcreate.kinetic

import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.p2p.StressP2PNetwork
import com.loliball.appliedcreate.p2p.StressP2PTunnelPart
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * Stress Provider Block Entity — the output companion for Stress P2P.
 *
 * Extends KineticBlockEntity and uses Create's custom connection system
 * (isCustomConnection / addPropagationLocations / propagateRotationTo)
 * to bridge kinetic networks through the AE2 ME P2P tunnel.
 *
 * The provider outputs rotation to the output kinetic network via its shaft.
 * Rotation propagates from the paired StressAcceptorBlockEntity on the input side
 * through the virtual connection.
 *
 * No stress capacity is needed — the provider is a bridge, not a generator.
 */
class StressProviderBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : KineticBlockEntity(type, pos, state) {
    constructor(pos: BlockPos, state: BlockState) : this(AppliedCreate.STRESS_PROVIDER_BE.get(), pos, state)

    /** The input tunnel's position, used as the network key in StressP2PNetwork */
    private var registeredInputPos: BlockPos? = null

    fun getRegisteredInputPos(): BlockPos? = registeredInputPos

    // ── Create Custom Connection Overrides ──

    override fun isCustomConnection(other: KineticBlockEntity, state: BlockState, otherState: BlockState): Boolean {
        if (other is StressAcceptorBlockEntity) {
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
            for (partnerPos in StressP2PNetwork.getPartners(key, worldPosition)) {
                if (!neighbours.contains(partnerPos)) {
                    neighbours.add(partnerPos)
                }
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
        if (target is StressAcceptorBlockEntity) {
            val myKey = registeredInputPos ?: return 0f
            val otherKey = target.getRegisteredInputPos() ?: return 0f
            if (myKey == otherKey) return 1f
        }
        return 0f
    }

    // ── Network Registration ──

    override fun initialize() {
        super.initialize()
        if (level != null && !level!!.isClientSide) {
            registerWithNetwork()
        }
    }

    override fun tick() {
        super.tick()
        if (level == null || level!!.isClientSide) return

        // Periodically check for tunnel changes (e.g., memory card relink)
        if (level!!.gameTime % 20 == 0L) {
            val currentInputPos = findInputTunnelPos()
            if (currentInputPos != registeredInputPos) {
                reloadKinetics()
            }
        }
    }

    override fun remove() {
        unregisterFromNetwork()
        super.remove()
    }

    /**
     * Called when the P2P tunnel notifies us of a network change.
     * Detaches and re-attaches kinetics to pick up new connections.
     */
    fun reloadKinetics() {
        if (level == null || level!!.isClientSide) return
        unregisterFromNetwork()
        if (hasNetwork()) getOrCreateNetwork().remove(this)
        detachKinetics()
        removeSource()
        registerWithNetwork()
        attachKinetics()
    }

    private fun registerWithNetwork() {
        val inputPos = findInputTunnelPos()
        registeredInputPos = inputPos
        if (inputPos != null) {
            StressP2PNetwork.register(inputPos, worldPosition)
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
     * For the provider (output side), the adjacent tunnel is an output tunnel,
     * so we follow the P2P link back to the input.
     */
    private fun findInputTunnelPos(): BlockPos? {
        val facing = blockState.getValue(DirectionalKineticBlock.FACING)
        val tunnelPos = worldPosition.relative(facing)
        val level = this.level ?: return null
        val be = level.getBlockEntity(tunnelPos) ?: return null
        val cableBus = be as? appeng.api.parts.IPartHost ?: return null
        val part = cableBus.getPart(facing.opposite) as? StressP2PTunnelPart ?: return null

        return if (part.isOutput) {
            // We're adjacent to an output tunnel; get the input's position
            part.input?.blockEntity?.blockPos
        } else {
            // We're adjacent to the input tunnel directly (unusual for provider)
            part.blockEntity.blockPos
        }
    }
}
