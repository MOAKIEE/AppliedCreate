package com.loliball.appliedcreate.energy

import appeng.api.config.Actionable
import appeng.api.ids.AEComponents
import appeng.api.networking.GridFlags
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.security.IActionSource
import appeng.api.orientation.BlockOrientation
import appeng.api.storage.StorageHelper
import appeng.util.SettingsFrom
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.config.ACConfig
import com.loliball.appliedcreate.storage.StressKey
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.foundation.utility.CreateLang
import com.simibubi.create.infrastructure.config.AllConfigs
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponentMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import java.util.EnumSet
import kotlin.math.abs

/** Mode and output RPM are the only settings. Capacity belongs to the network allocator. */
class MEGearboxBlockEntity(
    type: BlockEntityType<*>, pos: BlockPos, state: BlockState
) : NetworkedGeneratingKineticBlockEntity(type, pos, state) {
    companion object {
        fun getDefaultSpeed(): Int = ACConfig.SERVER.meGearboxDefaultSpeed.get()
        fun getMaxSpeed(): Int = AllConfigs.server().kinetics.maxRotationSpeed.get()
        fun getMaxStress(): Float = ACConfig.SERVER.meGearboxMaxStress.get().toFloat()
    }

    enum class Mode { EXPORT, IMPORT }

    var mode: Mode = Mode.EXPORT
        private set
    private var readingSettings = false
    private var unloading = false
    private var rotationEnabled = false
    private var exportRate = 0L
    private var exportCapacity = 0.0
    private var importRate = 0L
    private val budget = StressBudget()
    var transferRate = 0L
        private set

    var configuredSpeed: Int = getDefaultSpeed()
        set(value) {
            val max = getMaxSpeed()
            val clamped = value.coerceIn(-max, max).let { if (it == 0) 1 else it }
            if (field != clamped) {
                field = clamped
                if (!readingSettings) onConfigChanged()
            }
        }

    init {
        mainNode.setVisualRepresentation(AppliedCreate.ME_GEARBOX_BLOCK.asItem())
        mainNode.setIdlePowerUsage(2.0)
        mainNode.setFlags(GridFlags.REQUIRE_CHANNEL)
    }

    override fun getGridConnectableSides(orientation: BlockOrientation): Set<Direction> =
        EnumSet.allOf(Direction::class.java)

    override fun initialize() {
        unloading = false
        super.initialize()
        StressNetworkController.register(this)
        refreshRotation()
        updateAllocation()
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        if (!readingSettings && level is ServerLevel) {
            refreshRotation()
            updateAllocation()
        }
    }

    private fun refreshRotation() {
        if (level !is ServerLevel) return
        val enabled = !unloading && mainNode.isActive
        if (rotationEnabled != enabled) {
            rotationEnabled = enabled
            if (!enabled) {
                setAllocation(0, 0)
                transferRate = 0
            }
            updateGeneratedRotation()
            notifyUpdate()
        }
    }

    private fun updateAllocation() {
        if (level is ServerLevel && hasNetwork()) StressNetworkController.update(getOrCreateNetwork())
    }

    private fun onConfigChanged() {
        if (level !is ServerLevel || unloading) return
        setAllocation(0, 0)
        refreshRotation()
        updateGeneratedRotation()
        updateAllocation()
        notifyUpdate()
    }

    fun toggleMode() {
        if (level !is ServerLevel) return
        mode = if (mode == Mode.EXPORT) Mode.IMPORT else Mode.EXPORT
        onConfigChanged()
    }

    override fun tick() {
        super.tick()
        if (level !is ServerLevel || unloading) return
        StressNetworkController.register(this)
        refreshRotation()
    }

    override fun getGeneratedSpeed(): Float {
        if (mode != Mode.EXPORT || !rotationEnabled || unloading) return 0f
        return convertToDirection(configuredSpeed.toFloat(), blockState.getValue(DirectionalKineticBlock.FACING))
    }

    override fun calculateAddedStressCapacity(): Float {
        val rpm = abs(generatedSpeed)
        val capacity = if (mode == Mode.EXPORT && rpm > 0) exportCapacity / rpm else 0.0
        lastCapacityProvided = capacity.toFloat()
        return lastCapacityProvided
    }

    override fun calculateStressApplied(): Float {
        val rpm = abs(theoreticalSpeed)
        val impact = if (mode == Mode.IMPORT && rpm > 0) importRate.toDouble() / rpm else 0.0
        lastStressApplied = impact.toFloat()
        return lastStressApplied
    }

    fun getStressApplied(): Float = lastStressApplied
    fun allocatedStress(): Long = if (mode == Mode.EXPORT) exportRate else importRate
    fun allocatedCapacity(): Double = if (mode == Mode.EXPORT) exportCapacity else importRate.toDouble()

    internal fun canTransfer(): Boolean = !unloading && !isRemoved && mainNode.isActive && mainNode.grid != null

    internal fun transferLimit(): Long {
        if (!canTransfer()) return 0
        val rpm = abs(if (mode == Mode.EXPORT) generatedSpeed else theoreticalSpeed)
        return StressDistribution.units(getMaxStress().toDouble() * rpm)
    }

    internal fun setAllocation(sending: Long, receiving: Long, supplying: Double = sending.toDouble()) {
        val capacity = supplying.coerceIn(0.0, sending.toDouble())
        val changed = exportRate != sending || importRate != receiving || exportCapacity != capacity
        exportRate = sending
        exportCapacity = capacity
        importRate = receiving
        if (mode == Mode.EXPORT) transferRate = sending
        else if (receiving == 0L) transferRate = 0
        calculateAddedStressCapacity()
        calculateStressApplied()
        if (changed && !readingSettings && level is ServerLevel && !unloading) notifyUpdate()
    }

    internal fun reserveStress(tick: Long, target: Long): Long {
        if (!canTransfer()) return 0
        val grid = mainNode.grid ?: return 0
        val before = budget.stored()
        val available = budget.reserve(tick, target) { requested ->
            StorageHelper.poweredExtraction(grid.energyService, grid.storageService.inventory, StressKey.INSTANCE,
                requested, IActionSource.ofMachine(mainNode::getNode), Actionable.MODULATE)
        }
        if (budget.stored() != before) setChanged()
        return available
    }

    internal fun commitStress(tick: Long, amount: Long) {
        budget.commit(tick, amount)
        transferRate = amount
        if (amount > 0) setChanged()
    }

    internal fun simulateImport(requested: Long): Long {
        if (!canTransfer()) return 0
        val grid = mainNode.grid ?: return 0
        return StorageHelper.poweredInsert(grid.energyService, grid.storageService.inventory, StressKey.INSTANCE,
            requested, IActionSource.ofMachine(mainNode::getNode), Actionable.SIMULATE)
    }

    internal fun collectStress(requested: Long): Long {
        if (!canTransfer() || mode != Mode.IMPORT || isOverStressed || abs(speed) == 0f) return 0
        val grid = mainNode.grid ?: return 0
        return StorageHelper.poweredInsert(grid.energyService, grid.storageService.inventory, StressKey.INSTANCE,
            requested, IActionSource.ofMachine(mainNode::getNode), Actionable.MODULATE)
    }

    internal fun finishCollection(amount: Long) {
        val changed = transferRate != amount
        transferRate = amount
        if (changed) notifyUpdate()
    }

    internal fun returnUnusedStress() {
        if (!canTransfer()) return
        val grid = mainNode.grid ?: return
        // These units were already paid for on extraction; a refund must not charge AE again.
        if (budget.refund { grid.storageService.inventory.insert(StressKey.INSTANCE, it,
                Actionable.MODULATE, IActionSource.ofMachine(mainNode::getNode)) } > 0) setChanged()
    }

    fun storedStress(): Long = budget.stored()

    private fun leaveNetwork() {
        unloading = true
        setAllocation(0, 0)
        transferRate = 0
        StressNetworkController.unregister(this)
    }

    override fun remove() {
        leaveNetwork()
        super.remove()
    }

    override fun onChunkUnloaded() {
        leaveNetwork()
        super.onChunkUnloaded()
    }

    override fun write(compound: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
        super.write(compound, registries, clientPacket)
        writeSettingsAndBuffer(compound)
        if (clientPacket) {
            compound.putBoolean("RotationEnabled", rotationEnabled)
            compound.putLong("ExportRate", exportRate)
            compound.putDouble("ExportCapacity", exportCapacity)
            compound.putLong("ImportRate", importRate)
            compound.putLong("TransferRate", transferRate)
        }
    }

    fun writeSettingsAndBuffer(compound: CompoundTag) {
        compound.putString("GearboxMode", mode.name)
        compound.putInt("GearboxSpeed", configuredSpeed)
        compound.putLong("StressBuffer", budget.stored())
    }

    override fun read(compound: CompoundTag, registries: HolderLookup.Provider, clientPacket: Boolean) {
        readingSettings = true
        try {
            super.read(compound, registries, clientPacket)
            mode = runCatching { Mode.valueOf(compound.getString("GearboxMode")) }.getOrDefault(Mode.EXPORT)
            configuredSpeed = compound.getInt("GearboxSpeed").let { if (it == 0) getDefaultSpeed() else it }
            budget.restore(compound.getLong("StressBuffer"))
            if (clientPacket) {
                rotationEnabled = compound.getBoolean("RotationEnabled")
                setAllocation(compound.getLong("ExportRate"), compound.getLong("ImportRate"),
                    if (compound.contains("ExportCapacity")) compound.getDouble("ExportCapacity")
                    else compound.getLong("ExportRate").toDouble())
                transferRate = compound.getLong("TransferRate")
            } else {
                // Old GearboxStress and saved automatic allocations are deliberately not restored.
                rotationEnabled = false
                setAllocation(0, 0)
                transferRate = 0
            }
        } finally {
            readingSettings = false
        }
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        CreateLang.text("").add(Component.translatable("appliedcreate.me_gearbox.title"))
            .style(ChatFormatting.GOLD).forGoggles(tooltip)
        val modeKey = if (mode == Mode.EXPORT) "appliedcreate.me_gearbox.mode.export" else "appliedcreate.me_gearbox.mode.import"
        CreateLang.text("").add(Component.translatable(modeKey)).style(ChatFormatting.AQUA).forGoggles(tooltip, 1)
        CreateLang.text("").add(Component.translatable("gui.appliedcreate.MEGearbox.actual_stress", allocatedCapacity()))
            .style(ChatFormatting.GRAY).forGoggles(tooltip, 1)
        val sign = if (mode == Mode.IMPORT) "+" else "-"
        CreateLang.text("").add(Component.translatable("appliedcreate.me_gearbox.transfer"))
            .add(CreateLang.text(sign)).add(CreateLang.number(transferRate.toDouble()))
            .add(Component.translatable("appliedcreate.me_gearbox.transfer.unit"))
            .style(ChatFormatting.GOLD).forGoggles(tooltip, 1)
        return true
    }

    fun exportSettings(mode: SettingsFrom, builder: DataComponentMap.Builder, player: Player?) {
        if (mode != SettingsFrom.MEMORY_CARD) return
        builder.set(AEComponents.EXPORTED_SETTINGS_SOURCE, AppliedCreate.ME_GEARBOX_BLOCK.get().name)
        builder.set(AEComponents.EXPORTED_SETTINGS, mapOf("mode" to this.mode.ordinal.toString(),
            "configuredSpeed" to configuredSpeed.toString()))
    }

    fun importSettings(mode: SettingsFrom, input: DataComponentMap, player: Player?) {
        if (mode != SettingsFrom.MEMORY_CARD) return
        val data = input.get(AEComponents.EXPORTED_SETTINGS) ?: return
        readingSettings = true
        try {
            data["mode"]?.toIntOrNull()?.let { this.mode = Mode.entries.getOrElse(it) { Mode.EXPORT } }
            data["configuredSpeed"]?.toIntOrNull()?.let { configuredSpeed = it }
            // Compatibility: old memory cards may contain configuredStress; it is ignored.
        } finally {
            readingSettings = false
        }
        onConfigChanged()
    }
}
