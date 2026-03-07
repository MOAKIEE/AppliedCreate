package com.loliball.appliedcreate.patternprovider

import appeng.api.networking.IGridNodeListener
import appeng.api.orientation.BlockOrientation
import appeng.api.stacks.AEItemKey
import appeng.api.util.AECableType
import appeng.block.crafting.PushDirection
import appeng.blockentity.grid.AENetworkedBlockEntity
import appeng.helpers.patternprovider.PatternProviderLogic
import appeng.helpers.patternprovider.PatternProviderLogicHost
import appeng.menu.ISubMenu
import appeng.menu.MenuOpener
import appeng.menu.locator.MenuHostLocator
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.patternprovider.AndesitePatternProviderBlock
import com.loliball.appliedcreate.patternprovider.MechanicalCraftingPatternLogic
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import java.util.EnumSet

class AndesitePatternProviderBlockEntity(pos: BlockPos, state: BlockState) :
    AENetworkedBlockEntity(AppliedCreate.ANDESITE_PATTERN_PROVIDER_BE.get(), pos, state),
    PatternProviderLogicHost {

    internal val logic: PatternProviderLogic = createLogic()

    private fun createLogic(): PatternProviderLogic {
        return MechanicalCraftingPatternLogic(this.mainNode, this, PATTERN_SLOTS)
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        this.logic.onMainNodeStateChanged()
    }

    override fun getGridConnectableSides(orientation: BlockOrientation): Set<Direction> {
        val pushDirection = getPushDirection().getDirection()
        return if (pushDirection == null) {
            EnumSet.allOf(Direction::class.java)
        } else {
            EnumSet.complementOf(EnumSet.of(pushDirection))
        }
    }

    override fun addAdditionalDrops(level: Level, pos: BlockPos, drops: MutableList<ItemStack>) {
        super.addAdditionalDrops(level, pos, drops)
        this.logic.addDrops(drops)
    }

    override fun clearContent() {
        super.clearContent()
        this.logic.clearContent()
    }

    override fun onReady() {
        super.onReady()
        this.logic.updatePatterns()
    }

    override fun saveAdditional(data: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(data, registries)
        this.logic.writeToNBT(data, registries)
    }

    override fun loadTag(data: CompoundTag, registries: HolderLookup.Provider) {
        super.loadTag(data, registries)
        this.logic.readFromNBT(data, registries)
    }

    override fun getCableConnectionType(dir: Direction): AECableType {
        return AECableType.SMART
    }

    override fun getLogic(): PatternProviderLogic = logic

    override fun getTargets(): EnumSet<Direction> {
        val pushDirection = getPushDirection()
        return if (pushDirection.getDirection() == null) {
            EnumSet.allOf(Direction::class.java)
        } else {
            EnumSet.of(pushDirection.getDirection())
        }
    }

    override fun getTerminalIcon(): AEItemKey {
        return AEItemKey.of(AppliedCreate.ANDESITE_PATTERN_PROVIDER_ITEM.get())
    }

    override fun saveChanges() {
        this.setChanged()
    }

    override fun getMainMenuIcon(): ItemStack {
        return AppliedCreate.ANDESITE_PATTERN_PROVIDER_ITEM.get().defaultInstance
    }

    override fun openMenu(player: Player, locator: MenuHostLocator) {
        MenuOpener.open(AppliedCreate.ANDESITE_PATTERN_PROVIDER_MENU.get(), player, locator)
    }
    override fun returnToMainMenu(player: Player, subMenu: ISubMenu) {
        MenuOpener.returnTo(AppliedCreate.ANDESITE_PATTERN_PROVIDER_MENU.get(), player, subMenu.getLocator())
    }

    override fun setBlockState(state: BlockState) {
        super.setBlockState(state)
        onGridConnectableSidesChanged()
    }

    private fun getPushDirection(): PushDirection {
        return blockState.getValue(AndesitePatternProviderBlock.PUSH_DIRECTION)
    }

    companion object {
        const val PATTERN_SLOTS = 9
    }
}