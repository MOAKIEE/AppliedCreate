package com.loliball.appliedcreate.part

import appeng.api.parts.IPartItem
import appeng.api.stacks.AEItemKey
import appeng.helpers.patternprovider.PatternProviderLogic
import appeng.menu.ISubMenu
import appeng.menu.MenuOpener
import appeng.menu.locator.MenuLocator
import appeng.parts.crafting.PatternProviderPart
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.logic.MechanicalCraftingPatternLogic
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

    companion object {
        const val PATTERN_SLOTS = 9
    }
}
