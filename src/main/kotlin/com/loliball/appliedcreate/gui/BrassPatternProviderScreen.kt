package com.loliball.appliedcreate.gui

import appeng.client.gui.implementations.PatternProviderScreen
import appeng.client.gui.style.ScreenStyle
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class BrassPatternProviderScreen(
    menu: BrassPatternProviderMenu,
    playerInventory: Inventory,
    title: Component,
    style: ScreenStyle
) : PatternProviderScreen<BrassPatternProviderMenu>(menu, playerInventory, title, style)