package com.loliball.appliedcreate.part

import appeng.api.crafting.IPatternDetails
import appeng.api.networking.IGridNode
import appeng.api.networking.IGridNodeListener
import appeng.api.networking.IManagedGridNode
import appeng.api.networking.crafting.ICraftingProvider
import appeng.api.networking.ticking.IGridTickable
import appeng.api.networking.ticking.TickRateModulation
import appeng.api.networking.ticking.TickingRequest
import appeng.api.parts.IPartCollisionHelper
import appeng.api.parts.IPartItem
import appeng.api.parts.IPartModel
import appeng.api.stacks.AEKey
import appeng.api.stacks.KeyCounter
import appeng.api.util.AECableType
import appeng.parts.AEBasePart
import appeng.parts.PartModel
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.logic.MechanicalCraftingPatternLogic
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraftforge.network.NetworkHooks
import java.util.EnumSet

class BrassPatternProviderPart(partItem: IPartItem<*>) : AEBasePart(partItem),
    ICraftingProvider, IGridTickable {

    val logic = MechanicalCraftingPatternLogic(PATTERN_SLOTS, object : MechanicalCraftingPatternLogic.Host {
        override val mainNode: IManagedGridNode get() = this@BrassPatternProviderPart.mainNode
        override val blockEntity get() = this@BrassPatternProviderPart.blockEntity
        override val level: Level? get() = this@BrassPatternProviderPart.blockEntity?.level
        override val worldPosition: BlockPos get() = this@BrassPatternProviderPart.blockEntity.blockPos
        override fun setChanged() {
            this@BrassPatternProviderPart.host?.markForSave()
        }
        override fun getTargetDirections(): Set<Direction> = EnumSet.of(this@BrassPatternProviderPart.side)
    })

    override fun createMainNode(): IManagedGridNode {
        return super.createMainNode()
            .addService(ICraftingProvider::class.java, this)
            .addService(IGridTickable::class.java, this)
            .setIdlePowerUsage(8.0)
    }

    override fun onMainNodeStateChanged(reason: IGridNodeListener.State) {
        super.onMainNodeStateChanged(reason)
    }

    override fun getAvailablePatterns(): List<IPatternDetails> = logic.availablePatterns
    override fun pushPattern(patternDetails: IPatternDetails, inputs: Array<KeyCounter>): Boolean =
        logic.pushPattern(patternDetails, inputs)
    override fun isBusy(): Boolean = logic.isBusy
    override fun getPatternPriority(): Int = logic.patternPriority
    override fun getEmitableItems(): Set<AEKey> = logic.emitableItems

    override fun getTickingRequest(node: IGridNode): TickingRequest {
        return TickingRequest(5, 20, false, false)
    }

    override fun tickingRequest(node: IGridNode, ticksSinceLastCall: Int): TickRateModulation {
        val be = blockEntity ?: return TickRateModulation.SLEEP
        val level = be.level ?: return TickRateModulation.SLEEP
        logic.serverTick(level, be.blockPos)
        return if (logic.isBusy) TickRateModulation.SAME else TickRateModulation.SLEEP
    }

    override fun getBoxes(bch: IPartCollisionHelper) {
        bch.addBox(2.0, 2.0, 14.0, 14.0, 14.0, 16.0)
        bch.addBox(5.0, 5.0, 12.0, 11.0, 11.0, 14.0)
    }

    override fun getCableConnectionLength(cable: AECableType): Float = 4f

    override fun readFromNBT(data: CompoundTag) {
        super.readFromNBT(data)
        logic.loadTag(data)
    }

    override fun writeToNBT(data: CompoundTag) {
        super.writeToNBT(data)
        logic.saveAdditional(data)
    }

    override fun addToWorld() {
        super.addToWorld()
        ICraftingProvider.requestUpdate(mainNode)
    }

    override fun addAdditionalDrops(drops: MutableList<ItemStack>, wrenched: Boolean) {
        super.addAdditionalDrops(drops, wrenched)
        drops.addAll(logic.dropContents())
    }

    override fun onPartActivate(player: Player, hand: InteractionHand, pos: Vec3): Boolean {
        if (!player.level().isClientSide && player is ServerPlayer) {
            val be = blockEntity
            val side = side
            NetworkHooks.openScreen(player, object : net.minecraft.world.MenuProvider {
                override fun getDisplayName(): Component =
                    Component.translatable("item.appliedcreate.brass_pattern_provider_part")

                override fun createMenu(windowId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
                    return com.loliball.appliedcreate.gui.BrassPatternProviderPartMenu(
                        windowId, playerInventory, this@BrassPatternProviderPart
                    )
                }
            }) { buf ->
                buf.writeBlockPos(be.blockPos)
                buf.writeEnum(side)
            }
        }
        return true
    }

    override fun getStaticModels(): IPartModel {
        return if (isActive && isPowered) {
            MODELS_HAS_CHANNEL
        } else if (isPowered) {
            MODELS_ON
        } else {
            MODELS_OFF
        }
    }

    companion object {
        const val PATTERN_SLOTS = 36

        val MODELS_OFF = AndesitePatternProviderPart.MODELS_OFF
        val MODELS_ON = AndesitePatternProviderPart.MODELS_ON
        val MODELS_HAS_CHANNEL = AndesitePatternProviderPart.MODELS_HAS_CHANNEL
    }
}
