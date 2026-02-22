package com.loliball.appliedcreate.gui

import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.block.entity.MechanicalCraftEncoderBlockEntity
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.SlotItemHandler

class MechanicalCraftEncoderMenu : AbstractContainerMenu {

    val blockEntity: MechanicalCraftEncoderBlockEntity
    private val levelAccess: ContainerLevelAccess

    companion object {
        const val INPUT_ROWS = 9
        const val INPUT_COLS = 9
        const val INPUT_START_X = 8
        const val INPUT_START_Y = 18
        const val SLOT_SIZE = 18

        const val FILTER_SLOT_X = 174
        const val FILTER_SLOT_Y = 18

        const val OUTPUT_SLOT_X = 174
        const val OUTPUT_SLOT_Y = 54

        const val PLAYER_INV_START_X = 8
        const val PLAYER_INV_START_Y = 184
        const val PLAYER_HOTBAR_Y = 242

        const val TOTAL_BE_SLOTS = MechanicalCraftEncoderBlockEntity.TOTAL_SLOTS
    }

    constructor(windowId: Int, playerInventory: Inventory, buf: FriendlyByteBuf) : this(
        windowId,
        playerInventory,
        playerInventory.player.level().getBlockEntity(buf.readBlockPos()) as MechanicalCraftEncoderBlockEntity
    )

    constructor(windowId: Int, playerInventory: Inventory, be: MechanicalCraftEncoderBlockEntity) : super(
        AppliedCreate.MECHANICAL_CRAFT_ENCODER_MENU.get(),
        windowId
    ) {
        this.blockEntity = be
        this.levelAccess = ContainerLevelAccess.create(be.level!!, be.blockPos)

        val handler = be.inventory

        for (row in 0 until INPUT_ROWS) {
            for (col in 0 until INPUT_COLS) {
                addSlot(
                    SlotItemHandler(
                        handler,
                        row * INPUT_COLS + col,
                        INPUT_START_X + col * SLOT_SIZE,
                        INPUT_START_Y + row * SLOT_SIZE
                    )
                )
            }
        }

        addSlot(SlotItemHandler(handler, MechanicalCraftEncoderBlockEntity.FILTER_SLOT, FILTER_SLOT_X, FILTER_SLOT_Y))

        addSlot(object : SlotItemHandler(handler, MechanicalCraftEncoderBlockEntity.OUTPUT_SLOT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y) {
            override fun mayPlace(stack: ItemStack): Boolean = false
        })

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

        when {
            index < TOTAL_BE_SLOTS -> {
                if (!moveItemStackTo(stack, TOTAL_BE_SLOTS, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            }
            else -> {
                if (!moveItemStackTo(stack, 0, MechanicalCraftEncoderBlockEntity.INPUT_SLOTS, false)) {
                    return ItemStack.EMPTY
                }
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
        return stillValid(levelAccess, player, AppliedCreate.MECHANICAL_CRAFT_ENCODER_BLOCK.get())
    }
}
