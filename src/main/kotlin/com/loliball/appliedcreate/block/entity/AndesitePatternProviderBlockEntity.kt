package com.loliball.appliedcreate.block.entity

import appeng.api.crafting.IPatternDetails
import appeng.api.networking.GridHelper
import appeng.api.networking.IManagedGridNode
import appeng.api.networking.crafting.ICraftingProvider
import appeng.api.stacks.AEKey
import appeng.api.stacks.KeyCounter
import appeng.blockentity.grid.AENetworkBlockEntity
import appeng.me.helpers.BlockEntityNodeListener
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.gui.AndesitePatternProviderMenu
import com.loliball.appliedcreate.logic.MechanicalCraftingPatternLogic
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.Containers
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class AndesitePatternProviderBlockEntity(pos: BlockPos, state: BlockState) :
    AENetworkBlockEntity(AppliedCreate.ANDESITE_PATTERN_PROVIDER_BE.get(), pos, state),
    ICraftingProvider, MenuProvider {

    val logic = MechanicalCraftingPatternLogic(PATTERN_SLOTS, object : MechanicalCraftingPatternLogic.Host {
        override val mainNode: IManagedGridNode get() = this@AndesitePatternProviderBlockEntity.mainNode
        override val blockEntity get() = this@AndesitePatternProviderBlockEntity
        override val level get() = this@AndesitePatternProviderBlockEntity.level
        override val worldPosition get() = this@AndesitePatternProviderBlockEntity.blockPos
        override fun setChanged() = this@AndesitePatternProviderBlockEntity.setChanged()
        override fun getTargetDirections(): Set<Direction> = Direction.values().toSet()
    })

    override fun createMainNode(): IManagedGridNode {
        return GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .addService(ICraftingProvider::class.java, this)
            .setIdlePowerUsage(8.0)
    }

    override fun getAvailablePatterns(): List<IPatternDetails> = logic.availablePatterns
    override fun pushPattern(patternDetails: IPatternDetails, inputs: Array<KeyCounter>): Boolean =
        logic.pushPattern(patternDetails, inputs)
    override fun isBusy(): Boolean = logic.isBusy
    override fun getPatternPriority(): Int = logic.patternPriority
    override fun getEmitableItems(): Set<AEKey> = logic.emitableItems

    fun serverTick(level: Level, pos: BlockPos, state: BlockState) {
        logic.serverTick(level, pos)
    }

    override fun getDisplayName(): Component =
        Component.translatable("block.appliedcreate.andesite_pattern_provider")

    override fun createMenu(windowId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu =
        AndesitePatternProviderMenu(windowId, playerInventory, this)

    override fun loadTag(tag: CompoundTag) {
        super.loadTag(tag)
        logic.loadTag(tag)
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        logic.saveAdditional(tag)
    }

    fun dropContents(level: Level, pos: BlockPos) {
        for (stack in logic.dropContents()) {
            Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
        }
    }

    companion object {
        const val PATTERN_SLOTS = 9
    }
}
