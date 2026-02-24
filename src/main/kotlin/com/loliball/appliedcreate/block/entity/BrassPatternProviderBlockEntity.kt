package com.loliball.appliedcreate.block.entity

import appeng.api.networking.IGridNodeListener
import appeng.api.orientation.BlockOrientation
import appeng.api.stacks.AEItemKey
import appeng.api.util.AECableType
import appeng.block.crafting.PushDirection
import appeng.blockentity.grid.AENetworkBlockEntity
import appeng.helpers.patternprovider.PatternProviderLogic
import appeng.helpers.patternprovider.PatternProviderLogicHost
import appeng.menu.ISubMenu
import appeng.menu.MenuOpener
import appeng.menu.locator.MenuLocator
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.logic.MechanicalCraftingPatternLogic
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional
import java.util.EnumSet

class BrassPatternProviderBlockEntity(pos: BlockPos, state: BlockState) :
    AENetworkBlockEntity(AppliedCreate.BRASS_PATTERN_PROVIDER_BE.get(), pos, state),
    PatternProviderLogicHost {

    internal val logic: PatternProviderLogic = createLogic()

    private fun createLogic(): PatternProviderLogic {
        return MechanicalCraftingPatternLogic(this.mainNode, this, PATTERN_SLOTS)
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        this.logic.onMainNodeStateChanged()
    }

    override fun getGridConnectableSides(orientation: BlockOrientation): Set<Direction> {
        val pushDirection = getPushDirection().direction
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

    override fun saveAdditional(data: CompoundTag) {
        super.saveAdditional(data)
        this.logic.writeToNBT(data)
    }

    override fun loadTag(data: CompoundTag) {
        super.loadTag(data)
        this.logic.readFromNBT(data)
    }

    override fun getCableConnectionType(dir: Direction): AECableType {
        return AECableType.SMART
    }

    override fun getLogic(): PatternProviderLogic = logic

    override fun getTargets(): EnumSet<Direction> {
        val pushDirection = getPushDirection()
        return if (pushDirection.direction == null) {
            EnumSet.allOf(Direction::class.java)
        } else {
            EnumSet.of(pushDirection.direction)
        }
    }

    override fun getTerminalIcon(): AEItemKey {
        return AEItemKey.of(AppliedCreate.BRASS_PATTERN_PROVIDER_ITEM.get())
    }

    override fun saveChanges() {
        this.setChanged()
    }

    override fun getMainMenuIcon(): ItemStack {
        return AppliedCreate.BRASS_PATTERN_PROVIDER_ITEM.get().defaultInstance
    }

    override fun openMenu(player: Player, locator: MenuLocator) {
        MenuOpener.open(AppliedCreate.BRASS_PATTERN_PROVIDER_MENU.get(), player, locator)
    }

    override fun returnToMainMenu(player: Player, subMenu: ISubMenu) {
        MenuOpener.returnTo(AppliedCreate.BRASS_PATTERN_PROVIDER_MENU.get(), player, subMenu.locator)
    }

    override fun <T : Any> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        val lo = logic.getCapability(cap)
        if (lo.isPresent) {
            return lo
        }
        return super.getCapability(cap, side)
    }

    override fun setBlockState(state: BlockState) {
        super.setBlockState(state)
        onGridConnectableSidesChanged()
    }

    private fun getPushDirection(): PushDirection {
        return blockState.getValue(com.loliball.appliedcreate.block.AndesitePatternProviderBlock.PUSH_DIRECTION)
    }

    companion object {
        const val PATTERN_SLOTS = 36
    }
}
