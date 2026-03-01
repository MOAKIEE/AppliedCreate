package com.loliball.appliedcreate.cannon

import appeng.api.config.Actionable
import appeng.api.config.PowerMultiplier
import appeng.api.networking.GridHelper
import appeng.api.networking.IGridNode
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IManagedGridNode
import appeng.api.networking.crafting.ICraftingRequester
import appeng.api.networking.crafting.ICraftingLink
import appeng.core.definitions.AEItems
import com.google.common.collect.ImmutableSet
import appeng.api.orientation.BlockOrientation
import appeng.api.stacks.AEItemKey
import appeng.api.storage.StorageHelper
import appeng.api.upgrades.UpgradeInventories
import appeng.api.util.AECableType
import appeng.helpers.MultiCraftingTracker
import appeng.me.helpers.BlockEntityNodeListener
import appeng.me.helpers.IGridConnectedBlockEntity
import appeng.me.helpers.MachineSource
import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.schematics.SchematicPrinter
import com.simibubi.create.content.schematics.cannon.LaunchedItem
import com.simibubi.create.content.schematics.cannon.MaterialChecklist
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity
import com.simibubi.create.content.schematics.requirement.ItemRequirement
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.BlockHelper
import com.simibubi.create.infrastructure.config.AllConfigs
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.items.ItemStackHandler
import net.minecraftforge.registries.ForgeRegistries
import java.util.EnumSet
import java.util.LinkedList

class MEBlueprintCannonBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(type, pos, state), MenuProvider, IGridConnectedBlockEntity, ICraftingRequester {

    companion object {
        const val AE_POWER_PER_SHOT = 5.0
    }

    @Suppress("UNCHECKED_CAST")
    private val mainNode: IManagedGridNode = GridHelper.createManagedNode(
        this, BlockEntityNodeListener.INSTANCE as IGridNodeListener<IGridConnectedBlockEntity>
    )
        .setVisualRepresentation(AppliedCreate.ME_BLUEPRINT_CANNON_ITEM.get())
        .setInWorldNode(true)
        .setTagName("proxy")
        .setIdlePowerUsage(2.0)
        .setExposedOnSides(EnumSet.allOf(Direction::class.java))

    private val actionSource = MachineSource(this)
    private val craftingTracker = MultiCraftingTracker(this, 9)
    val upgradeInventory = UpgradeInventories.forMachine(AppliedCreate.ME_BLUEPRINT_CANNON_ITEM.get(), 5) { this.setChanged() }

