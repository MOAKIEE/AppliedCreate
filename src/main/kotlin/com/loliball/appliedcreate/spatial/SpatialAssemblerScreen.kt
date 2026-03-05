package com.loliball.appliedcreate.spatial

import com.simibubi.create.foundation.gui.AllGuiTextures
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class SpatialAssemblerScreen(menu: SpatialAssemblerMenu, inventory: Inventory, title: Component) :
    AbstractSimiContainerScreen<SpatialAssemblerMenu>(menu, inventory, title) {

    companion object {
        // GUI dimensions (simple layout: 176x166 standard container)
        private const val GUI_WIDTH = 176
        private const val GUI_HEIGHT = 166

        // Slot positions (must match Menu)
        private const val INPUT_SLOT_X = 53
        private const val INPUT_SLOT_Y = 35
        private const val OUTPUT_SLOT_X = 107
        private const val OUTPUT_SLOT_Y = 35

        // Colors
        private const val TITLE_COLOR = 0x404040
        private const val STATUS_COLOR = 0xDDEEFF
        private const val SLOT_BG = 0xFF8B8B8B.toInt()
        private const val SLOT_BORDER_DARK = 0xFF373737.toInt()
        private const val SLOT_BORDER_LIGHT = 0xFFFFFFFF.toInt()
        private const val BG_COLOR = 0xFFC6C6C6.toInt()
        private const val BORDER_DARK = 0xFF555555.toInt()
    }

    override fun init() {
        setWindowSize(GUI_WIDTH, GUI_HEIGHT)
        super.init()
    }

    override fun renderBg(graphics: GuiGraphics, partialTicks: Float, mouseX: Int, mouseY: Int) {
        val x = leftPos
        val y = topPos

        // Main background panel
        renderPanel(graphics, x, y, GUI_WIDTH, GUI_HEIGHT - 83)

        // Player inventory
        val invY = y + GUI_HEIGHT - 83
        renderPlayerInventory(graphics, x, invY)

        // Title
        graphics.drawString(font, title, x + (GUI_WIDTH - font.width(title)) / 2, y + 6, TITLE_COLOR, false)

        // Arrow between input and output slots
        renderArrow(graphics, x + 76, y + 35)

        // Slot backgrounds
        renderSlot(graphics, x + INPUT_SLOT_X - 1, y + INPUT_SLOT_Y - 1)
        renderSlot(graphics, x + OUTPUT_SLOT_X - 1, y + OUTPUT_SLOT_Y - 1)

        // Labels
        val inputLabel = Component.translatable("gui.appliedcreate.spatial_assembler.input")
        val outputLabel = Component.translatable("gui.appliedcreate.spatial_assembler.output")
        graphics.drawString(font, inputLabel, x + INPUT_SLOT_X + 8 - font.width(inputLabel) / 2, y + 24, TITLE_COLOR, false)
        graphics.drawString(font, outputLabel, x + OUTPUT_SLOT_X + 8 - font.width(outputLabel) / 2, y + 24, TITLE_COLOR, false)

        // Status message
        val be = menu.contentHolder ?: return
        val statusKey = "gui.appliedcreate.spatial_assembler.status.${be.statusMsg}"
        val statusText = Component.translatable(statusKey)
        graphics.drawString(font, statusText, x + GUI_WIDTH / 2 - font.width(statusText) / 2, y + 58, STATUS_COLOR)
    }

    override fun renderForeground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.renderForeground(graphics, mouseX, mouseY, partialTicks)
    }

    private fun renderPanel(graphics: GuiGraphics, x: Int, y: Int, w: Int, h: Int) {
        // Fill
        graphics.fill(x, y, x + w, y + h, BG_COLOR)
        // Outer dark border
        graphics.fill(x, y, x + w, y + 1, SLOT_BORDER_DARK)
        graphics.fill(x, y, x + 1, y + h, SLOT_BORDER_DARK)
        graphics.fill(x + w - 1, y, x + w, y + h, SLOT_BORDER_DARK)
        graphics.fill(x, y + h - 1, x + w, y + h, SLOT_BORDER_DARK)
        // 3D inner border
        graphics.fill(x + 1, y + 1, x + w - 1, y + 2, SLOT_BORDER_LIGHT)
        graphics.fill(x + 1, y + 1, x + 2, y + h - 1, SLOT_BORDER_LIGHT)
        graphics.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, BORDER_DARK)
        graphics.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BORDER_DARK)
    }

    private fun renderSlot(graphics: GuiGraphics, x: Int, y: Int) {
        val s = 18
        graphics.fill(x, y, x + s, y + s, SLOT_BG)
        graphics.fill(x, y, x + s, y + 1, SLOT_BORDER_DARK)
        graphics.fill(x, y, x + 1, y + s, SLOT_BORDER_DARK)
        graphics.fill(x, y + s - 1, x + s, y + s, SLOT_BORDER_LIGHT)
        graphics.fill(x + s - 1, y, x + s, y + s, SLOT_BORDER_LIGHT)
    }

    private fun renderArrow(graphics: GuiGraphics, x: Int, y: Int) {
        // Simple arrow: a line with a head
        val arrowColor = 0xFF404040.toInt()
        // Horizontal line
        graphics.fill(x, y + 7, x + 24, y + 9, arrowColor)
        // Arrow head
        graphics.fill(x + 20, y + 5, x + 24, y + 11, arrowColor)
        graphics.fill(x + 24, y + 6, x + 27, y + 10, arrowColor)
        graphics.fill(x + 27, y + 7, x + 29, y + 9, arrowColor)
    }
}
