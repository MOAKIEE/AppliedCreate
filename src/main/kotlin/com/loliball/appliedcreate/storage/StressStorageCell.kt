package com.loliball.appliedcreate.storage

import appeng.api.config.FuzzyMode
import appeng.api.ids.AEComponents
import appeng.api.stacks.AEKey
import appeng.api.stacks.AEKeyType
import appeng.api.storage.StorageCells
import appeng.api.storage.cells.CellState
import appeng.api.storage.cells.IBasicCellItem
import appeng.api.upgrades.IUpgradeInventory
import appeng.api.upgrades.UpgradeInventories
import appeng.core.localization.PlayerMessages
import appeng.util.ConfigInventory
import appeng.util.InteractionUtil
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.tooltip.TooltipComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import java.util.Optional
import java.util.function.Supplier

class StressStorageCell(
    properties: Properties,
    private val idleDrain: Double,
    kilobytes: Int,
    private val bytesPerType: Int,
    private val totalTypes: Int,
    private val housingItem: Supplier<Item>,
    private val componentItem: Supplier<Item>
) : Item(properties.stacksTo(1)), IBasicCellItem {

    private val totalBytes: Int = kilobytes * 1024

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        lines: MutableList<Component>,
        advancedTooltips: TooltipFlag
    ) {
        addCellInformationToTooltip(stack, lines)
    }

    override fun getTooltipImage(stack: ItemStack): Optional<TooltipComponent> {
        return getCellTooltipImage(stack)
    }

    override fun getKeyType(): AEKeyType = StressKeyType.TYPE

    override fun getBytes(cellItem: ItemStack): Int = totalBytes

    override fun getTotalTypes(cellItem: ItemStack): Int = totalTypes

    override fun getIdleDrain(): Double = idleDrain

    override fun getBytesPerType(cellItem: ItemStack): Int = bytesPerType

    override fun getUpgrades(stack: ItemStack): IUpgradeInventory {
        return UpgradeInventories.forItem(stack, 0)
    }

    override fun getConfigInventory(stack: ItemStack): ConfigInventory {
        return ConfigInventory.configStacks(0).build()
    }

    override fun getFuzzyMode(stack: ItemStack): FuzzyMode {
        return stack.getOrDefault(AEComponents.STORAGE_CELL_FUZZY_MODE, FuzzyMode.IGNORE_ALL)
    }

    override fun setFuzzyMode(stack: ItemStack, fzMode: FuzzyMode) {
        stack.set(AEComponents.STORAGE_CELL_FUZZY_MODE, fzMode)
    }

    override fun isBlackListed(cellItem: ItemStack, requestedAddition: AEKey): Boolean {
        return requestedAddition !is StressKey
    }

    override fun storableInStorageCell(): Boolean = false

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (disassembleDrive(stack, level, player)) {
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
        }
        return InteractionResultHolder.pass(stack)
    }

    override fun onItemUseFirst(stack: ItemStack, context: UseOnContext): InteractionResult {
        return if (disassembleDrive(stack, context.level, context.player ?: return InteractionResult.PASS)) {
            InteractionResult.sidedSuccess(context.level.isClientSide)
        } else {
            InteractionResult.PASS
        }
    }

    private fun disassembleDrive(stack: ItemStack, level: Level, player: Player): Boolean {
        if (!InteractionUtil.isInAlternateUseMode(player)) return false

        val playerInventory = player.inventory
        if (playerInventory.getSelected() != stack) return false

        val inv = StorageCells.getCellInventory(stack, null)
        if (inv != null && !inv.availableStacks.isEmpty) {
            player.displayClientMessage(PlayerMessages.OnlyEmptyCellsCanBeDisassembled.text(), true)
            return false
        }

        playerInventory.setItem(playerInventory.selected, ItemStack.EMPTY)
        playerInventory.placeItemBackInInventory(ItemStack(housingItem.get()))
        playerInventory.placeItemBackInInventory(ItemStack(componentItem.get()))
        getUpgrades(stack).forEach { playerInventory.placeItemBackInInventory(it) }

        return true
    }

    companion object {
        @JvmStatic
        fun getColor(stack: ItemStack, tintIndex: Int): Int {
            if (tintIndex == 1) {
                val cellInv = StorageCells.getCellInventory(stack, null)
                val cellStatus = cellInv?.status ?: CellState.EMPTY
                return cellStatus.stateColor or (0xFF shl 24)  // Force full alpha
            }
            return -1  // 0xFFFFFFFF = opaque white (no tint)
        }
    }
}
