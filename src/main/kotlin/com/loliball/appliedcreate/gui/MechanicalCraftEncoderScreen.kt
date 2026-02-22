package com.loliball.appliedcreate.gui

import com.loliball.appliedcreate.AppliedCreate
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class MechanicalCraftEncoderScreen(
    menu: MechanicalCraftEncoderMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<MechanicalCraftEncoderMenu>(menu, playerInventory, title) {

    companion object {
        val TEXTURE = ResourceLocation(AppliedCreate.MOD_ID, "textures/gui/mechanical_craft_encoder.png")
        const val TEXTURE_WIDTH = 256
        const val TEXTURE_HEIGHT = 512
    }

    private lateinit var widthField: EditBox

    init {
        imageWidth = 198
        imageHeight = 266
        inventoryLabelY = imageHeight - 94
    }

    override fun init() {
        super.init()

        widthField = EditBox(font, leftPos + 174, topPos + 90, 18, 14, Component.empty())
        widthField.setMaxLength(1)
        widthField.value = menu.blockEntity.minWidth.toString()
        widthField.setFilter { text -> text.isEmpty() || (text.length == 1 && text[0] in '1'..'9') }
        widthField.setResponder { text ->
            val value = text.toIntOrNull()
            if (value != null && value in 1..9) {
                menu.blockEntity.minWidth = value
                menu.blockEntity.setChanged()
            }
        }
        addRenderableWidget(widthField)
    }

    override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        graphics.blit(TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT)
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics)
        super.render(graphics, mouseX, mouseY, partialTick)
        renderTooltip(graphics, mouseX, mouseY)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (widthField.isFocused) {
            if (keyCode == 256) {
                widthField.isFocused = false
                return true
            }
            return widthField.keyPressed(keyCode, scanCode, modifiers)
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}
