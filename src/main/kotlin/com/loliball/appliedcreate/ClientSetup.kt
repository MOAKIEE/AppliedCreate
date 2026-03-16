package com.loliball.appliedcreate

import com.loliball.appliedcreate.client.StressKeyRenderHandler
import com.loliball.appliedcreate.gui.BrassPatternProviderMenu
import com.loliball.appliedcreate.cannon.MEBlueprintCannonMenu
import com.loliball.appliedcreate.cannon.MEBlueprintCannonRenderer
import com.loliball.appliedcreate.cannon.MEBlueprintCannonScreen
import com.loliball.appliedcreate.energy.KineticEnergyAcceptorRenderer
import com.loliball.appliedcreate.energy.MEGearboxMenu
import com.loliball.appliedcreate.energy.MEGearboxRenderer
import com.loliball.appliedcreate.energy.MEGearboxScreen
import com.loliball.appliedcreate.storage.StressStorageCell
import appeng.client.gui.implementations.PatternProviderScreen
import appeng.client.gui.style.ScreenStyle
import appeng.init.client.InitScreens
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.client.event.RegisterColorHandlersEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

@OnlyIn(Dist.CLIENT)
object ClientSetup {

    fun register(bus: IEventBus) {
        bus.addListener(::onClientSetup)
        bus.addListener(::onRegisterItemColors)
        bus.addListener(::onRegisterRenderers)
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            InitScreens.register(
                AppliedCreate.BRASS_PATTERN_PROVIDER_MENU.get(),
                { menu: BrassPatternProviderMenu, inv: Inventory, title: Component, style: ScreenStyle ->
                    PatternProviderScreen(menu, inv, title, style)
                },
                "/screens/appliedcreate/brass_pattern_provider.json"
            )

            MenuScreens.register(
                AppliedCreate.ME_BLUEPRINT_CANNON_MENU.get(),
                ::MEBlueprintCannonScreen
            )

            InitScreens.register(
                AppliedCreate.ME_GEARBOX_MENU.get(),
                { menu: MEGearboxMenu, inv: Inventory, title: Component, style: ScreenStyle ->
                    MEGearboxScreen(menu, inv, title, style)
                },
                "/screens/appliedcreate/me_gearbox.json"
            )

            StressKeyRenderHandler.register()
        }
    }

    private fun onRegisterItemColors(event: RegisterColorHandlersEvent.Item) {
        // Register color handler for stress storage cells (LED tint layer)
        val cellItems = AppliedCreate.STRESS_CELLS.map { it.get() }.toTypedArray()
        event.register(
            { stack, tintIndex -> StressStorageCell.getColor(stack, tintIndex) },
            *cellItems
        )
    }

    private fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(AppliedCreate.ME_BLUEPRINT_CANNON_BE.get(), ::MEBlueprintCannonRenderer)
        event.registerBlockEntityRenderer(AppliedCreate.KINETIC_ENERGY_ACCEPTOR_BE.get(), ::KineticEnergyAcceptorRenderer)
        event.registerBlockEntityRenderer(AppliedCreate.ME_GEARBOX_BE.get(), ::MEGearboxRenderer)
    }
}
