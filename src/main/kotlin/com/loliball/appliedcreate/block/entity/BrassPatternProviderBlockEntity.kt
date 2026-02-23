package com.loliball.appliedcreate.block.entity

import appeng.api.crafting.IPatternDetails
import appeng.api.crafting.PatternDetailsHelper
import appeng.api.networking.GridHelper
import appeng.api.networking.IManagedGridNode
import appeng.api.networking.crafting.ICraftingProvider
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.AEKey
import appeng.api.stacks.KeyCounter
import appeng.blockentity.grid.AENetworkBlockEntity
import appeng.me.helpers.BlockEntityNodeListener
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.gui.BrassPatternProviderMenu
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.items.ItemStackHandler

class BrassPatternProviderBlockEntity(pos: BlockPos, state: BlockState) :
    AENetworkBlockEntity(AppliedCreate.BRASS_PATTERN_PROVIDER_BE.get(), pos, state),
    ICraftingProvider, MenuProvider {

    val inventory = object : ItemStackHandler(9) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
            ICraftingProvider.requestUpdate(mainNode)
        }
    }

    private var isBusy = false

    override fun createMainNode(): IManagedGridNode {
        return GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
            .addService(ICraftingProvider::class.java, this)
            .setIdlePowerUsage(8.0)
    }

    override fun getAvailablePatterns(): List<IPatternDetails> {
        val level = this.level ?: return emptyList()
        val patterns = ArrayList<IPatternDetails>()
        for (i in 0 until inventory.slots) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                val details = PatternDetailsHelper.decodePattern(stack, level)
                if (details != null) {
                    patterns.add(details)
                }
            }
        }
        return patterns
    }

    override fun pushPattern(patternDetails: IPatternDetails, inputs: Array<KeyCounter>): Boolean {
        if (isBusy) return false

        val level = this.level ?: return false
        val outputKey = patternDetails.primaryOutput?.what as? AEItemKey ?: return false
        val outputStack = outputKey.toStack()

        @Suppress("UNCHECKED_CAST")
        val recipeType = BuiltInRegistries.RECIPE_TYPE
            .get(ResourceLocation("create", "mechanical_crafting"))
            as? RecipeType<MechanicalCraftingRecipe> ?: return false
        val recipes = level.recipeManager.getAllRecipesFor(recipeType)

        val recipe = recipes.firstOrNull {
            ItemStack.isSameItemSameTags(it.getResultItem(level.registryAccess()), outputStack)
        } ?: return false

        for (direction in Direction.values()) {
            val neighborPos = worldPosition.relative(direction)
            val neighbor = level.getBlockEntity(neighborPos)
            if (neighbor is MechanicalCrafterBlockEntity) {
                val crafters = RecipeGridHandler.getAllCraftersOfChain(neighbor)
                if (crafters.isEmpty()) continue

                val recipeWidth = recipe.width
                val recipeHeight = recipe.height

                if (crafters.size < recipeWidth * recipeHeight) continue

                val inputStacks = ArrayList<ItemStack>()
                for (input in inputs) {
                    for (entry in input) {
                        val key = entry.key
                        if (key is AEItemKey) {
                            inputStacks.add(key.toStack(entry.longValue.toInt()))
                        }
                    }
                }

                for (i in 0 until recipeWidth * recipeHeight) {
                    if (i >= crafters.size) break

                    val ingredient = recipe.ingredients.getOrNull(i)
                    if (ingredient != null && !ingredient.isEmpty) {
                        val matchIndex = inputStacks.indexOfFirst { ingredient.test(it) }
                        if (matchIndex != -1) {
                            val stackToInsert = inputStacks[matchIndex]
                            val inserted = crafters[i].inventory.insertItem(0, stackToInsert.copy(), false)
                            if (inserted.isEmpty) {
                                inputStacks.removeAt(matchIndex)
                            }
                        }
                    }
                }

                isBusy = true
                return true
            }
        }

        return false
    }

    override fun isBusy(): Boolean {
        return isBusy
    }

    override fun getPatternPriority(): Int {
        return 0
    }

    override fun getEmitableItems(): Set<AEKey> {
        return emptySet()
    }

    fun serverTick(level: Level, pos: BlockPos, state: BlockState) {
        if (!isBusy) return

        var active = false
        for (direction in Direction.values()) {
            val neighbor = level.getBlockEntity(pos.relative(direction))
            if (neighbor is MechanicalCrafterBlockEntity) {
                val crafters = RecipeGridHandler.getAllCraftersOfChain(neighbor)
                if (crafters.any { !it.inventory.getStackInSlot(0).isEmpty }) {
                    active = true
                    break
                }
            }
        }

        if (!active) {
            isBusy = false
        }
    }

    override fun getDisplayName(): Component {
        return Component.translatable("block.appliedcreate.brass_pattern_provider")
    }

    override fun createMenu(windowId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
        return BrassPatternProviderMenu(windowId, playerInventory, this)
    }

    override fun loadTag(tag: CompoundTag) {
        super.loadTag(tag)
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"))
        }
        isBusy = tag.getBoolean("IsBusy")
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.put("Inventory", inventory.serializeNBT())
        tag.putBoolean("IsBusy", isBusy)
    }
}
