package com.loliball.appliedcreate

import com.loliball.appliedcreate.gui.BrassPatternProviderMenu
import appeng.client.gui.implementations.PatternProviderScreen
import appeng.client.gui.style.ScreenStyle
import appeng.init.client.InitScreens
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent

@OnlyIn(Dist.CLIENT)
object ClientSetup {

    fun register(bus: IEventBus) {
        bus.addListener(::onRegisterMenuScreens)
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
    }
}
