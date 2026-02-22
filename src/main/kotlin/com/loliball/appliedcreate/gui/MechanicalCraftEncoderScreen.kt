package com.loliball.appliedcreate.gui

import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.block.entity.MechanicalCraftEncoderBlockEntity
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

@OnlyIn(Dist.CLIENT)
class MechanicalCraftEncoderScreen(
    menu: MechanicalCraftEncoderMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<MechanicalCraftEncoderMenu>(menu, playerInventory, title) {

    companion object {
        val TEXTURE = ResourceLocation(AppliedCreate.MOD_ID, "textures/gui/mechanical_craft_encoder.png")
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
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight)
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

@Mod.EventBusSubscriber(modid = AppliedCreate.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ScreenRegistration {
    @SubscribeEvent
    @JvmStatic
    fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            MenuScreens.register(AppliedCreate.MECHANICAL_CRAFT_ENCODER_MENU.get(), ::MechanicalCraftEncoderScreen)
        }
    }
}
