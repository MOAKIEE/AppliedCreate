package com.loliball.appliedcreate.block.entity

import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.gui.MechanicalCraftEncoderMenu
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe
import com.simibubi.create.content.logistics.BigItemStack
import com.simibubi.create.content.logistics.box.PackageItem
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.Containers
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.ItemStackHandler
import kotlin.math.abs

class MechanicalCraftEncoderBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(AppliedCreate.MECHANICAL_CRAFT_ENCODER_BE.get(), pos, state), MenuProvider {

    companion object {
        const val INPUT_SLOTS = 81
        const val FILTER_SLOT = 81
        const val OUTPUT_SLOT = 82
        const val TOTAL_SLOTS = 83
        const val TICK_INTERVAL = 30
        const val DEFAULT_MIN_WIDTH = 5
    }

    val inventory: ItemStackHandler = object : ItemStackHandler(TOTAL_SLOTS) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
        }

        override fun getSlotLimit(slot: Int): Int {
            return if (slot == FILTER_SLOT) 1 else 64
        }
    }

    private val inventoryCapability: LazyOptional<ItemStackHandler> = LazyOptional.of { inventory }
    private var tickCounter = 0
    var minWidth: Int = DEFAULT_MIN_WIDTH

    override fun getDisplayName(): Component {
        return Component.translatable("block.appliedcreate.mechanical_craft_encoder")
    }

    override fun createMenu(windowId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
        return MechanicalCraftEncoderMenu(windowId, playerInventory, this)
    }

    fun serverTick(level: Level, pos: BlockPos, state: BlockState) {
        tickCounter++
        if (tickCounter < TICK_INTERVAL) return
        tickCounter = 0

        if (!level.hasNeighborSignal(pos)) return
        processRecipes(level)
    }

    fun onNeighborChanged() {
        val level = this.level ?: return
        if (level.isClientSide) return
        if (!level.hasNeighborSignal(blockPos)) return
        processRecipes(level)
    }

    private fun processRecipes(level: Level) {
        val outputStack = inventory.getStackInSlot(OUTPUT_SLOT)
        if (!outputStack.isEmpty) return

        val filterStack = inventory.getStackInSlot(FILTER_SLOT)
        val recipeManager = level.recipeManager

        @Suppress("UNCHECKED_CAST")
        val mechanicalCraftingType = net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE
            .get(net.minecraft.resources.ResourceLocation("create", "mechanical_crafting"))
            as? net.minecraft.world.item.crafting.RecipeType<MechanicalCraftingRecipe>
            ?: return

        val allRecipes = recipeManager.getAllRecipesFor(mechanicalCraftingType)

        val filteredRecipes = if (!filterStack.isEmpty) {
            allRecipes.filter { recipe ->
                val resultItem = recipe.getResultItem(level.registryAccess())
                ItemStack.isSameItemSameTags(resultItem, filterStack)
            }
        } else {
            allRecipes
        }

        val feasibleRecipes = filteredRecipes.filter { recipe -> canCraftRecipe(recipe) }

        val sortedRecipes = feasibleRecipes
            .filter { it.width <= minWidth }
            .sortedBy { abs(it.width - minWidth) }

        val recipe = sortedRecipes.firstOrNull() ?: return

        consumeAndOutput(recipe, level)
    }

    private fun canCraftRecipe(recipe: MechanicalCraftingRecipe): Boolean {
        val snapshot = createSnapshot()
        val ingredients = recipe.ingredients
        for (ingredient in ingredients) {
            if (ingredient.isEmpty) continue
            var found = false
            for (slot in 0 until INPUT_SLOTS) {
                val stack = snapshot[slot]
                if (!stack.isEmpty && ingredient.test(stack)) {
                    stack.shrink(1)
                    if (stack.isEmpty) snapshot[slot] = ItemStack.EMPTY
                    found = true
                    break
                }
            }
            if (!found) return false
        }
        return true
    }

    private fun createSnapshot(): Array<ItemStack> {
        return Array(INPUT_SLOTS) { slot ->
            inventory.getStackInSlot(slot).copy()
        }
    }

    private fun consumeAndOutput(recipe: MechanicalCraftingRecipe, level: Level) {
        val ingredients = recipe.ingredients
        for (ingredient in ingredients) {
            if (ingredient.isEmpty) continue
            for (slot in 0 until INPUT_SLOTS) {
                val stack = inventory.getStackInSlot(slot)
                if (!stack.isEmpty && ingredient.test(stack)) {
                    inventory.extractItem(slot, 1, false)
                    break
                }
            }
        }

        val width = recipe.width
        val height = recipe.height
        val craftingIngredients = mutableListOf<BigItemStack>()

        for (row in 0 until height) {
            for (col in 0 until width) {
                val idx = row * width + col
                if (idx < ingredients.size) {
                    val ingredient = ingredients[idx]
                    if (!ingredient.isEmpty) {
                        val matchingItems = ingredient.items
                        if (matchingItems.isNotEmpty()) {
                            craftingIngredients.add(BigItemStack(matchingItems[0].copy()))
                        } else {
                            craftingIngredients.add(BigItemStack(ItemStack.EMPTY))
                        }
                    } else {
                        craftingIngredients.add(BigItemStack(ItemStack.EMPTY))
                    }
                }
            }
        }

        val order = PackageOrderWithCrafts.singleRecipe(craftingIngredients)
        val resultHandler = ItemStackHandler(1)
        val resultItem = recipe.getResultItem(level.registryAccess()).copy()
        resultHandler.setStackInSlot(0, resultItem)

        val packageStack = PackageItem.containing(resultHandler)
        val tag = packageStack.getOrCreateTag()
        val fragmentTag = CompoundTag()
        fragmentTag.put("OrderContext", order.write())
        tag.put("Fragment", fragmentTag)

        inventory.setStackInSlot(OUTPUT_SLOT, packageStack)
        setChanged()
    }

    fun dropContents(level: Level, pos: BlockPos) {
        for (i in 0 until TOTAL_SLOTS) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
            }
        }
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.put("Inventory", inventory.serializeNBT())
        tag.putInt("MinWidth", minWidth)
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"))
        }
        minWidth = tag.getInt("MinWidth").coerceIn(1, 9).let { if (it == 0) DEFAULT_MIN_WIDTH else it }
    }

    override fun <T> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryCapability.cast()
        }
        return super.getCapability(cap, side)
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        inventoryCapability.invalidate()
    }
}
