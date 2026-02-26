package com.loliball.appliedcreate.kinetic

import appeng.api.networking.GridHelper
import appeng.api.networking.IGridNode
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IManagedGridNode
import appeng.api.storage.MEStorage
import appeng.api.util.AECableType
import appeng.api.orientation.BlockOrientation
import appeng.me.helpers.BlockEntityNodeListener
import appeng.me.helpers.IGridConnectedBlockEntity
import appeng.me.helpers.MachineSource
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.p2p.StressP2PNetwork
import com.loliball.appliedcreate.p2p.StressP2PTunnelPart
import com.loliball.appliedcreate.storage.StressKey
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import java.util.EnumSet

/**
 * Stress Provider Block Entity — dual-purpose block:
 *
 * 1. P2P Companion (output side): Uses Create's custom connection system to bridge
 *    kinetic networks through AE2 P2P tunnels. Receives rotation from the paired
 *    StressAcceptorBlockEntity on the input side through the virtual connection.
 *
 * 2. ME Grid Device: Connects to the AE2 ME network and extracts stress from ME
 *    stress storage cells. Provides the extracted stress as rotation to the local
 *    kinetic network.
 *
 * Technical pattern: KineticBlockEntity + manual IManagedGridNode composition.
 * No stress capacity — the provider is a bridge, not a generator (for P2P mode).
 */
class StressProviderBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : KineticBlockEntity(type, pos, state), IGridConnectedBlockEntity {

    // ── AE2 Grid Node (manual composition) ──

    @Suppress("UNCHECKED_CAST")
    private val mainNode: IManagedGridNode = GridHelper.createManagedNode(
        this,
        BlockEntityNodeListener.INSTANCE as IGridNodeListener<IGridConnectedBlockEntity>
    )
        .setVisualRepresentation(AppliedCreate.STRESS_PROVIDER_ITEM.get())
        .setInWorldNode(true)
        .setTagName("proxy")
        .setIdlePowerUsage(1.0)
        .setExposedOnSides(EnumSet.allOf(Direction::class.java))

    private val actionSource = MachineSource(this)

    // ── IGridConnectedBlockEntity implementation ──

    override fun getMainNode(): IManagedGridNode = mainNode

    override fun getGridConnectableSides(orientation: BlockOrientation): Set<Direction> {
        return EnumSet.allOf(Direction::class.java)
    }

    override fun saveChanges() {
        setChanged()
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        // Could send visual update to client if needed
    }

    override fun getCableConnectionType(dir: Direction): AECableType {
        return AECableType.SMART
    }

    override fun getActionableNode(): IGridNode? {
        return mainNode.node
    }

    override fun setOwner(owner: Player) {
        mainNode.setOwningPlayer(owner)
    }

    override fun getGridNode(dir: Direction): IGridNode? {
        val node = mainNode.node ?: return null
        if (node is appeng.me.InWorldGridNode && node.isExposedOnSide(dir)) {
            return node
        }
        return null
    }

    // ── P2P Companion (Create Custom Connection) ──

    /** The input tunnel's position, used as the network key in StressP2PNetwork */
    private var registeredInputPos: BlockPos? = null

    fun getRegisteredInputPos(): BlockPos? = registeredInputPos

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

    // ── Lifecycle ──

    override fun initialize() {
        super.initialize()
        if (level != null && !level!!.isClientSide) {
            registerWithNetwork()
            mainNode.create(level, worldPosition)
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
        mainNode.destroy()
        super.remove()
    }

    override fun onChunkUnloaded() {
        super.onChunkUnloaded()
        mainNode.destroy()
    }

    // ── NBT Persistence ──

    override fun write(compound: CompoundTag, registries: net.minecraft.core.HolderLookup.Provider, clientPacket: Boolean) {
        super.write(compound, registries, clientPacket)
        mainNode.saveToNBT(compound)
    }

    override fun read(compound: CompoundTag, registries: net.minecraft.core.HolderLookup.Provider, clientPacket: Boolean) {
        super.read(compound, registries, clientPacket)
        mainNode.loadFromNBT(compound)
    }

    // ── P2P Network Registration ──

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
     * For the provider (output side), the adjacent tunnel is typically an output tunnel,
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
