package com.loliball.appliedcreate.gui

import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.block.entity.BrassPatternProviderBlockEntity
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.SlotItemHandler

class BrassPatternProviderMenu : AbstractContainerMenu {

    val blockEntity: BrassPatternProviderBlockEntity
    private val levelAccess: ContainerLevelAccess

    companion object {
        const val PATTERN_SLOTS = 36
        const val COLS = 9
        const val ROWS = 4
        const val SLOT_SIZE = 18
        const val PATTERN_START_X = 8
        const val PATTERN_START_Y = 32

        const val PLAYER_INV_START_X = 8
        const val PLAYER_INV_START_Y = 126
        const val PLAYER_HOTBAR_Y = 184
    }

    constructor(windowId: Int, playerInventory: Inventory, buf: FriendlyByteBuf) : this(
        windowId,
        playerInventory,
        playerInventory.player.level().getBlockEntity(buf.readBlockPos()) as? BrassPatternProviderBlockEntity
            ?: throw IllegalStateException("Block entity not found")
    )

    constructor(windowId: Int, playerInventory: Inventory, be: BrassPatternProviderBlockEntity) : super(
        AppliedCreate.BRASS_PATTERN_PROVIDER_MENU.get(),
        windowId
    ) {
        this.blockEntity = be
        this.levelAccess = ContainerLevelAccess.create(be.level!!, be.blockPos)

        val handler = be.logic.inventory

        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                addSlot(
                    SlotItemHandler(
                        handler,
                        col + row * COLS,
                        PATTERN_START_X + col * SLOT_SIZE,
                        PATTERN_START_Y + row * SLOT_SIZE
                    )
                )
            }
        }

        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_START_X + col * SLOT_SIZE, PLAYER_INV_START_Y + row * SLOT_SIZE))
            }
        }

        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, PLAYER_INV_START_X + col * SLOT_SIZE, PLAYER_HOTBAR_Y))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return result
        if (!slot.hasItem()) return result

        val stack = slot.item
        result = stack.copy()

        if (index < PATTERN_SLOTS) {
            if (!moveItemStackTo(stack, PATTERN_SLOTS, slots.size, true)) {
                return ItemStack.EMPTY
            }
        } else {
            if (!moveItemStackTo(stack, 0, PATTERN_SLOTS, false)) {
                return ItemStack.EMPTY
            }
        }

        if (stack.isEmpty) {
            slot.setByPlayer(ItemStack.EMPTY)
        } else {
            slot.setChanged()
        }

        return result
    }

    override fun stillValid(player: Player): Boolean {
        return stillValid(levelAccess, player, AppliedCreate.BRASS_PATTERN_PROVIDER_BLOCK.get())
    }
}
