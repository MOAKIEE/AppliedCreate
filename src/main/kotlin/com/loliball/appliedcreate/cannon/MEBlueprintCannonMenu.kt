package com.loliball.appliedcreate.cannon

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.foundation.gui.menu.MenuBase
import net.minecraft.client.Minecraft
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.SlotItemHandler

class MEBlueprintCannonMenu : MenuBase<MEBlueprintCannonBlockEntity> {

    constructor(type: MenuType<*>, id: Int, inv: Inventory, buffer: RegistryFriendlyByteBuf) : super(type, id, inv, buffer)

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
        // Match SchematicannonMenu slot positions (relative to background origin)
        // 0: Blueprint input at (15, 65)
        addSlot(SlotItemHandler(contentHolder.inventory, 0, 15, 65))
        // 1: Output at (171, 65)
        addSlot(SlotItemHandler(contentHolder.inventory, 1, 171, 65))
        // 2-6: Upgrade slots on right side panel
        val upgradeHandler = contentHolder.upgradeInventory.toItemHandler()
        for (i in 0 until 5) {
            addSlot(SlotItemHandler(upgradeHandler, i, 221, 7 + i * 18))
        }
        // Player inventory at (37, 161) — matching Schematicannon
        addPlayerSlots(37, 161)
    }

    override fun saveData(contentHolder: MEBlueprintCannonBlockEntity) {}

    override fun createOnClient(extraData: RegistryFriendlyByteBuf): MEBlueprintCannonBlockEntity? {
        val level = Minecraft.getInstance().level ?: return null
        val pos = extraData.readBlockPos()
        val be = level.getBlockEntity(pos)
        if (be is MEBlueprintCannonBlockEntity) {
            be.readClient(extraData.readNbt(), extraData.registryAccess())
            return be
        }
        return null
    }

    override fun quickMoveStack(playerIn: Player, index: Int): ItemStack {
        val clickedSlot = getSlot(index)
        if (!clickedSlot.hasItem()) return ItemStack.EMPTY
        val stack = clickedSlot.item

        if (index < 7) {
            // Container slots (0-1 inventory, 2-6 upgrades) -> player inventory
            moveItemStackTo(stack, 7, slots.size, true)
        } else {
            // Player inventory -> try blueprint slot first, then upgrade slots
            if (!moveItemStackTo(stack, 0, 1, false)) {
                moveItemStackTo(stack, 2, 7, false)
            }
        }

        return ItemStack.EMPTY
    }
}
