package com.loliball.appliedcreate.gui

import appeng.helpers.patternprovider.PatternProviderLogicHost
import appeng.menu.implementations.PatternProviderMenu
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType

class AndesitePatternProviderMenu(
    menuType: MenuType<out PatternProviderMenu>,
    id: Int,
    playerInventory: Inventory,
    host: PatternProviderLogicHost
) : PatternProviderMenu(menuType, id, playerInventory, host)
