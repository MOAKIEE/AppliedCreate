package com.loliball.appliedcreate.part

import appeng.api.parts.IPartItem
import appeng.api.parts.IPartModel
import appeng.api.stacks.AEItemKey
import appeng.core.AppEng
import appeng.helpers.patternprovider.PatternProviderLogic
import appeng.items.parts.PartModels
import appeng.menu.ISubMenu
import appeng.menu.MenuOpener
import appeng.menu.locator.MenuLocator
import appeng.parts.PartModel
import appeng.parts.crafting.PatternProviderPart
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.logic.MechanicalCraftingPatternLogic
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

class AndesitePatternProviderPart(partItem: IPartItem<*>) : PatternProviderPart(partItem) {

    override fun createLogic(): PatternProviderLogic {
        return MechanicalCraftingPatternLogic(this.mainNode, this, PATTERN_SLOTS)
    }

    override fun openMenu(player: Player, locator: MenuLocator) {
        MenuOpener.open(appeng.menu.implementations.PatternProviderMenu.TYPE, player, locator)
    }
    override fun returnToMainMenu(player: Player, subMenu: ISubMenu) {
        MenuOpener.returnTo(appeng.menu.implementations.PatternProviderMenu.TYPE, player, subMenu.locator)
    }

    override fun getTerminalIcon(): AEItemKey {
        return AEItemKey.of(AppliedCreate.ANDESITE_PATTERN_PROVIDER_PART_ITEM.get())
    }

    override fun getMainMenuIcon(): ItemStack {
        return AppliedCreate.ANDESITE_PATTERN_PROVIDER_PART_ITEM.get().defaultInstance
    }

    override fun getStaticModels(): IPartModel {
        return if (isActive && isPowered) {
            ANDESITE_MODELS_HAS_CHANNEL
        } else if (isPowered) {
            ANDESITE_MODELS_ON
        } else {
            ANDESITE_MODELS_OFF
        }
    }

    companion object {
        const val PATTERN_SLOTS = 9

        @JvmStatic
        val ANDESITE_MODEL_BASE = ResourceLocation(AppliedCreate.MOD_ID, "part/andesite_pattern_provider_base")

        @JvmStatic
        @PartModels
        val ANDESITE_MODELS_OFF = PartModel(
            ANDESITE_MODEL_BASE,
            ResourceLocation(AppEng.MOD_ID, "part/interface_off")
        )

        @JvmStatic
        @PartModels
        val ANDESITE_MODELS_ON = PartModel(
            ANDESITE_MODEL_BASE,
            ResourceLocation(AppEng.MOD_ID, "part/interface_on")
        )

        @JvmStatic
        @PartModels
        val ANDESITE_MODELS_HAS_CHANNEL = PartModel(
            ANDESITE_MODEL_BASE,
            ResourceLocation(AppEng.MOD_ID, "part/interface_has_channel")
        )
    }
}
