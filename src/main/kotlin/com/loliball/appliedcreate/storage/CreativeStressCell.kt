package com.loliball.appliedcreate.storage

import appeng.api.config.Actionable
import appeng.api.networking.security.IActionSource
import appeng.api.stacks.AEKey
import appeng.api.stacks.KeyCounter
import appeng.api.storage.cells.CellState
import appeng.api.storage.cells.ICellHandler
import appeng.api.storage.cells.ISaveProvider
import appeng.api.storage.cells.StorageCell
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

/**
 * Creative Stress Cell — provides infinite stress storage capacity in an ME network.
 *
 * Unlike regular [StressStorageCell] which uses AE2's IBasicCellItem with finite capacity,
 * this creative-mode cell implements a custom [StorageCell] that accepts and provides
 * unlimited stress, similar to ExtendedAE's infinite water/cobblestone cells.
 */
class CreativeStressCell(properties: Properties) : Item(properties.stacksTo(1)) {

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        lines: MutableList<Component>,
        advancedTooltips: TooltipFlag
    ) {
        lines.add(Component.translatable("item.appliedcreate.creative_stress_cell.tooltip"))
    }

    /**
     * Inventory implementation for the Creative Stress Cell.
     * Provides infinite insert/extract for [StressKey] only.
     */
    class Inventory(private val stack: ItemStack) : StorageCell {

        override fun getStatus(): CellState = CellState.NOT_EMPTY

        override fun getIdleDrain(): Double = 0.0

        override fun getDescription(): Component {
            return Component.translatable("item.appliedcreate.creative_stress_cell")
        }

        override fun persist() {
            // Nothing to persist — infinite cell has no mutable state
        }

        override fun insert(what: AEKey, amount: Long, mode: Actionable, source: IActionSource): Long {
            // Accept any amount of stress — it's infinite
            return if (what is StressKey) amount else 0
        }

        override fun extract(what: AEKey, amount: Long, mode: Actionable, source: IActionSource): Long {
            // Provide any requested amount of stress — it's infinite
            return if (what is StressKey) amount else 0
        }

        override fun getAvailableStacks(out: KeyCounter) {
            // Report a large amount so the network knows stress is available
            out.add(StressKey.INSTANCE, Long.MAX_VALUE / 2)
        }

        override fun isPreferredStorageFor(what: AEKey, source: IActionSource): Boolean {
            return what is StressKey
        }
    }

    /**
     * Cell handler that tells AE2 how to create an [Inventory] from a [CreativeStressCell] item stack.
     */
    object Handler : ICellHandler {

        override fun isCell(stack: ItemStack): Boolean {
            return stack.item is CreativeStressCell
        }

        override fun getCellInventory(stack: ItemStack, host: ISaveProvider?): StorageCell? {
            return if (isCell(stack)) Inventory(stack) else null
        }
    }
}
