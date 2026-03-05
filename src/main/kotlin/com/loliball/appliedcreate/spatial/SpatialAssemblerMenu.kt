package com.loliball.appliedcreate.spatial

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.foundation.gui.menu.MenuBase
import net.minecraft.client.Minecraft
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.SlotItemHandler

class SpatialAssemblerMenu : MenuBase<SpatialAssemblerBlockEntity> {

    constructor(type: MenuType<*>, id: Int, inv: Inventory, buffer: RegistryFriendlyByteBuf) : super(type, id, inv, buffer)

    constructor(type: MenuType<*>, id: Int, inv: Inventory, be: SpatialAssemblerBlockEntity) : super(type, id, inv, be)

    constructor(id: Int, inv: Inventory, be: SpatialAssemblerBlockEntity) : this(
        AppliedCreate.SPATIAL_ASSEMBLER_MENU.get(),
        id,
        inv,
        be
    )

    override fun initAndReadInventory(contentHolder: SpatialAssemblerBlockEntity) {}

    override fun addSlots() {
        // Slot 0: Cell input (center of GUI)
        addSlot(SlotItemHandler(contentHolder.cellInventory, 0, 53, 35))
        // Slot 1: Cell output
        addSlot(SlotItemHandler(contentHolder.cellInventory, 1, 107, 35))
        // Player inventory
        addPlayerSlots(8, 84)
    }

    override fun saveData(contentHolder: SpatialAssemblerBlockEntity) {}

    override fun createOnClient(extraData: RegistryFriendlyByteBuf): SpatialAssemblerBlockEntity? {
        val level = Minecraft.getInstance().level ?: return null
        val pos = extraData.readBlockPos()
        val be = level.getBlockEntity(pos)
        if (be is SpatialAssemblerBlockEntity) {
            be.readClient(extraData.readNbt(), extraData.registryAccess())
            return be
        }
        return null
    }

    override fun quickMoveStack(playerIn: Player, index: Int): ItemStack {
        val clickedSlot = getSlot(index)
        if (!clickedSlot.hasItem()) return ItemStack.EMPTY
        val stack = clickedSlot.item

        if (index < 2) {
            // Container slots -> player inventory
            moveItemStackTo(stack, 2, slots.size, true)
        } else {
            // Player inventory -> try cell input slot
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY
            }
        }

        return ItemStack.EMPTY
    }
}
