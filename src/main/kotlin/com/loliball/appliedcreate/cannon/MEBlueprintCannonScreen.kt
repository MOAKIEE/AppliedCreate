package com.loliball.appliedcreate.cannon

import com.loliball.appliedcreate.AppliedCreate
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity
import com.simibubi.create.foundation.gui.AllGuiTextures
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.Indicator
import net.createmod.catnip.lang.FontHelper
import net.createmod.catnip.lang.LangBuilder
import net.createmod.catnip.gui.element.GuiGameElement
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import java.util.*

class MEBlueprintCannonScreen(menu: MEBlueprintCannonMenu, inventory: Inventory, title: Component) :
    AbstractSimiContainerScreen<MEBlueprintCannonMenu>(menu, inventory, title) {

    companion object {
        private val GUI_TEXTURE = ResourceLocation(AppliedCreate.MOD_ID, "textures/gui/me_blueprint_cannon.png")
        private const val TEX_SIZE = 256

        private const val BG_W = 213
        private const val BG_H = 143

        private const val PROGRESS_U = 0
        private const val PROGRESS_V = 150
        private const val PROGRESS_W = 114
        private const val PROGRESS_H = 16

        private const val HIGHLIGHT_U = 0
        private const val HIGHLIGHT_V = 170
        private const val HIGHLIGHT_W = 26
        private const val HIGHLIGHT_H = 26

        private const val TITLE_U = 0
        private const val TITLE_V = 200
        private const val TITLE_W = 205
        private const val TITLE_H = 15
    }

    private val _showSettings = "gui.schematicannon.showOptions"
    private val _slotSchematic = "gui.schematicannon.slot.schematic"
    private val _slotUpgrades = "gui.appliedcreate.slot.upgrades"

    private val optionEnabled = Component.translatable("create.gui.schematicannon.optionEnabled")
    private val optionDisabled = Component.translatable("create.gui.schematicannon.optionDisabled")

    private var replaceLevelIndicators = Vector<Indicator>()
    private var replaceLevelButtons = Vector<IconButton>()

    private lateinit var skipMissingButton: IconButton
    private lateinit var skipMissingIndicator: Indicator
    private lateinit var skipBlockEntitiesButton: IconButton
    private lateinit var skipBlockEntitiesIndicator: Indicator

    private lateinit var playButton: IconButton
    private lateinit var playIndicator: Indicator
    private lateinit var pauseButton: IconButton
    private lateinit var pauseIndicator: Indicator
    private lateinit var resetButton: IconButton
    private lateinit var resetIndicator: Indicator

    private lateinit var confirmButton: IconButton
    private lateinit var showSettingsButton: IconButton
    private lateinit var showSettingsIndicator: Indicator
    private lateinit var viewMaterialsButton: IconButton

    private var placementSettingWidgets = ArrayList<AbstractWidget>()
    private var extraAreas: List<Rect2i> = Collections.emptyList()

    private val UPGRADE_SLOT_SIZE = 18
    private val UPGRADE_PADDING = 7
    private val UPGRADE_SLOTS = 5

    private var showingMaterials = false

    override fun init() {
        setWindowSize(BG_W, BG_H + 2 + AllGuiTextures.PLAYER_INVENTORY.height)
        setWindowOffset(-11, 0)
        super.init()

        val x = leftPos
        val y = topPos

        playButton = IconButton(x + 75, y + 85, AllIcons.I_PLAY)
        playButton.withCallback<IconButton>(Runnable { sendOptionUpdate(ConfigureMECannonPacket.Option.PLAY, true) })
        playIndicator = Indicator(x + 75, y + 79, CommonComponents.EMPTY)

        pauseButton = IconButton(x + 93, y + 85, AllIcons.I_PAUSE)
        pauseButton.withCallback<IconButton>(Runnable { sendOptionUpdate(ConfigureMECannonPacket.Option.PAUSE, true) })
        pauseIndicator = Indicator(x + 93, y + 79, CommonComponents.EMPTY)

        resetButton = IconButton(x + 111, y + 85, AllIcons.I_STOP)
        resetButton.withCallback<IconButton>(Runnable { sendOptionUpdate(ConfigureMECannonPacket.Option.STOP, true) })
        resetIndicator = Indicator(x + 111, y + 79, CommonComponents.EMPTY)
        resetIndicator.state = Indicator.State.RED

        addRenderableWidgets(playButton, playIndicator, pauseButton, pauseIndicator, resetButton, resetIndicator)

        confirmButton = IconButton(x + 180, y + 111, AllIcons.I_CONFIRM)
        confirmButton.withCallback<IconButton>(Runnable { minecraft!!.player!!.closeContainer() })
        addRenderableWidget(confirmButton)

        showSettingsButton = IconButton(x + 8, y + 111, AllIcons.I_PLACEMENT_SETTINGS)
        showSettingsButton.withCallback<IconButton>(Runnable {
            showSettingsIndicator.state = if (placementSettingsHidden()) Indicator.State.GREEN else Indicator.State.OFF
            initPlacementSettings()
        })
        showSettingsButton.setToolTip(Component.translatable("create.$_showSettings"))
        addRenderableWidget(showSettingsButton)

        showSettingsIndicator = Indicator(x + 9, y + 111, CommonComponents.EMPTY)

        viewMaterialsButton = IconButton(x + 134, y + 19, AllIcons.I_VIEW_SCHEDULE)
        viewMaterialsButton.withCallback<IconButton>(Runnable {
            showingMaterials = !showingMaterials
            if (showingMaterials) {
                menu.contentHolder.dontUpdateChecklist = false
            }
        })
        viewMaterialsButton.setToolTip(Component.translatable("gui.appliedcreate.me_blueprint_cannon.viewMaterials"))
        addRenderableWidget(viewMaterialsButton)

        val upgradePanelX = x + BG_W
        val upgradePanelY = y
        val upgradePanelW = UPGRADE_PADDING * 2 + UPGRADE_SLOT_SIZE
        val upgradePanelH = UPGRADE_PADDING * 2 + UPGRADE_SLOTS * UPGRADE_SLOT_SIZE
        extraAreas = listOf(
            Rect2i(x + BG_W, y + BG_H - 62, 84, 92),
            Rect2i(upgradePanelX, upgradePanelY, upgradePanelW, upgradePanelH)
        )
        tick()
    }

    private fun initPlacementSettings() {
        removeWidgets(placementSettingWidgets)
        placementSettingWidgets.clear()

        if (placementSettingsHidden()) return

        val x = leftPos
        val y = topPos

        replaceLevelButtons = Vector(4)
        replaceLevelIndicators = Vector(4)
        val icons = listOf(AllIcons.I_DONT_REPLACE, AllIcons.I_REPLACE_SOLID, AllIcons.I_REPLACE_ANY, AllIcons.I_REPLACE_EMPTY)
        val toolTips = listOf(
            Component.translatable("create.gui.schematicannon.option.dontReplaceSolid"),
            Component.translatable("create.gui.schematicannon.option.replaceWithSolid"),
            Component.translatable("create.gui.schematicannon.option.replaceWithAny"),
            Component.translatable("create.gui.schematicannon.option.replaceWithEmpty")
        )

        val options = listOf(
            ConfigureMECannonPacket.Option.DONT_REPLACE,
            ConfigureMECannonPacket.Option.REPLACE_SOLID,
            ConfigureMECannonPacket.Option.REPLACE_ANY,
            ConfigureMECannonPacket.Option.REPLACE_EMPTY
        )

        for (i in 0..3) {
            replaceLevelIndicators.add(Indicator(x + 33 + i * 18, y + 111, CommonComponents.EMPTY))
            val replaceLevelButton = IconButton(x + 33 + i * 18, y + 111, icons[i])
            val replaceMode = i
            replaceLevelButton.withCallback<IconButton>(Runnable {
                if (menu.contentHolder.replaceMode != replaceMode) {
                    sendOptionUpdate(options[replaceMode], true)
                }
            })
            replaceLevelButton.setToolTip(toolTips[i])
            replaceLevelButtons.add(replaceLevelButton)
        }
        placementSettingWidgets.addAll(replaceLevelButtons)

        skipMissingButton = IconButton(x + 111, y + 111, AllIcons.I_SKIP_MISSING)
        skipMissingButton.withCallback<IconButton>(Runnable {
            sendOptionUpdate(ConfigureMECannonPacket.Option.SKIP_MISSING, !menu.contentHolder.skipMissing)
        })
        skipMissingButton.setToolTip(Component.translatable("create.gui.schematicannon.option.skipMissing"))
        skipMissingIndicator = Indicator(x + 111, y + 111, CommonComponents.EMPTY)
        placementSettingWidgets.add(skipMissingButton)

        skipBlockEntitiesButton = IconButton(x + 135, y + 111, AllIcons.I_SKIP_BLOCK_ENTITIES)
        skipBlockEntitiesButton.withCallback<IconButton>(Runnable {
            sendOptionUpdate(ConfigureMECannonPacket.Option.SKIP_BLOCK_ENTITIES, !menu.contentHolder.replaceBlockEntities)
        })
        skipBlockEntitiesButton.setToolTip(Component.translatable("create.gui.schematicannon.option.skipBlockEntities"))
        skipBlockEntitiesIndicator = Indicator(x + 129, y + 111, CommonComponents.EMPTY)
        placementSettingWidgets.add(skipBlockEntitiesButton)

        addRenderableWidgets(placementSettingWidgets)
    }

    private fun placementSettingsHidden(): Boolean {
        return showSettingsIndicator.state == Indicator.State.OFF
    }

    override fun containerTick() {
        super.containerTick()

        val be = menu.contentHolder

        if (!placementSettingsHidden()) {
            for (replaceMode in 0 until replaceLevelButtons.size) {
                replaceLevelButtons[replaceMode].green = replaceMode == be.replaceMode
                replaceLevelIndicators[replaceMode].state = if (replaceMode == be.replaceMode) Indicator.State.ON else Indicator.State.OFF
            }
            skipMissingButton.green = be.skipMissing
            skipBlockEntitiesButton.green = !be.replaceBlockEntities
        }

        playIndicator.state = Indicator.State.OFF
        pauseIndicator.state = Indicator.State.OFF
        resetIndicator.state = Indicator.State.OFF

        when (be.state) {
            SchematicannonBlockEntity.State.PAUSED -> {
                pauseIndicator.state = Indicator.State.YELLOW
                playButton.active = true
                pauseButton.active = false
                resetButton.active = true
            }
            SchematicannonBlockEntity.State.RUNNING -> {
                playIndicator.state = Indicator.State.GREEN
                playButton.active = false
                pauseButton.active = true
                resetButton.active = true
            }
            SchematicannonBlockEntity.State.STOPPED -> {
                resetIndicator.state = Indicator.State.RED
                playButton.active = true
                pauseButton.active = false
                resetButton.active = false
            }
        }

        handleTooltips()
    }

    private fun holdShiftComponent(): Component {
        return if (hasShiftDown()) {
            Component.translatable("create.tooltip.keyShift").withStyle(ChatFormatting.GRAY)
        } else {
            Component.translatable("create.tooltip.holdForDescription",
                Component.translatable("create.tooltip.keyShift").withStyle(ChatFormatting.GRAY)
            ).withStyle(ChatFormatting.DARK_GRAY)
        }
    }

    private fun handleTooltips() {
        if (placementSettingsHidden()) return

        for (w in placementSettingWidgets) {
            if (w is IconButton) {
                if (!w.toolTip.isEmpty()) {
                    val first = w.toolTip[0]
                    w.toolTip.clear()
                    w.toolTip.add(first)
                    w.toolTip.add(holdShiftComponent())
                }
            }
        }

        if (hasShiftDown()) {
            fillToolTip(skipMissingButton, skipMissingIndicator, "skipMissing")
            fillToolTip(skipBlockEntitiesButton, skipBlockEntitiesIndicator, "skipBlockEntities")
            fillToolTip(replaceLevelButtons[0], replaceLevelIndicators[0], "dontReplaceSolid")
            fillToolTip(replaceLevelButtons[1], replaceLevelIndicators[1], "replaceWithSolid")
            fillToolTip(replaceLevelButtons[2], replaceLevelIndicators[2], "replaceWithAny")
            fillToolTip(replaceLevelButtons[3], replaceLevelIndicators[3], "replaceWithEmpty")
        }
    }

    private fun fillToolTip(button: IconButton, indicator: Indicator, tooltipKey: String) {
        if (!button.isHovered) return
        val enabled = button.green
        val tip = button.toolTip
        tip.add((if (enabled) optionEnabled else optionDisabled).plainCopy().withStyle(if (enabled) ChatFormatting.DARK_GREEN else ChatFormatting.RED))
        tip.addAll(FontHelper.cutTextComponent(Component.translatable("create.gui.schematicannon.option.$tooltipKey.description"), FontHelper.Palette.ALL_GRAY))
    }

    override fun renderBg(graphics: GuiGraphics, partialTicks: Float, mouseX: Int, mouseY: Int) {
        val invX = getLeftOfCentered(AllGuiTextures.PLAYER_INVENTORY.width)
        val invY = topPos + BG_H + 2
        renderPlayerInventory(graphics, invX, invY)

        val x = leftPos
        val y = topPos

        graphics.blit(GUI_TEXTURE, x, y, 0f, 0f, BG_W, BG_H, TEX_SIZE, TEX_SIZE)
        graphics.blit(GUI_TEXTURE, x, y - 2, TITLE_U.toFloat(), TITLE_V.toFloat(), TITLE_W, TITLE_H, TEX_SIZE, TEX_SIZE)

        val be = menu.contentHolder
        renderPrintingProgress(graphics, x, y, be.schematicProgress)

        if (!be.inventory.getStackInSlot(0).isEmpty) {
            renderBlueprintHighlight(graphics, x, y)
        }

        renderUpgradePanel(graphics, x + BG_W, y)

        graphics.drawString(font, title, x + (BG_W - 8 - font.width(title)) / 2, y + 4, 0xE0E0E0, false)

        val msg = Component.translatable("create.schematicannon.status." + be.statusMsg)
        var stringWidth = font.width(msg)

        if (be.missingItem != null) {
            stringWidth += 16
            GuiGameElement.of(be.missingItem)
                .at<GuiGameElement.GuiRenderBuilder>((x + 128).toFloat(), (y + 49).toFloat(), 100f)
                .scale(1.0)
                .render(graphics)
        }

        graphics.drawString(font, msg, x + 103 - stringWidth / 2, y + 53, 0xDDEEFF)

        if ("schematicErrored" == be.statusMsg) {
            graphics.drawString(font, Component.translatable("create.schematicannon.status.schematicErroredCheckLogs"),
                x + 103 - stringWidth / 2, y + 65, 0xDDEEFF)
        }

        if (showingMaterials) {
            renderMaterialChecklist(graphics, x, y)
        }
    }

    private fun renderUpgradePanel(graphics: GuiGraphics, panelX: Int, panelY: Int) {
        val w = UPGRADE_PADDING * 2 + UPGRADE_SLOT_SIZE
        val h = UPGRADE_PADDING * 2 + UPGRADE_SLOTS * UPGRADE_SLOT_SIZE

        graphics.fill(panelX, panelY, panelX + w, panelY + h, 0xFFC6C6C6.toInt())

        graphics.fill(panelX, panelY, panelX + w, panelY + 1, 0xFFFFFFFF.toInt())
        graphics.fill(panelX, panelY, panelX + 1, panelY + h, 0xFFFFFFFF.toInt())
        graphics.fill(panelX + w - 1, panelY, panelX + w, panelY + h, 0xFF555555.toInt())
        graphics.fill(panelX, panelY + h - 1, panelX + w, panelY + h, 0xFF555555.toInt())

        for (i in 0 until UPGRADE_SLOTS) {
            val slotX = panelX + UPGRADE_PADDING
            val slotY = panelY + UPGRADE_PADDING + i * UPGRADE_SLOT_SIZE
            graphics.fill(slotX, slotY, slotX + UPGRADE_SLOT_SIZE, slotY + UPGRADE_SLOT_SIZE, 0xFF8B8B8B.toInt())
            graphics.fill(slotX + 1, slotY + 1, slotX + UPGRADE_SLOT_SIZE, slotY + UPGRADE_SLOT_SIZE, 0xFFFFFFFF.toInt())
            graphics.fill(slotX + 1, slotY + 1, slotX + UPGRADE_SLOT_SIZE - 1, slotY + UPGRADE_SLOT_SIZE - 1, 0xFF8B8B8B.toInt())
        }
    }

    private fun renderMaterialChecklist(graphics: GuiGraphics, guiX: Int, guiY: Int) {
        val be = menu.contentHolder
        val checklist = be.checklist

        val overlayX = guiX + 10
        val overlayY = guiY + 20
        val overlayW = 193
        val overlayH = 100

        graphics.fill(overlayX, overlayY, overlayX + overlayW, overlayY + overlayH, 0xDD000000.toInt())

        val titleText = Component.translatable("gui.appliedcreate.me_blueprint_cannon.materials").withStyle(ChatFormatting.WHITE)
        graphics.drawString(font, titleText, overlayX + 4, overlayY + 4, 0xFFFFFF)

        var lineY = overlayY + 16
        val maxLines = 7
        var lineCount = 0

        for ((key, value) in checklist.required) {
            if (lineCount >= maxLines) {
                graphics.drawString(font, "...", overlayX + 4, lineY, 0xAAAAAA)
                break
            }
            val gathered = (checklist.gathered as java.util.Map<Any, Int>).getOrDefault(key, 0)
            val color = if (gathered >= value) 0x55FF55 else 0xFF5555
            val text = "${key.descriptionId.substringAfterLast('.')}: $gathered / $value"
            graphics.drawString(font, text, overlayX + 4, lineY, color)
            lineY += 11
            lineCount++
        }

        if (checklist.required.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.appliedcreate.me_blueprint_cannon.noMaterials").withStyle(ChatFormatting.GRAY), overlayX + 4, lineY, 0xAAAAAA)
        }
    }

    private fun renderBlueprintHighlight(graphics: GuiGraphics, x: Int, y: Int) {
        graphics.blit(GUI_TEXTURE, x + 10, y + 60, HIGHLIGHT_U.toFloat(), HIGHLIGHT_V.toFloat(), HIGHLIGHT_W, HIGHLIGHT_H, TEX_SIZE, TEX_SIZE)
    }

    private fun renderPrintingProgress(graphics: GuiGraphics, x: Int, y: Int, progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        graphics.blit(GUI_TEXTURE, x + 44, y + 64, PROGRESS_U.toFloat(), PROGRESS_V.toFloat(), (PROGRESS_W * p).toInt(), PROGRESS_H, TEX_SIZE, TEX_SIZE)
    }

    override fun renderForeground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val be = menu.contentHolder

        if (hoveredSlot != null && !hoveredSlot!!.hasItem()) {
            if (hoveredSlot!!.index == 0)
                graphics.renderComponentTooltip(font, FontHelper.cutTextComponent(Component.translatable("create.$_slotSchematic"), FontHelper.Palette.GRAY_AND_BLUE), mouseX, mouseY)
            if (hoveredSlot!!.index in 2..6)
                graphics.renderComponentTooltip(font, FontHelper.cutTextComponent(Component.translatable(_slotUpgrades), FontHelper.Palette.GRAY_AND_BLUE), mouseX, mouseY)
        }

        if (be.missingItem != null) {
            val missingBlockX = leftPos + 128
            val missingBlockY = topPos + 49
            if (mouseX >= missingBlockX && mouseY >= missingBlockY && mouseX <= missingBlockX + 16 && mouseY <= missingBlockY + 16) {
                graphics.renderTooltip(font, be.missingItem!!, mouseX, mouseY)
            }
        }

        super.renderForeground(graphics, mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (showingMaterials && button == 0) {
            val x = leftPos
            val y = topPos
            val overlayX = x + 10
            val overlayY = y + 20
            val overlayW = 193
            val overlayH = 100
            if (mouseX < overlayX || mouseX > overlayX + overlayW || mouseY < overlayY || mouseY > overlayY + overlayH) {
                if (!viewMaterialsButton.isMouseOver(mouseX, mouseY)) {
                    showingMaterials = false
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun sendOptionUpdate(option: ConfigureMECannonPacket.Option, set: Boolean) {
        AppliedCreate.CHANNEL.sendToServer(ConfigureMECannonPacket(option, set))
    }

    override fun getExtraAreas(): List<Rect2i> {
        return extraAreas
    }
}
