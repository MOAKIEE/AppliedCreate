package com.loliball.appliedcreate

import com.loliball.appliedcreate.client.StressKeyRenderHandler
import com.loliball.appliedcreate.gui.BrassPatternProviderMenu
import com.loliball.appliedcreate.storage.StressStorageCell
import appeng.client.gui.implementations.PatternProviderScreen
import appeng.client.gui.style.ScreenStyle
import appeng.init.client.InitScreens
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent

@OnlyIn(Dist.CLIENT)
object ClientSetup {

    fun register(bus: IEventBus) {
        bus.addListener(::onRegisterMenuScreens)
        bus.addListener(::onRegisterItemColors)
    }

    private fun onRegisterMenuScreens(event: RegisterMenuScreensEvent) {
        InitScreens.register(
            event,
            AppliedCreate.BRASS_PATTERN_PROVIDER_MENU.get(),
            { menu: BrassPatternProviderMenu, inv: Inventory, title: Component, style: ScreenStyle ->
                PatternProviderScreen(menu, inv, title, style)
            },
            "/screens/appliedcreate/brass_pattern_provider.json"
        )

        // Register StressKey render handler for ME terminal display
        StressKeyRenderHandler.register()
    }

    private fun onRegisterItemColors(event: RegisterColorHandlersEvent.Item) {
        // Register color handler for stress storage cells (LED tint layer)
        val cellItems = AppliedCreate.STRESS_CELLS.map { it.get() }.toTypedArray()
        event.register(
            { stack, tintIndex -> StressStorageCell.getColor(stack, tintIndex) },
            *cellItems
        )
    }
}