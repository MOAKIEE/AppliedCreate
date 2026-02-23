package com.loliball.appliedcreate

import com.loliball.appliedcreate.gui.AndesitePatternProviderMenu
import com.loliball.appliedcreate.gui.AndesitePatternProviderPartMenu
import com.loliball.appliedcreate.gui.AndesitePatternProviderPartScreen
import com.loliball.appliedcreate.gui.AndesitePatternProviderScreen
import com.loliball.appliedcreate.gui.BrassPatternProviderMenu
import com.loliball.appliedcreate.gui.BrassPatternProviderPartMenu
import com.loliball.appliedcreate.gui.BrassPatternProviderPartScreen
import com.loliball.appliedcreate.gui.BrassPatternProviderScreen
import com.loliball.appliedcreate.gui.MechanicalCraftEncoderMenu
import com.loliball.appliedcreate.gui.MechanicalCraftEncoderScreen
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
            MenuScreens.register(AppliedCreate.BRASS_PATTERN_PROVIDER_MENU.get()) { menu: BrassPatternProviderMenu, inv, title ->
                BrassPatternProviderScreen(menu, inv, title)
            }
            MenuScreens.register(AppliedCreate.ANDESITE_PATTERN_PROVIDER_MENU.get()) { menu: AndesitePatternProviderMenu, inv, title ->
                AndesitePatternProviderScreen(menu, inv, title)
            }
            MenuScreens.register(AppliedCreate.ANDESITE_PATTERN_PROVIDER_PART_MENU.get()) { menu: AndesitePatternProviderPartMenu, inv, title ->
                AndesitePatternProviderPartScreen(menu, inv, title)
            }
            MenuScreens.register(AppliedCreate.BRASS_PATTERN_PROVIDER_PART_MENU.get()) { menu: BrassPatternProviderPartMenu, inv, title ->
                BrassPatternProviderPartScreen(menu, inv, title)
            }
        }
        AppliedCreate.LOGGER.info("Applied Create client setup - screens registered")
    }
}
