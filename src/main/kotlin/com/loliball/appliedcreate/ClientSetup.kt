package com.loliball.appliedcreate

import com.loliball.appliedcreate.gui.BrassPatternProviderMenu
import com.loliball.appliedcreate.gui.MechanicalCraftEncoderMenu
import com.loliball.appliedcreate.gui.MechanicalCraftEncoderScreen
import appeng.client.gui.implementations.PatternProviderScreen
import appeng.client.gui.style.ScreenStyle
import appeng.init.client.InitScreens
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

@OnlyIn(Dist.CLIENT)
object ClientSetup {

    fun register(bus: IEventBus) {
        bus.addListener(::onClientSetup)
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            MenuScreens.register(AppliedCreate.MECHANICAL_CRAFT_ENCODER_MENU.get()) { menu: MechanicalCraftEncoderMenu, inv, title ->
                MechanicalCraftEncoderScreen(menu, inv, title)
            }

            InitScreens.register(
                AppliedCreate.BRASS_PATTERN_PROVIDER_MENU.get(),
                { menu: BrassPatternProviderMenu, inv: Inventory, title: Component, style: ScreenStyle ->
                    PatternProviderScreen(menu, inv, title, style)
                },
                "/screens/appliedcreate/brass_pattern_provider.json"
            )
        }
    }
}
