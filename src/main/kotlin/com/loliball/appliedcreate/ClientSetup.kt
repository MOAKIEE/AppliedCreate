package com.loliball.appliedcreate

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
        }
        AppliedCreate.LOGGER.info("Applied Create client setup - screen registered")
    }
}
