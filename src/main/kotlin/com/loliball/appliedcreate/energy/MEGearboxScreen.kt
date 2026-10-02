package com.loliball.appliedcreate.energy

import appeng.client.gui.AEBaseScreen
import appeng.client.gui.style.ScreenStyle
import appeng.client.gui.widgets.AETextField
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class MEGearboxScreen(
    menu: MEGearboxMenu,
    playerInventory: Inventory,
    title: Component,
    style: ScreenStyle
) : AEBaseScreen<MEGearboxMenu>(menu, playerInventory, title, style) {

    private val modeToggle: Button
    private val speedInput: AETextField

    init {
        modeToggle = widgets.addButton("modeToggle", getModeText(menu.currentMode), Runnable { menu.requestToggleMode() })

        speedInput = widgets.addTextField("speedInput")
        speedInput.setMaxLength(7)
        speedInput.setValue(menu.currentConfiguredSpeed.toString())
        speedInput.setResponder { text -> onSpeedChanged(text) }

    }

    private fun onSpeedChanged(text: String) {
        if (menu.currentMode != MEGearboxBlockEntity.Mode.EXPORT) return
        val value = text.toIntOrNull() ?: return
        val maxSpeed = MEGearboxBlockEntity.getMaxSpeed()
        if (value in -maxSpeed..maxSpeed && value != 0) {
            menu.requestSetSpeed(value)
        }
    }

    override fun updateBeforeRender() {
        super.updateBeforeRender()
        modeToggle.message = getModeText(menu.currentMode)
        val sending = menu.currentMode == MEGearboxBlockEntity.Mode.EXPORT
        speedInput.visible = sending
        speedInput.setEditable(sending)

        val serverSpeed = menu.currentConfiguredSpeed.toString()
        if (!speedInput.isFocused && speedInput.value != serverSpeed) {
            speedInput.setValue(serverSpeed)
        }
    }

    override fun drawFG(guiGraphics: GuiGraphics, offsetX: Int, offsetY: Int, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(
            font,
            Component.translatable("gui.appliedcreate.MEGearbox"),
            8, 6, 0x404040, false
        )

        guiGraphics.drawString(
            font,
            Component.translatable(if (menu.currentMode == MEGearboxBlockEntity.Mode.EXPORT)
                "gui.appliedcreate.MEGearbox.speed.label" else "gui.appliedcreate.MEGearbox.automatic_import"),
            8, 48, 0x404040, false
        )

        guiGraphics.drawString(
            font,
            Component.translatable(if (menu.currentActive) "gui.appliedcreate.MEGearbox.automatic_stress"
                else "gui.appliedcreate.MEGearbox.offline"),
            8, 70, 0x404040, false
        )

        val actualSpeedText = Component.translatable(
            "gui.appliedcreate.MEGearbox.actual_speed",
            String.format("%.1f", menu.currentSpeed)
        )
        guiGraphics.drawString(font, actualSpeedText, 8, 96, 0x404040, false)

        val actualStressText = Component.translatable(
            "gui.appliedcreate.MEGearbox.actual_stress",
            String.format("%.1f", menu.currentStress)
        )
        guiGraphics.drawString(font, actualStressText, 8, 108, 0x404040, false)
        guiGraphics.drawString(font, Component.translatable("gui.appliedcreate.MEGearbox.transfer",
            menu.currentTransferRate), 8, 120, 0x404040, false)
    }

    private fun getModeText(mode: MEGearboxBlockEntity.Mode): Component {
        val modeKey = when (mode) {
            MEGearboxBlockEntity.Mode.EXPORT -> "gui.appliedcreate.MEGearbox.mode.export"
            MEGearboxBlockEntity.Mode.IMPORT -> "gui.appliedcreate.MEGearbox.mode.import"
        }
        return Component.translatable(modeKey)
    }
}
