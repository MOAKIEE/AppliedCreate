package com.loliball.appliedcreate.gui

import appeng.api.parts.IPartHost
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.part.AndesitePatternProviderPart
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.SlotItemHandler

class AndesitePatternProviderPartMenu : AbstractContainerMenu {

    val part: AndesitePatternProviderPart

    companion object {
        const val PATTERN_SLOTS = 9
        const val SLOT_SIZE = 18
        const val PATTERN_START_X = 8
        const val PATTERN_START_Y = 32

        const val PLAYER_INV_START_X = 8
        const val PLAYER_INV_START_Y = 84
        const val PLAYER_HOTBAR_Y = 142
    }

    constructor(windowId: Int, playerInventory: Inventory, buf: FriendlyByteBuf) : this(
        windowId,
        playerInventory,
        findPart(playerInventory, buf)
    )

    constructor(windowId: Int, playerInventory: Inventory, part: AndesitePatternProviderPart) : super(
        AppliedCreate.ANDESITE_PATTERN_PROVIDER_PART_MENU.get(),
        windowId
    ) {
        this.part = part

        val handler = part.logic.inventory

        for (col in 0 until PATTERN_SLOTS) {
            addSlot(
                SlotItemHandler(
                    handler,
                    col,
                    PATTERN_START_X + col * SLOT_SIZE,
                    PATTERN_START_Y
                )
            )
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
        val be = part.blockEntity ?: return false
        return player.distanceToSqr(
            be.blockPos.x + 0.5,
            be.blockPos.y + 0.5,
            be.blockPos.z + 0.5
        ) <= 64.0
    }
}

private fun findPart(playerInventory: Inventory, buf: FriendlyByteBuf): AndesitePatternProviderPart {
    val pos = buf.readBlockPos()
    val side = buf.readEnum(Direction::class.java)
    val level = playerInventory.player.level()
    val be = level.getBlockEntity(pos)
    if (be is IPartHost) {
        val part = be.getPart(side)
        if (part is AndesitePatternProviderPart) {
            return part
        }
    }
    throw IllegalStateException("Andesite Pattern Provider Part not found at $pos side $side")
}
