package com.loliball.appliedcreate.kinetic

import appeng.api.networking.GridHelper
import appeng.api.networking.IGridNode
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IInWorldGridNodeHost
import appeng.api.networking.IManagedGridNode
import appeng.api.networking.security.IActionHost
import appeng.api.orientation.BlockOrientation
import appeng.api.stacks.AEKeyType
import appeng.api.storage.MEStorage
import appeng.api.util.AECableType
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
import kotlin.math.abs

/**
 * Stress Acceptor Block Entity — dual-purpose block:
 *
 * 1. P2P Companion (input side): Uses Create's custom connection system to bridge
 *    kinetic networks through AE2 P2P tunnels. All companions sharing the same P2P
 *    input key form a virtual kinetic network.
 *
 * 2. ME Grid Device: Connects to the AE2 ME network and inserts stress (from the
 *    kinetic network) into ME stress storage cells. Consumes rotation and converts
 *    it to stored stress units.
 *
 * Technical pattern: KineticBlockEntity + manual IManagedGridNode composition
 * (can't extend both KineticBlockEntity and AENetworkedBlockEntity).
 *
 * Stress impact is 0 — this block passively bridges rotation without adding load
 * (for the P2P companion role). For the ME storage role, stress is inserted based
 * on the kinetic speed.
 */
class StressAcceptorBlockEntity(
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
        .setVisualRepresentation(AppliedCreate.STRESS_ACCEPTOR_ITEM.get())
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
        if (other is StressAcceptorBlockEntity || other is StressProviderBlockEntity) {
            val myKey = registeredInputPos ?: return false
            val otherKey = when (other) {
                is StressAcceptorBlockEntity -> other.getRegisteredInputPos()
                is StressProviderBlockEntity -> other.getRegisteredInputPos()
                else -> null
            } ?: return false
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
        if (target is StressAcceptorBlockEntity || target is StressProviderBlockEntity) {
            val myKey = registeredInputPos ?: return 0f
            val otherKey = when (target) {
                is StressAcceptorBlockEntity -> target.getRegisteredInputPos()
                is StressProviderBlockEntity -> target.getRegisteredInputPos()
                else -> null
            } ?: return 0f
            if (myKey == otherKey) return 1f
        }
        return 0f
    }

    // ── Lifecycle ──

    override fun initialize() {
        super.initialize()
        if (level != null && !level!!.isClientSide) {
            registerWithNetwork()
            // Create grid node after level is available
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
                updateRegistration()
            }
        }

        // Insert stress into ME network based on kinetic speed
        insertStressIntoNetwork()
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
    fun updateRegistration() {
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
     */
    private fun findInputTunnelPos(): BlockPos? {
        val facing = blockState.getValue(DirectionalKineticBlock.FACING)
        val tunnelPos = worldPosition.relative(facing)
        val level = this.level ?: return null
        val be = level.getBlockEntity(tunnelPos) ?: return null
        val cableBus = be as? appeng.api.parts.IPartHost ?: return null
        val part = cableBus.getPart(facing.opposite) as? StressP2PTunnelPart ?: return null

        return if (!part.isOutput) {
            // We're adjacent to the input tunnel directly
            part.blockEntity.blockPos
        } else {
            // We're adjacent to an output tunnel; get the input's position
            part.input?.blockEntity?.blockPos
        }
    }

    // ── ME Stress Storage ──

    /**
     * Insert stress into ME network based on current kinetic speed.
     * Runs every tick when the block has rotation speed.
     */
    private fun insertStressIntoNetwork() {
        val currentSpeed = abs(speed)
        if (currentSpeed < 0.01f) return

        val grid = mainNode.grid ?: return
        val storage: MEStorage = grid.storageService?.inventory ?: return

        // Convert speed to stress units: speed * 256 per tick
        val amount = (currentSpeed * 256).toLong()
        if (amount <= 0) return

        storage.insert(
            StressKey.INSTANCE,
            amount,
            appeng.api.config.Actionable.MODULATE,
            actionSource
        )
    }
}
