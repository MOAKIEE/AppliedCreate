package com.loliball.appliedcreate.spatial

import appeng.api.config.Actionable
import appeng.api.config.PowerMultiplier
import appeng.api.config.YesNo
import appeng.api.implementations.items.ISpatialStorageCell
import appeng.api.networking.GridHelper
import appeng.api.networking.IGridNode
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IManagedGridNode
import appeng.api.orientation.BlockOrientation
import appeng.api.util.AECableType
import appeng.me.helpers.BlockEntityNodeListener
import appeng.me.helpers.IGridConnectedBlockEntity
import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.neoforged.neoforge.items.ItemStackHandler
import java.util.EnumSet
import java.util.LinkedList

class SpatialAssemblerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(type, pos, state), MenuProvider, IGridConnectedBlockEntity {

    companion object {
        /** Base power cost per block in the spatial region (same as AE2's spatial IO). */
        const val POWER_PER_BLOCK = 1500.0

        /** Max BFS expansion to avoid infinite loops. */
        const val MAX_GLUE_SEARCH = 16384
    }

    // ── AE2 Grid Node ──
    @Suppress("UNCHECKED_CAST")
    private val mainNode: IManagedGridNode = GridHelper.createManagedNode(
        this, BlockEntityNodeListener.INSTANCE as IGridNodeListener<IGridConnectedBlockEntity>
    )
        .setVisualRepresentation(AppliedCreate.SPATIAL_ASSEMBLER_BLOCK.asItem())
        .setInWorldNode(true)
        .setTagName("proxy")
        .setIdlePowerUsage(2.0)
        .setExposedOnSides(EnumSet.allOf(Direction::class.java))

