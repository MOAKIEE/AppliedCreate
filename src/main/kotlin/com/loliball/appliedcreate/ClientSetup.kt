package com.loliball.appliedcreate

import com.loliball.appliedcreate.client.StressKeyRenderHandler
import com.loliball.appliedcreate.gui.BrassPatternProviderMenu
import com.loliball.appliedcreate.cannon.MEBlueprintCannonMenu
import com.loliball.appliedcreate.cannon.MEBlueprintCannonRenderer
import com.loliball.appliedcreate.energy.KineticEnergyAcceptorRenderer
import com.loliball.appliedcreate.energy.MEGearboxRenderer
import com.loliball.appliedcreate.kinetic.StressP2PCompanionRenderer
import com.loliball.appliedcreate.cannon.MEBlueprintCannonScreen
import com.loliball.appliedcreate.storage.StressStorageCell

import appeng.client.gui.style.ScreenStyle
import appeng.client.gui.implementations.PatternProviderScreen
import appeng.init.client.InitScreens

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
@OnlyIn(Dist.CLIENT)
object ClientSetup {

    fun register(bus: IEventBus) {
        bus.addListener(::onRegisterMenuScreens)
        bus.addListener(::onRegisterItemColors)
        bus.addListener(::onRegisterRenderers)
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
        event.register(
            AppliedCreate.ME_BLUEPRINT_CANNON_MENU.get(),
            ::MEBlueprintCannonScreen
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

    private fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(AppliedCreate.ME_BLUEPRINT_CANNON_BE.get(), ::MEBlueprintCannonRenderer)
        event.registerBlockEntityRenderer(AppliedCreate.KINETIC_ENERGY_ACCEPTOR_BE.get(), ::KineticEnergyAcceptorRenderer)
        event.registerBlockEntityRenderer(AppliedCreate.ME_GEARBOX_BE.get(), ::MEGearboxRenderer)
        event.registerBlockEntityRenderer(AppliedCreate.STRESS_P2P_COMPANION_BE.get(), ::StressP2PCompanionRenderer)
    }
}