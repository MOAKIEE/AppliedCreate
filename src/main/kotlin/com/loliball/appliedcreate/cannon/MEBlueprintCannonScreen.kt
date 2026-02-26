package com.loliball.appliedcreate.cannon

import com.loliball.appliedcreate.AppliedCreate
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity
import com.simibubi.create.foundation.gui.AllGuiTextures
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.Indicator
import com.simibubi.create.foundation.item.TooltipHelper
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.gui.element.GuiGameElement
import net.createmod.catnip.lang.FontHelper
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.renderer.Rect2i
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.network.PacketDistributor
import java.util.*

class MEBlueprintCannonScreen(menu: MEBlueprintCannonMenu, inventory: Inventory, title: Component) :
    AbstractSimiContainerScreen<MEBlueprintCannonMenu>(menu, inventory, title) {

    private val BG_BOTTOM = AllGuiTextures.SCHEMATICANNON_BOTTOM
    private val BG_TOP = AllGuiTextures.SCHEMATICANNON_TOP

    private val listPrinter = CreateLang.translateDirect("gui.schematicannon.listPrinter")
    private val _showSettings = "gui.schematicannon.showOptions"
    private val _slotListPrinter = "gui.schematicannon.slot.listPrinter"
    private val _slotSchematic = "gui.schematicannon.slot.schematic"
    
    // Custom translation keys or reuse Create's? Reuse Create's generic ones, but custom for upgrades.
    private val _slotUpgrades = "gui.appliedcreate.slot.upgrades"

    private val optionEnabled = CreateLang.translateDirect("gui.schematicannon.optionEnabled")
    private val optionDisabled = CreateLang.translateDirect("gui.schematicannon.optionDisabled")

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

    private var placementSettingWidgets = ArrayList<AbstractWidget>()
    private val renderedItem = ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "schematicannon")))
    private var extraAreas: List<Rect2i> = Collections.emptyList()

    override fun init() {
        setWindowSize(BG_TOP.width, BG_TOP.height + BG_BOTTOM.height + 2 + AllGuiTextures.PLAYER_INVENTORY.height)
        setWindowOffset(-11, 0)
        super.init()

        val x = leftPos
        val y = topPos

        // Play Pause Stop
        playButton = IconButton(x + 75, y + 85, AllIcons.I_PLAY)
        playButton.withCallback<IconButton> { sendOptionUpdate(ConfigureMECannonPayload.Option.PLAY, true) }
        playIndicator = Indicator(x + 75, y + 79, CommonComponents.EMPTY)
        
        pauseButton = IconButton(x + 93, y + 85, AllIcons.I_PAUSE)
        pauseButton.withCallback<IconButton> { sendOptionUpdate(ConfigureMECannonPayload.Option.PAUSE, true) }
        pauseIndicator = Indicator(x + 93, y + 79, CommonComponents.EMPTY)
        
        resetButton = IconButton(x + 111, y + 85, AllIcons.I_STOP)
        resetButton.withCallback<IconButton> { sendOptionUpdate(ConfigureMECannonPayload.Option.STOP, true) }
        resetIndicator = Indicator(x + 111, y + 79, CommonComponents.EMPTY)
        resetIndicator.state = Indicator.State.RED
        
        addRenderableWidgets(playButton, playIndicator, pauseButton, pauseIndicator, resetButton, resetIndicator)

        confirmButton = IconButton(x + 180, y + 111, AllIcons.I_CONFIRM)
        confirmButton.withCallback<IconButton> { minecraft!!.player!!.closeContainer() }
        addRenderableWidget(confirmButton)
        
        showSettingsButton = IconButton(x + 8, y + 111, AllIcons.I_PLACEMENT_SETTINGS)
        showSettingsButton.withCallback<IconButton> {
            showSettingsIndicator.state = if (placementSettingsHidden()) Indicator.State.GREEN else Indicator.State.OFF
            initPlacementSettings()
        }
        showSettingsButton.setToolTip(CreateLang.translateDirect(_showSettings))
        addRenderableWidget(showSettingsButton)
        
        showSettingsIndicator = Indicator(x + 9, y + 111, CommonComponents.EMPTY)
        // addRenderableWidget(showSettingsIndicator)

        extraAreas = listOf(Rect2i(x + BG_TOP.width, y + BG_TOP.height + BG_BOTTOM.height - 62, 84, 92))
        tick()
    }

    private fun initPlacementSettings() {
        removeWidgets(placementSettingWidgets)
        placementSettingWidgets.clear()

        if (placementSettingsHidden()) return

        val x = leftPos
        val y = topPos

        // Replace settings
        replaceLevelButtons = Vector(4)
        replaceLevelIndicators = Vector(4)
        val icons = listOf(AllIcons.I_DONT_REPLACE, AllIcons.I_REPLACE_SOLID, AllIcons.I_REPLACE_ANY, AllIcons.I_REPLACE_EMPTY)
        val toolTips = listOf(
            CreateLang.translateDirect("gui.schematicannon.option.dontReplaceSolid"),
            CreateLang.translateDirect("gui.schematicannon.option.replaceWithSolid"),
            CreateLang.translateDirect("gui.schematicannon.option.replaceWithAny"),
            CreateLang.translateDirect("gui.schematicannon.option.replaceWithEmpty")
        )
        
        // Map index to Option enum
        val options = listOf(
            ConfigureMECannonPayload.Option.DONT_REPLACE, // 0
            ConfigureMECannonPayload.Option.REPLACE_SOLID, // 1
            ConfigureMECannonPayload.Option.REPLACE_ANY, // 2
            ConfigureMECannonPayload.Option.REPLACE_EMPTY // 3
        )
        // Wait, check BE logic for mapping.
        // SchematicannonBlockEntity: 
        // 0: DONT_REPLACE (actually replaceMode=0) -> checking logic in BE
        // 1: REPLACE_SOLID (actually replaceMode=1)
        // 2: REPLACE_ANY (replaceMode=2)
        // 3: REPLACE_EMPTY (replaceMode=3)
        // The icons list order in SchematicannonScreen matches this?
        // SchematicannonScreen:
        // icons: DONT_REPLACE, REPLACE_SOLID, REPLACE_ANY, REPLACE_EMPTY
        // indices: 0, 1, 2, 3
        // BE.replaceMode checks:
        // replaceMode == 3 -> REPLACE_EMPTY (implied from shouldPlace logic, returns true immediately)
        // replaceMode == 2 -> REPLACE_ANY
        // replaceMode == 1 -> REPLACE_SOLID
        // replaceMode == 0 -> DONT_REPLACE
        // So the mapping is correct.

        for (i in 0..3) {
            replaceLevelIndicators.add(Indicator(x + 33 + i * 18, y + 111, CommonComponents.EMPTY))
            val replaceLevelButton = IconButton(x + 33 + i * 18, y + 111, icons[i])
            val replaceMode = i
            replaceLevelButton.withCallback<IconButton> {
                if (menu.contentHolder.replaceMode != replaceMode) {
                    sendOptionUpdate(options[replaceMode], true)
                }
            }
            replaceLevelButton.setToolTip(toolTips[i])
            replaceLevelButtons.add(replaceLevelButton)
        }
        placementSettingWidgets.addAll(replaceLevelButtons)

        // Other Settings
        skipMissingButton = IconButton(x + 111, y + 111, AllIcons.I_SKIP_MISSING)
        skipMissingButton.withCallback<IconButton> {
            sendOptionUpdate(ConfigureMECannonPayload.Option.SKIP_MISSING, !menu.contentHolder.skipMissing)
        }
        skipMissingButton.setToolTip(CreateLang.translateDirect("gui.schematicannon.option.skipMissing"))
        skipMissingIndicator = Indicator(x + 111, y + 111, CommonComponents.EMPTY)
        placementSettingWidgets.add(skipMissingButton)

        skipBlockEntitiesButton = IconButton(x + 135, y + 111, AllIcons.I_SKIP_BLOCK_ENTITIES)
        skipBlockEntitiesButton.withCallback<IconButton> {
            sendOptionUpdate(ConfigureMECannonPayload.Option.SKIP_BLOCK_ENTITIES, !menu.contentHolder.replaceBlockEntities)
        }
        skipBlockEntitiesButton.setToolTip(CreateLang.translateDirect("gui.schematicannon.option.skipBlockEntities"))
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

    private fun handleTooltips() {
        if (placementSettingsHidden()) return

        for (w in placementSettingWidgets) {
            if (w is IconButton) {
                if (!w.toolTip.isEmpty()) {
                    val first = w.toolTip[0]
                    w.toolTip.clear()
                    w.toolTip.add(first)
                    w.toolTip.add(TooltipHelper.holdShift(FontHelper.Palette.BLUE, hasShiftDown()))
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
        tip.addAll(TooltipHelper.cutTextComponent(CreateLang.translateDirect("gui.schematicannon.option.$tooltipKey.description"), FontHelper.Palette.ALL_GRAY))
    }

    override fun renderBg(graphics: GuiGraphics, partialTicks: Float, mouseX: Int, mouseY: Int) {
        val invX = getLeftOfCentered(AllGuiTextures.PLAYER_INVENTORY.width)
        val invY = topPos + BG_TOP.height + BG_BOTTOM.height + 2
        renderPlayerInventory(graphics, invX, invY)

        val x = leftPos
        val y = topPos

        BG_TOP.render(graphics, x, y)
        BG_BOTTOM.render(graphics, x, y + BG_TOP.height)
        AllGuiTextures.SCHEMATIC_TITLE.render(graphics, x, y - 2)

        val be = menu.contentHolder
        renderPrintingProgress(graphics, x, y, be.schematicProgress)
        
        // Render AE power bar instead of gunpowder
        // Use a different texture or color?
        // For now reuse gunpowder bar but maybe tint it?
        // Or just render it normally as "fuel"
        val amount = be.remainingFuel / (be.getShotsPerGunpowder().toFloat().coerceAtLeast(1f)) // Avoid div by zero
        renderFuelBar(graphics, x, y, amount.coerceIn(0f, 1f))
        
        renderChecklistPrinterProgress(graphics, x, y, be.bookPrintingProgress)

        if (!be.inventory.getStackInSlot(0).isEmpty) {
            renderBlueprintHighlight(graphics, x, y)
        }

        GuiGameElement.of(renderedItem)
            .at<GuiGameElement.GuiRenderBuilder>(x + BG_TOP.width.toFloat(), (y + BG_TOP.height + BG_BOTTOM.height - 48).toFloat(), -200f)
            .scale(5.0)
            .render(graphics)

        graphics.drawString(font, title, x + (BG_TOP.width - 8 - font.width(title)) / 2, y + 2, 0x505050, false)

        val msg = CreateLang.translateDirect("schematicannon.status." + be.statusMsg)
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
            graphics.drawString(font, CreateLang.translateDirect("schematicannon.status.schematicErroredCheckLogs"),
                x + 103 - stringWidth / 2, y + 65, 0xDDEEFF)
        }
    }

    private fun renderBlueprintHighlight(graphics: GuiGraphics, x: Int, y: Int) {
        AllGuiTextures.SCHEMATICANNON_HIGHLIGHT.render(graphics, x + 10, y + 60)
    }

    private fun renderPrintingProgress(graphics: GuiGraphics, x: Int, y: Int, progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        val sprite = AllGuiTextures.SCHEMATICANNON_PROGRESS
        graphics.blit(sprite.location, x + 44, y + 64, sprite.startX, sprite.startY, (sprite.width * p).toInt(), sprite.height)
    }

    private fun renderChecklistPrinterProgress(graphics: GuiGraphics, x: Int, y: Int, progress: Float) {
        val sprite = AllGuiTextures.SCHEMATICANNON_CHECKLIST_PROGRESS
        graphics.blit(sprite.location, x + 154, y + 20, sprite.startX, sprite.startY, (sprite.width * progress).toInt(), sprite.height)
    }

    private fun renderFuelBar(graphics: GuiGraphics, x: Int, y: Int, amount: Float) {
        // Reuse schematicannon fuel bar for now, maybe in future use a custom AE energy bar
        val sprite = AllGuiTextures.SCHEMATICANNON_FUEL
        graphics.blit(sprite.location, x + 36, y + 19, sprite.startX, sprite.startY, (sprite.width * amount).toInt(), sprite.height)
    }

    override fun renderForeground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val be = menu.contentHolder
        val x = leftPos
        val y = topPos

        val fuelX = x + 36
        val fuelY = y + 19
        if (mouseX >= fuelX && mouseY >= fuelY && mouseX <= fuelX + AllGuiTextures.SCHEMATICANNON_FUEL.width && mouseY <= fuelY + AllGuiTextures.SCHEMATICANNON_FUEL.height) {
            val tooltip = getFuelLevelTooltip(be)
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY)
        }

        if (hoveredSlot != null && !hoveredSlot!!.hasItem()) {
            if (hoveredSlot!!.index == 0)
                graphics.renderComponentTooltip(font, TooltipHelper.cutTextComponent(CreateLang.translateDirect(_slotSchematic), FontHelper.Palette.GRAY_AND_BLUE), mouseX, mouseY)
            if (hoveredSlot!!.index == 2)
                graphics.renderComponentTooltip(font, TooltipHelper.cutTextComponent(CreateLang.translateDirect(_slotListPrinter), FontHelper.Palette.GRAY_AND_BLUE), mouseX, mouseY)
            if (hoveredSlot!!.index == 4)
                graphics.renderComponentTooltip(font, TooltipHelper.cutTextComponent(Component.translatable(_slotUpgrades), FontHelper.Palette.GRAY_AND_BLUE), mouseX, mouseY)
        }

        if (be.missingItem != null) {
            val missingBlockX = x + 128
            val missingBlockY = y + 49
            if (mouseX >= missingBlockX && mouseY >= missingBlockY && mouseX <= missingBlockX + 16 && mouseY <= missingBlockY + 16) {
                graphics.renderTooltip(font, be.missingItem!!, mouseX, mouseY)
            }
        }

        val paperX = x + 112
        val paperY = y + 19
        if (mouseX >= paperX && mouseY >= paperY && mouseX <= paperX + 16 && mouseY <= paperY + 16)
            graphics.renderTooltip(font, listPrinter, mouseX, mouseY)

        super.renderForeground(graphics, mouseX, mouseY, partialTicks)
    }

    private fun getFuelLevelTooltip(be: MEBlueprintCannonBlockEntity): List<Component> {
        val shotsLeft = be.remainingFuel
        val tooltip = ArrayList<Component>()
        // Simple tooltip for AE power
        tooltip.add(Component.literal("AE Power Buffer: $shotsLeft shots").withStyle(ChatFormatting.BLUE))
        // Add more details if needed
        return tooltip
    }

    private fun sendOptionUpdate(option: ConfigureMECannonPayload.Option, set: Boolean) {
        PacketDistributor.sendToServer(ConfigureMECannonPayload(option, set))
    }

    override fun getExtraAreas(): List<Rect2i> {
        return extraAreas
    }
}
