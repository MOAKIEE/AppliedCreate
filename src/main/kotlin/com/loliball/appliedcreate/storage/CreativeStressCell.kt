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
import net.minecraft.world.level.Level

class CreativeStressCell(properties: Properties) : Item(properties.stacksTo(1)) {

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        lines: MutableList<Component>,
        advancedTooltips: TooltipFlag
    ) {
        lines.add(Component.translatable("item.appliedcreate.creative_stress_cell.tooltip"))
    }

    class Inventory(private val stack: ItemStack) : StorageCell {

        override fun getStatus(): CellState = CellState.NOT_EMPTY

        override fun getIdleDrain(): Double = 0.0

        override fun getDescription(): Component {
            return Component.translatable("item.appliedcreate.creative_stress_cell")
        }

        override fun persist() {}

        override fun insert(what: AEKey, amount: Long, mode: Actionable, source: IActionSource): Long {
            return if (what is StressKey) amount else 0
        }

        override fun extract(what: AEKey, amount: Long, mode: Actionable, source: IActionSource): Long {
            return if (what is StressKey) amount else 0
        }

        override fun getAvailableStacks(out: KeyCounter) {
            out.add(StressKey.INSTANCE, Long.MAX_VALUE / 2)
        }

        override fun isPreferredStorageFor(what: AEKey, source: IActionSource): Boolean {
            return what is StressKey
        }
    }

    object Handler : ICellHandler {

        override fun isCell(stack: ItemStack): Boolean {
            return stack.item is CreativeStressCell
        }

        override fun getCellInventory(stack: ItemStack, host: ISaveProvider?): StorageCell? {
            return if (isCell(stack)) Inventory(stack) else null
        }
    }
}
