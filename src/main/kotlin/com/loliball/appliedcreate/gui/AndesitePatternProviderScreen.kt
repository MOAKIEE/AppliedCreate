package com.loliball.appliedcreate.gui

import com.loliball.appliedcreate.AppliedCreate
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class AndesitePatternProviderScreen(
    menu: AndesitePatternProviderMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<AndesitePatternProviderMenu>(menu, playerInventory, title) {

    companion object {
        private val TEXTURE = ResourceLocation(AppliedCreate.MOD_ID, "textures/gui/andesite_pattern_provider.png")
    }

    init {
        imageWidth = 176
        imageHeight = 166
        inventoryLabelY = imageHeight - 94
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight)
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 4210752, false)
        guiGraphics.drawString(font, Component.translatable("gui.appliedcreate.patterns"), 8, 21, 4210752, false)
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false)
    }
}