    val inventory = object : ItemStackHandler(2) {
        override fun onContentsChanged(slot: Int) {
            super.onContentsChanged(slot)
            this@MEBlueprintCannonBlockEntity.setChanged()
        }

        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            return when (slot) {
                0 -> stack.item == ForgeRegistries.ITEMS.getValue(ResourceLocation("create", "schematic"))
                1 -> false
                else -> super.isItemValid(slot, stack)
            }
        }
    }

    val printer = SchematicPrinter()
    val checklist = MaterialChecklist()
    var state = SchematicannonBlockEntity.State.STOPPED
    var statusMsg = "idle"
    val flyingBlocks: MutableList<LaunchedItem> = LinkedList()
    var missingItem: ItemStack? = null
    var positionNotLoaded = false
    var sendUpdate = false
    var dontUpdateChecklist = false
    var neighbourCheckCooldown = 0
    private var printerCooldown = 0
    private var skipsLeft = 0
    private var blockSkipped = false
    var previousTarget: BlockPos? = null
    var schematicProgress = 0f
    var blocksPlaced = 0
    var blocksToPlace = 0
    var replaceMode = 2
    var skipMissing = false
    var replaceBlockEntities = false
    var firstRenderTick = false
    var defaultYaw = 0f
    val hasCreativeCrate = false

    init {
        setLazyTickRate(30)
        mainNode.addService(ICraftingRequester::class.java, this)
    }

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

    override fun getRequestedJobs(): ImmutableSet<ICraftingLink> = craftingTracker.requestedJobs
    override fun insertCraftedItems(link: ICraftingLink, what: appeng.api.stacks.AEKey, amount: Long, mode: Actionable): Long {
        return amount
    }

    override fun jobStateChange(link: ICraftingLink) {
        craftingTracker.jobStateChange(link)
        setChanged()
    }

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

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun lazyTick() {
        super.lazyTick()
    }

    override fun tick() {
        super.tick()

        if (state != SchematicannonBlockEntity.State.STOPPED && neighbourCheckCooldown-- <= 0) {
            neighbourCheckCooldown = SchematicannonBlockEntity.NEIGHBOUR_CHECKING
        }

        firstRenderTick = true
        previousTarget = printer.currentTarget
        tickFlyingBlocks()

        if (level!!.isClientSide) return

        skipsLeft = 1000
        blockSkipped = true

        while (blockSkipped && skipsLeft-- > 0) {
            tickPrinter()
        }

        schematicProgress = 0f
        if (blocksToPlace > 0) {
            schematicProgress = blocksPlaced.toFloat() / blocksToPlace
        }

        if (sendUpdate) {
            sendUpdate = false
            level!!.sendBlockUpdated(worldPosition, blockState, blockState, 6)
        }
    }

    private fun tickFlyingBlocks() {
        val toRemove: MutableList<LaunchedItem> = LinkedList()
        for (b in flyingBlocks) {
            if (b.update(level)) {
                toRemove.add(b)
            }
        }
        flyingBlocks.removeAll(toRemove)
    }

    private fun tryConsumeAEPower(): Boolean {
        val grid = mainNode.grid ?: return false
        val energyService = grid.energyService ?: return false
        val costPerShot = AE_POWER_PER_SHOT
        if (energyService.extractAEPower(costPerShot, Actionable.SIMULATE, PowerMultiplier.CONFIG) >= costPerShot) {
            energyService.extractAEPower(costPerShot, Actionable.MODULATE, PowerMultiplier.CONFIG)
            return true
        }
        return false
    }

    private fun tickPrinter() {
        val blueprint = inventory.getStackInSlot(0)
        blockSkipped = false

        if (blueprint.isEmpty && statusMsg != "idle" && inventory.getStackInSlot(1).isEmpty) {
            state = SchematicannonBlockEntity.State.STOPPED
            statusMsg = "idle"
            sendUpdate = true
            return
        }

        if (state == SchematicannonBlockEntity.State.STOPPED) {
            if (printer.isLoaded) resetPrinter()
            return
        }

        if (state == SchematicannonBlockEntity.State.PAUSED && !positionNotLoaded && missingItem == null && statusMsg != "noAEPower") return

        if (!printer.isLoaded) {
            initializePrinter(blueprint)
            return
        }

        if (printerCooldown > 0) {
            printerCooldown--
            return
        }

        if (!tryConsumeAEPower()) {
            state = SchematicannonBlockEntity.State.PAUSED
            statusMsg = "noAEPower"
            sendUpdate = true
            return
        }

        if (missingItem == null && !positionNotLoaded) {
            if (!printer.advanceCurrentPos()) {
                finishedPrinting()
                return
            }
            sendUpdate = true
        }

        if (!level!!.isLoaded(printer.currentTarget)) {
            positionNotLoaded = true
            statusMsg = "targetNotLoaded"
            state = SchematicannonBlockEntity.State.PAUSED
            return
        } else {
            if (positionNotLoaded) {
                positionNotLoaded = false
                state = SchematicannonBlockEntity.State.RUNNING
            }
        }

        val requirement = printer.currentRequirement
        if (requirement.isInvalid || !printer.shouldPlaceCurrent(level) { pos, st, be, toReplace, toReplaceOther, isNormalCube -> shouldPlace(pos, st, be, toReplace, toReplaceOther, isNormalCube) }) {
            sendUpdate = statusMsg != "searching"
            statusMsg = "searching"
            blockSkipped = true
            return
        }

        val requiredItems = requirement.requiredItems
        if (!requirement.isEmpty) {
            for (required in requiredItems) {
                if (!grabItemsFromNetwork(required, true)) {
                    if (skipMissing) {
                        statusMsg = "skipping"
                        blockSkipped = true
                        if (missingItem != null) {
                            missingItem = null
                            state = SchematicannonBlockEntity.State.RUNNING
                        }
                        return
                    }

                    missingItem = required.stack
                    state = SchematicannonBlockEntity.State.PAUSED
                    statusMsg = "missingBlock"
                    return
                }
            }

            for (required in requiredItems) {
                grabItemsFromNetwork(required, false)
            }
        }

        state = SchematicannonBlockEntity.State.RUNNING
        val icon = if (requirement.isEmpty || requiredItems.isEmpty()) ItemStack.EMPTY else requiredItems[0].stack
        printer.handleCurrentTarget({ target, blockState, blockEntity ->
            statusMsg = if (blockState.block !== Blocks.AIR) "placing" else "clearing"
            launchBlockOrBelt(target, icon, blockState, blockEntity)
        }, { target, entity ->
            statusMsg = "placing"
            launchEntity(target, icon, entity)
        })

        val speedCards = upgradeInventory.getInstalledUpgrades(AEItems.SPEED_CARD)
        var delay = AllConfigs.server().schematics.schematicannonDelay.get()
        if (speedCards > 0) {
            delay = delay / (1 shl speedCards)
        }

        printerCooldown = delay
        sendUpdate = true
        missingItem = null
    }

    private fun grabItemsFromNetwork(required: ItemRequirement.StackRequirement, simulate: Boolean): Boolean {
        val grid = mainNode.grid ?: return false
        val storage = grid.storageService?.inventory ?: return false
        val energy = grid.energyService ?: return false
        val craftingService = grid.craftingService ?: return false

        val aeKey = AEItemKey.of(required.stack) ?: return false
        val amount = required.stack.count.toLong()

        val extracted = StorageHelper.poweredExtraction(energy, storage, aeKey, amount, actionSource, Actionable.SIMULATE)

        if (extracted >= amount) {
            if (!simulate) {
                StorageHelper.poweredExtraction(energy, storage, aeKey, amount, actionSource, Actionable.MODULATE)
            }
            return true
        }

        if (upgradeInventory.getInstalledUpgrades(AEItems.CRAFTING_CARD) > 0) {
            if (simulate) {
                return craftingService.isCraftable(aeKey)
            } else {
                craftingTracker.handleCrafting(0, aeKey, amount, level, craftingService, actionSource)
                return false
            }
        }

        return false
    }

    private fun initializePrinter(blueprint: ItemStack) {
        // In Create 1.20.1, schematic data is stored as NBT tags on the ItemStack
        val tag = blueprint.tag
        if (tag == null || !tag.contains("Anchor")) {
            state = SchematicannonBlockEntity.State.STOPPED
            statusMsg = "schematicInvalid"
            sendUpdate = true
            return
        }
        if (!tag.getBoolean("Deployed")) {
            state = SchematicannonBlockEntity.State.STOPPED
            statusMsg = "schematicNotPlaced"
            sendUpdate = true
            return
        }
        printer.loadSchematic(blueprint, level, true)
        if (printer.isErrored) {
            state = SchematicannonBlockEntity.State.STOPPED
            statusMsg = "schematicErrored"
            inventory.setStackInSlot(0, ItemStack.EMPTY)
            inventory.setStackInSlot(1, ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation("create", "empty_schematic"))!!))
            printer.resetSchematic()
            sendUpdate = true
            return
        }
        if (printer.isWorldEmpty) {
            state = SchematicannonBlockEntity.State.STOPPED
            statusMsg = "schematicExpired"
            inventory.setStackInSlot(0, ItemStack.EMPTY)
            inventory.setStackInSlot(1, ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation("create", "empty_schematic"))!!))
            printer.resetSchematic()
            sendUpdate = true
            return
        }
        if (!printer.anchor.closerThan(blockPos, SchematicannonBlockEntity.MAX_ANCHOR_DISTANCE.toDouble())) {
            state = SchematicannonBlockEntity.State.STOPPED
            statusMsg = "targetOutsideRange"
            printer.resetSchematic()
            sendUpdate = true
            return
        }
        state = SchematicannonBlockEntity.State.PAUSED
        statusMsg = "ready"
        updateChecklist()
        sendUpdate = true
        blocksToPlace += blocksPlaced
    }

    fun updateChecklist() {
        checklist.required.clear()
        checklist.damageRequired.clear()
        checklist.blocksNotLoaded = false
        if (printer.isLoaded && !printer.isErrored) {
            blocksToPlace = blocksPlaced
            blocksToPlace += printer.markAllBlockRequirements(checklist, level) { pos, st, be, toReplace, toReplaceOther, isNormalCube -> shouldPlace(pos, st, be, toReplace, toReplaceOther, isNormalCube) }
            printer.markAllEntityRequirements(checklist)
        }
        checklist.gathered.clear()

        val grid = mainNode.grid
        if (grid != null) {
            val storage = grid.storageService?.inventory
            if (storage != null) {
                val available = storage.availableStacks
                for (stack in available) {
                    val key = stack.key
                    if (key is AEItemKey) {
                        checklist.collect(key.toStack(stack.longValue.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()))
                    }
                }
            }
        }

        sendUpdate = true
    }

    private fun shouldPlace(pos: BlockPos, state: BlockState, be: BlockEntity?, toReplace: BlockState, toReplaceOther: BlockState?, isNormalCube: Boolean): Boolean {
        if (pos.closerThan(blockPos, 2.0)) return false
        if (!replaceBlockEntities && (toReplace.hasBlockEntity() || (toReplaceOther != null && toReplaceOther.hasBlockEntity()))) return false
        if (shouldIgnoreBlockState(state, be)) return false

        val placingAir = state.isAir
        if (replaceMode == 3) return true
        if (replaceMode == 2 && !placingAir) return true
        if (replaceMode == 1 && (isNormalCube || (!toReplace.isRedstoneConductor(level!!, pos) && (toReplaceOther == null || !toReplaceOther.isRedstoneConductor(level!!, pos)))) && !placingAir) return true
        if (replaceMode == 0 && !toReplace.isRedstoneConductor(level!!, pos) && (toReplaceOther == null || !toReplaceOther.isRedstoneConductor(level!!, pos)) && !placingAir) return true
        return false
    }

    private fun shouldIgnoreBlockState(state: BlockState, be: BlockEntity?): Boolean {
        if (state.block === Blocks.STRUCTURE_VOID) return true
        val requirement = ItemRequirement.of(state, be)
        if (requirement.isEmpty) return false
        if (requirement.isInvalid) return false
        return false
    }

    private fun finishedPrinting() {
        if (replaceMode == 1) {
            printer.sendBlockUpdates(level)
        }
        inventory.setStackInSlot(0, ItemStack.EMPTY)
        val emptySchematic = ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation("create", "empty_schematic"))!!, inventory.getStackInSlot(1).count + 1)
        inventory.setStackInSlot(1, emptySchematic)
        state = SchematicannonBlockEntity.State.STOPPED
        statusMsg = "finished"
        resetPrinter()
        AllSoundEvents.SCHEMATICANNON_FINISH.playOnServer(level, worldPosition)
        sendUpdate = true
    }

    private fun resetPrinter() {
        printer.resetSchematic()
        missingItem = null
        sendUpdate = true
        schematicProgress = 0f
        blocksPlaced = 0
        blocksToPlace = 0
    }

    private fun launchBlockOrBelt(target: BlockPos, icon: ItemStack, blockState: BlockState, blockEntity: BlockEntity?) {
        var st = blockState
        if (st.block == ForgeRegistries.BLOCKS.getValue(ResourceLocation("create", "belt"))) {
            st = SchematicannonBlockEntity.stripBeltIfNotLast(st)
            if (st !== Blocks.AIR.defaultBlockState()) {
                launchBlock(target, icon, st, null)
            }
            return
        }
        val data = BlockHelper.prepareBlockEntityData(st, blockEntity)
        launchBlock(target, icon, st, data)
    }

    private fun launchBlock(target: BlockPos, stack: ItemStack, state: BlockState, data: CompoundTag?) {
        if (!state.isAir) blocksPlaced++
        flyingBlocks.add(LaunchedItem.ForBlockState(blockPos, target, stack, state, data))
        playFiringSound()
    }

    private fun launchEntity(target: BlockPos, stack: ItemStack, entity: net.minecraft.world.entity.Entity) {
        blocksPlaced++
        flyingBlocks.add(LaunchedItem.ForEntity(blockPos, target, stack, entity))
        playFiringSound()
    }

    private fun playFiringSound() {
        AllSoundEvents.SCHEMATICANNON_LAUNCH_BLOCK.playOnServer(level, worldPosition)
    }

    override fun sendToMenu(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(blockPos)
        buffer.writeNbt(getUpdateTag())
    }

    override fun createMenu(id: Int, inv: Inventory, player: Player): AbstractContainerMenu {
        return MEBlueprintCannonMenu(id, inv, this)
    }

    override fun getDisplayName(): Component {
        return Component.translatable("block.appliedcreate.me_blueprint_cannon")
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        mainNode.saveToNBT(compound)
        compound.put("Inventory", inventory.serializeNBT())
        upgradeInventory.writeToNBT(compound, "Upgrades")

        compound.putFloat("Progress", schematicProgress)
        compound.putString("Status", statusMsg)
        compound.putString("State", state.name)
        compound.putInt("AmountPlaced", blocksPlaced)
        compound.putInt("AmountToPlace", blocksToPlace)

        if (missingItem != null) compound.put("MissingItem", missingItem!!.save(CompoundTag()))

        val options = CompoundTag()
        options.putInt("ReplaceMode", replaceMode)
        options.putBoolean("SkipMissing", skipMissing)
        options.putBoolean("ReplaceTileEntities", replaceBlockEntities)
        compound.put("Options", options)

        val printerData = CompoundTag()
        printer.write(printerData)
        compound.put("Printer", printerData)

        val tagFlyingBlocks = ListTag()
        for (b in flyingBlocks) tagFlyingBlocks.add(b.serializeNBT())
        compound.put("FlyingBlocks", tagFlyingBlocks)

        compound.putFloat("DefaultYaw", defaultYaw)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        mainNode.loadFromNBT(compound)
        inventory.deserializeNBT(compound.getCompound("Inventory"))
        upgradeInventory.readFromNBT(compound, "Upgrades")

        statusMsg = compound.getString("Status").ifEmpty { "idle" }
        schematicProgress = compound.getFloat("Progress")
        state = try { SchematicannonBlockEntity.State.valueOf(compound.getString("State")) } catch (e: Exception) { SchematicannonBlockEntity.State.STOPPED }
        blocksPlaced = compound.getInt("AmountPlaced")
        blocksToPlace = compound.getInt("AmountToPlace")

        missingItem = null
        if (compound.contains("MissingItem")) {
            val itemTag = compound.getCompound("MissingItem")
            val stack = ItemStack.of(itemTag)
            if (!stack.isEmpty) missingItem = stack
        }

        val options = compound.getCompound("Options")
        replaceMode = options.getInt("ReplaceMode")
        skipMissing = options.getBoolean("SkipMissing")
        replaceBlockEntities = options.getBoolean("ReplaceTileEntities")

        if (compound.contains("Printer")) printer.fromTag(compound.getCompound("Printer"), clientPacket)

        if (compound.contains("FlyingBlocks")) {
            val tagBlocks = compound.getList("FlyingBlocks", 10)
            if (tagBlocks.isEmpty()) flyingBlocks.clear()
            for (i in 0 until tagBlocks.size) {
                flyingBlocks.add(LaunchedItem.fromNBT(tagBlocks.getCompound(i), level!!.holderLookup(net.minecraft.core.registries.Registries.BLOCK)))
            }
        }

        defaultYaw = compound.getFloat("DefaultYaw")
    }
}
