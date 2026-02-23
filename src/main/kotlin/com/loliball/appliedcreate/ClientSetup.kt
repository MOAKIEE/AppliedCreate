package com.loliball.appliedcreate

import com.loliball.appliedcreate.gui.AndesitePatternProviderScreen
import com.loliball.appliedcreate.gui.BrassPatternProviderScreen
import com.loliball.appliedcreate.gui.MechanicalCraftEncoderMenu
import com.loliball.appliedcreate.gui.MechanicalCraftEncoderScreen
import appeng.init.client.InitScreens
import net.minecraft.client.gui.screens.MenuScreens
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
                AppliedCreate.ANDESITE_PATTERN_PROVIDER_MENU.get(),
                ::AndesitePatternProviderScreen,
                "/screens/appliedcreate/andesite_pattern_provider.json"
            )
            InitScreens.register(
                AppliedCreate.BRASS_PATTERN_PROVIDER_MENU.get(),
                ::BrassPatternProviderScreen,
                "/screens/appliedcreate/brass_pattern_provider.json"
            )
        }
    }
}