    // ── Inventory: 1 cell slot ──
    val cellInventory = object : ItemStackHandler(2) {
        override fun onContentsChanged(slot: Int) {
            super.onContentsChanged(slot)
            this@SpatialAssemblerBlockEntity.setChanged()
            this@SpatialAssemblerBlockEntity.sendData()
        }

        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            return when (slot) {
                0 -> isSpatialCell(stack) // Input: only spatial cells
                1 -> false // Output: extraction only
                else -> false
            }
        }
    }

    // ── State ──
    private var lastRedstoneState = YesNo.UNDECIDED

    /** Status message key for GUI display. */
    var statusMsg = "idle"
        private set

    /** Whether the cell currently contains stored blocks (for display purposes). */
    var cellHasRegion = false
        private set

    // ── Lifecycle ──
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun initialize() {
        super.initialize()
        if (level != null && !level!!.isClientSide) {
            mainNode.create(level, worldPosition)
        }
    }

    override fun onChunkUnloaded() {
        super.onChunkUnloaded()
        mainNode.destroy()
    }

    override fun remove() {
        mainNode.destroy()
        super.remove()
    }

    // ── IGridConnectedBlockEntity ──
    override fun getMainNode(): IManagedGridNode = mainNode
    override fun getGridConnectableSides(orientation: BlockOrientation): Set<Direction> = EnumSet.allOf(Direction::class.java)
    override fun saveChanges() = setChanged()
    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {}
    override fun getCableConnectionType(dir: Direction): AECableType = AECableType.SMART
    override fun getActionableNode(): IGridNode? = mainNode.node
    override fun getGridNode(dir: Direction): IGridNode? {
        val node = mainNode.node ?: return null
        if (node is appeng.me.InWorldGridNode && node.isExposedOnSide(dir)) {
            return node
        }
        return null
    }

    // ── Redstone Logic (modeled after SpatialIOPortBlockEntity) ──
    fun updateRedstoneState() {
        if (level == null || level!!.isClientSide) return
        val currentState = if (level!!.getBestNeighborSignal(worldPosition) != 0) YesNo.YES else YesNo.NO
        if (lastRedstoneState != currentState) {
            lastRedstoneState = currentState
            if (lastRedstoneState == YesNo.YES) {
                triggerTransition()
            }
        }
    }

    private fun triggerTransition() {
        if (level == null || level!!.isClientSide) return
        val cell = cellInventory.getStackInSlot(0)
        if (!isSpatialCell(cell)) {
            statusMsg = "noCell"
            sendData()
            return
        }

        // Use TickHandler-like deferred approach for safety (same as AE2)
        // But for simplicity, we execute directly in the current tick
        transition()
    }

    private fun transition() {
        val serverLevel = level as? ServerLevel ?: return
        val cell = cellInventory.getStackInSlot(0)
        if (!isSpatialCell(cell)) return
        // Output slot must be empty
        if (!cellInventory.getStackInSlot(1).isEmpty) {
            statusMsg = "outputFull"
            sendData()
            return
        }

        val sc = cell.item as ISpatialStorageCell

        // Check grid is active
        if (!mainNode.isActive) {
            statusMsg = "noGrid"
            sendData()
            return
        }

        // Find glued blocks via BFS
        val glueResult = findGluedRegion()
        if (glueResult == null) {
            statusMsg = "noGlue"
            sendData()
            return
        }

        val (min, max) = glueResult
        val sizeX = max.x - min.x + 1
        val sizeY = max.y - min.y + 1
        val sizeZ = max.z - min.z + 1
        val maxDim = sc.getMaxStoredDim(cell)

        // Validate region fits in cell
        if (sizeX > maxDim || sizeY > maxDim || sizeZ > maxDim) {
            statusMsg = "tooLarge"
            sendData()
            return
        }

        // Calculate power cost (volume-based, like AE2)
        val volume = sizeX.toLong() * sizeY.toLong() * sizeZ.toLong()
        val requiredPower = volume * POWER_PER_BLOCK

        // Check power
        val grid = mainNode.grid ?: return
        val energy = grid.energyService
        val extracted = energy.extractAEPower(requiredPower, Actionable.SIMULATE, PowerMultiplier.CONFIG)
        if (extracted < requiredPower * 0.999) {
            statusMsg = "noPower"
            sendData()
            return
        }

        // Get player ID from grid node owner
        val playerId = mainNode.node?.owningPlayerId ?: 0

        // Perform the spatial transition
        val success = sc.doSpatialTransition(cell, serverLevel, min, max, playerId)
        if (success) {
            energy.extractAEPower(requiredPower, Actionable.MODULATE, PowerMultiplier.CONFIG)
            // Move cell from input to output slot
            cellInventory.setStackInSlot(0, ItemStack.EMPTY)
            cellInventory.setStackInSlot(1, cell)
            statusMsg = "transitionDone"
            cellHasRegion = sc.getAllocatedPlotId(cell) != -1
        } else {
            statusMsg = "transitionFailed"
        }
        sendData()
    }

    /**
     * BFS from all adjacent positions, finding blocks connected by SuperGlue.
     * Returns the AABB (min, max) of the glued region, or null if no glue found.
     */
    private fun findGluedRegion(): Pair<BlockPos, BlockPos>? {
        val level = this.level ?: return null

        // Start BFS from all 6 neighbors of the assembler
        val frontier = LinkedList<BlockPos>()
        val visited = HashSet<BlockPos>()

        // Seed: check all 6 directions from this block
        for (dir in Direction.entries) {
            val neighbor = worldPosition.relative(dir)
            if (!neighbor.equals(worldPosition) && !visited.contains(neighbor)) {
                // Check if there's any glue involving this neighbor
                if (hasGlueAt(neighbor)) {
                    frontier.add(neighbor)
                    visited.add(neighbor)
                }
            }
        }

        if (frontier.isEmpty()) return null

        // BFS expansion through glue connections
        while (frontier.isNotEmpty() && visited.size < MAX_GLUE_SEARCH) {
            val current = frontier.poll()
            for (dir in Direction.entries) {
                val next = current.relative(dir)
                if (visited.contains(next)) continue
                if (next == worldPosition) continue // Don't include the assembler itself

                // Check if current and next are glued together
                if (SuperGlueEntity.isGlued(level, current, dir, null)) {
                    visited.add(next)
                    frontier.add(next)
                }
            }
        }

        if (visited.isEmpty()) return null

        // Calculate AABB
        var minX = Int.MAX_VALUE; var minY = Int.MAX_VALUE; var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE; var maxY = Int.MIN_VALUE; var maxZ = Int.MIN_VALUE
        for (pos in visited) {
            if (pos.x < minX) minX = pos.x
            if (pos.y < minY) minY = pos.y
            if (pos.z < minZ) minZ = pos.z
            if (pos.x > maxX) maxX = pos.x
            if (pos.y > maxY) maxY = pos.y
            if (pos.z > maxZ) maxZ = pos.z
        }

        return Pair(BlockPos(minX, minY, minZ), BlockPos(maxX, maxY, maxZ))
    }

    /**
     * Check if there's any SuperGlue entity touching the given block position.
     */
    private fun hasGlueAt(pos: BlockPos): Boolean {
        val level = this.level ?: return false
        val searchBox = AABB(pos).inflate(0.5)
        val glueEntities = level.getEntitiesOfClass(SuperGlueEntity::class.java, searchBox)
        return glueEntities.isNotEmpty()
    }

    private fun isSpatialCell(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val item = stack.item
        if (item is ISpatialStorageCell) {
            return item.isSpatialStorage(stack)
        }
        return false
    }

    // ── Menu Provider ──
    override fun sendToMenu(buffer: RegistryFriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeNbt(getUpdateTag(buffer.registryAccess()))
    }

    override fun createMenu(id: Int, inv: Inventory, player: Player): AbstractContainerMenu {
        return SpatialAssemblerMenu(id, inv, this)
    }

    override fun getDisplayName(): Component {
        return Component.translatable("block.appliedcreate.spatial_assembler")
    }

    // ── NBT ──
    override fun write(compound: CompoundTag, registries: net.minecraft.core.HolderLookup.Provider, clientPacket: Boolean) {
        super.write(compound, registries, clientPacket)
        mainNode.saveToNBT(compound)
        compound.put("CellInventory", cellInventory.serializeNBT(registries))
        compound.putInt("LastRedstone", lastRedstoneState.ordinal)
        compound.putString("Status", statusMsg)
        compound.putBoolean("CellHasRegion", cellHasRegion)
    }

    override fun read(compound: CompoundTag, registries: net.minecraft.core.HolderLookup.Provider, clientPacket: Boolean) {
        super.read(compound, registries, clientPacket)
        mainNode.loadFromNBT(compound)
        cellInventory.deserializeNBT(registries, compound.getCompound("CellInventory"))
        if (compound.contains("LastRedstone")) {
            lastRedstoneState = YesNo.entries[compound.getInt("LastRedstone")]
        }
        statusMsg = compound.getString("Status").ifEmpty { "idle" }
        cellHasRegion = compound.getBoolean("CellHasRegion")
    }

    // ── Tick ──
    override fun tick() {
        super.tick()
        if (level == null || level!!.isClientSide) return

        // Update cell status for display
        val cell = cellInventory.getStackInSlot(0)
        if (isSpatialCell(cell)) {
            val sc = cell.item as ISpatialStorageCell
            cellHasRegion = sc.getAllocatedPlotId(cell) != -1
        } else if (cellInventory.getStackInSlot(1).let { isSpatialCell(it) }) {
            val outCell = cellInventory.getStackInSlot(1)
            val sc = outCell.item as ISpatialStorageCell
            cellHasRegion = sc.getAllocatedPlotId(outCell) != -1
        } else {
            cellHasRegion = false
        }
    }
}
