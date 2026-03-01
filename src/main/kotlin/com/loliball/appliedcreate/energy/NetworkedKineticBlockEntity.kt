package com.loliball.appliedcreate.energy

import appeng.api.networking.GridHelper
import appeng.api.networking.IGridNode
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IManagedGridNode
import appeng.api.networking.IInWorldGridNodeHost
import appeng.api.orientation.BlockOrientation
import appeng.api.util.AECableType
import appeng.me.InWorldGridNode
import appeng.me.helpers.BlockEntityNodeListener
import appeng.me.helpers.IGridConnectedBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import java.util.EnumSet

abstract class NetworkedKineticBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : KineticBlockEntity(type, pos, state), IGridConnectedBlockEntity, IInWorldGridNodeHost {

    @Suppress("UNCHECKED_CAST")
    private val _mainNode: IManagedGridNode = GridHelper.createManagedNode(
        this, BlockEntityNodeListener.INSTANCE as IGridNodeListener<IGridConnectedBlockEntity>
    )
        .setInWorldNode(true)
        .setTagName("proxy")

    override fun getMainNode(): IManagedGridNode = _mainNode
    override fun getGridConnectableSides(orientation: BlockOrientation): Set<Direction> =
        EnumSet.allOf(Direction::class.java)
    override fun saveChanges() = setChanged()
    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {}
    override fun getCableConnectionType(dir: Direction): AECableType = AECableType.COVERED
    override fun getActionableNode(): IGridNode? = mainNode.node
    override fun getGridNode(dir: Direction): IGridNode? {
        val node = mainNode.node ?: return null
        if (node is InWorldGridNode && node.isExposedOnSide(dir)) {
            return node
        }
        return null
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun initialize() {
        super.initialize()
        if (level != null && !level!!.isClientSide) {
            mainNode.create(level, worldPosition)
        }
        exposeSides()
    }

    override fun remove() {
        mainNode.destroy()
        super.remove()
    }

    override fun onChunkUnloaded() {
        super.onChunkUnloaded()
        mainNode.destroy()
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        mainNode.saveToNBT(compound)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        mainNode.loadFromNBT(compound)
        exposeSides()
    }

    protected fun exposeSides() {
        mainNode.setExposedOnSides(getGridConnectableSides(BlockOrientation.get(blockState)))
    }
}
