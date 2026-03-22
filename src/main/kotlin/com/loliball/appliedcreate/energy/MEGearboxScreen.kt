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
    private val stressInput: AETextField

    init {
        modeToggle = widgets.addButton("modeToggle", getModeText(menu.currentMode), Runnable { menu.requestToggleMode() })

        speedInput = widgets.addTextField("speedInput")
        speedInput.setMaxLength(7)
        speedInput.setValue(menu.currentConfiguredSpeed.toString())
        speedInput.setResponder { text -> onSpeedChanged(text) }

        stressInput = widgets.addTextField("stressInput")
        stressInput.setMaxLength(5)
        stressInput.setValue(menu.currentConfiguredStress.toString())
        stressInput.setResponder { text -> onStressChanged(text) }
    }

    private fun onSpeedChanged(text: String) {
        val value = text.toIntOrNull() ?: return
        val maxSpeed = MEGearboxBlockEntity.getMaxSpeed()
        if (value in -maxSpeed..maxSpeed && value != 0) {
            menu.requestSetSpeed(value)
        }
    }

    private fun onStressChanged(text: String) {
        val value = text.toIntOrNull() ?: return
        if (value in MEGearboxBlockEntity.MIN_STRESS.toInt()..MEGearboxBlockEntity.getMaxStress().toInt()) {
            menu.requestSetStress(value)
        }
    }

    override fun updateBeforeRender() {
        super.updateBeforeRender()
        modeToggle.message = getModeText(menu.currentMode)

        val serverSpeed = menu.currentConfiguredSpeed.toString()
        if (!speedInput.isFocused && speedInput.value != serverSpeed) {
            speedInput.setValue(serverSpeed)
        }
        val serverStress = menu.currentConfiguredStress.toString()
        if (!stressInput.isFocused && stressInput.value != serverStress) {
            stressInput.setValue(serverStress)
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
            Component.translatable("gui.appliedcreate.MEGearbox.speed.label"),
            8, 48, 0x404040, false
        )

        guiGraphics.drawString(
            font,
            Component.translatable("gui.appliedcreate.MEGearbox.stress.label"),
            8, 72, 0x404040, false
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
    }

    private fun getModeText(mode: MEGearboxBlockEntity.Mode): Component {
        val modeKey = when (mode) {
            MEGearboxBlockEntity.Mode.EXPORT -> "gui.appliedcreate.MEGearbox.mode.export"
            MEGearboxBlockEntity.Mode.IMPORT -> "gui.appliedcreate.MEGearbox.mode.import"
        }
        return Component.translatable(modeKey)
    }
}
