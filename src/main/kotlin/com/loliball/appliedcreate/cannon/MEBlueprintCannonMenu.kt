package com.loliball.appliedcreate.cannon

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.foundation.gui.menu.MenuBase
import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.SlotItemHandler

class MEBlueprintCannonMenu : MenuBase<MEBlueprintCannonBlockEntity> {

    constructor(type: MenuType<*>, id: Int, inv: Inventory, buffer: FriendlyByteBuf) : super(type, id, inv, buffer)

    constructor(type: MenuType<*>, id: Int, inv: Inventory, be: MEBlueprintCannonBlockEntity) : super(
        type, id, inv, be
    )

    constructor(id: Int, inv: Inventory, be: MEBlueprintCannonBlockEntity) : this(
        AppliedCreate.ME_BLUEPRINT_CANNON_MENU.get(),
        id,
        inv,
        be
    )

    override fun initAndReadInventory(contentHolder: MEBlueprintCannonBlockEntity) {}

    override fun addSlots() {
        val x = 0
        val y = 0

        // 0: Blueprint
        addSlot(SlotItemHandler(contentHolder.inventory, 0, x + 15, y + 65))
        // 1: Blueprint Output
        addSlot(SlotItemHandler(contentHolder.inventory, 1, x + 171, y + 65))

        // 2-6: Upgrade Card Slots
        val upgradeHandler = contentHolder.upgradeInventory.toItemHandler()
        for (i in 0 until 5) {
            addSlot(SlotItemHandler(upgradeHandler, i, x + 220, y + 7 + i * 18))
        }

        addPlayerSlots(37, 161)
    }

    override fun saveData(contentHolder: MEBlueprintCannonBlockEntity) {}

    override fun createOnClient(extraData: FriendlyByteBuf): MEBlueprintCannonBlockEntity? {
        val level = Minecraft.getInstance().level ?: return null
        val pos = extraData.readBlockPos()
        val be = level.getBlockEntity(pos)
        if (be is MEBlueprintCannonBlockEntity) {
            be.readClient(extraData.readNbt())
            return be
        }
        return null
    }

    override fun quickMoveStack(playerIn: Player, index: Int): ItemStack {
        val clickedSlot = getSlot(index)
        if (!clickedSlot.hasItem()) return ItemStack.EMPTY
        val stack = clickedSlot.item

        if (index < 7) {
            moveItemStackTo(stack, 7, slots.size, true)
        } else {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                moveItemStackTo(stack, 2, 7, false)
            }
        }

        return ItemStack.EMPTY
    }
}
