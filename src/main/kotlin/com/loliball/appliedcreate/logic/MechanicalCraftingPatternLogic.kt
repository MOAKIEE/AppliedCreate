package com.loliball.appliedcreate.logic

import appeng.api.config.LockCraftingMode
import appeng.api.crafting.IPatternDetails
import appeng.api.networking.IManagedGridNode
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.KeyCounter
import appeng.helpers.patternprovider.PatternProviderLogic
import appeng.helpers.patternprovider.PatternProviderLogicHost
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import java.lang.reflect.Method

/**
 * Extends AE2's PatternProviderLogic to add Create mechanical crafter support.
 *
 * When pushPattern is called, this logic first attempts to push items into
 * adjacent Create Mechanical Crafter chains. If no suitable crafter chain is
 * found, it falls back to the standard AE2 pattern provider behavior (generic
 * inventories, blocking mode, round-robin, etc.).
 */
class MechanicalCraftingPatternLogic(
    mainNode: IManagedGridNode,
    host: PatternProviderLogicHost,
    patternInventorySize: Int
) : PatternProviderLogic(mainNode, host, patternInventorySize) {

    private val hostRef: PatternProviderLogicHost = host

    companion object {
        private val onPushPatternSuccessMethod: Method? by lazy {
            try {
                PatternProviderLogic::class.java
                    .getDeclaredMethod("onPushPatternSuccess", IPatternDetails::class.java)
                    .also { it.isAccessible = true }
            } catch (e: Exception) {
                null
            }
        }

        private val mainNodeField by lazy {
            try {
                PatternProviderLogic::class.java
                    .getDeclaredField("mainNode")
                    .also { it.isAccessible = true }
            } catch (e: Exception) {
                null
            }
        }

        private val sendListField by lazy {
            try {
                PatternProviderLogic::class.java
                    .getDeclaredField("sendList")
                    .also { it.isAccessible = true }
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun pushPattern(patternDetails: IPatternDetails, inputHolder: Array<KeyCounter>): Boolean {
        // Check sendList is empty (parent checks this too)
        val sendList = try {
            sendListField?.get(this) as? List<*>
        } catch (e: Exception) {
            null
        }
        if (sendList != null && sendList.isNotEmpty()) {
            return false
        }

        // Check mainNode is active
        val mainNode = try {
            mainNodeField?.get(this) as? IManagedGridNode
        } catch (e: Exception) {
            null
        }
        if (mainNode != null && !mainNode.isActive) {
            return false
        }

        // Check pattern is known
        if (!this.availablePatterns.contains(patternDetails)) {
            return false
        }

        if (getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false
        }

        // Try to push to mechanical crafters first
        val be = hostRef.blockEntity
        val level = be.level ?: return super.pushPattern(patternDetails, inputHolder)

        val outputKey = patternDetails.primaryOutput?.what as? AEItemKey
        if (outputKey != null) {
            val outputStack = outputKey.toStack()

            @Suppress("UNCHECKED_CAST")
            val recipeType = BuiltInRegistries.RECIPE_TYPE
                .get(ResourceLocation("create", "mechanical_crafting"))
                    as? RecipeType<MechanicalCraftingRecipe>

            if (recipeType != null) {
                val recipes = level.recipeManager.getAllRecipesFor(recipeType)
                val recipe = recipes.firstOrNull {
                    ItemStack.isSameItemSameTags(it.getResultItem(level.registryAccess()), outputStack)
                }

                if (recipe != null) {
                    val targets = hostRef.targets
                    for (direction in targets) {
                        val neighborPos = be.blockPos.relative(direction)
                        val neighbor = level.getBlockEntity(neighborPos)
                        if (neighbor is MechanicalCrafterBlockEntity) {
                            val crafters = RecipeGridHandler.getAllCraftersOfChain(neighbor)
                            if (crafters.isEmpty()) continue

                            val recipeWidth = recipe.width
                            val recipeHeight = recipe.height
                            if (crafters.size < recipeWidth * recipeHeight) continue

                            // Collect input stacks
                            val inputStacks = ArrayList<ItemStack>()
                            for (input in inputHolder) {
                                for (entry in input) {
                                    val key = entry.key
                                    if (key is AEItemKey) {
                                        inputStacks.add(key.toStack(entry.longValue.toInt()))
                                    }
                                }
                            }

                            // Simulation pass: verify all slots can accept items
                            val slotAssignments = ArrayList<Pair<Int, ItemStack>>()
                            val tempInputs = ArrayList(inputStacks)
                            var canFit = true

                            for (i in 0 until recipeWidth * recipeHeight) {
                                if (i >= crafters.size) {
                                    canFit = false
                                    break
                                }

                                val ingredient = recipe.ingredients.getOrNull(i)
                                if (ingredient != null && !ingredient.isEmpty) {
                                    val matchIndex = tempInputs.indexOfFirst { ingredient.test(it) }
                                    if (matchIndex == -1) {
                                        canFit = false
                                        break
                                    }

                                    // Simulate insertion
                                    val stackToInsert = tempInputs[matchIndex]
                                    val remainder = crafters[i].inventory.insertItem(0, stackToInsert.copy(), true)
                                    if (!remainder.isEmpty) {
                                        canFit = false
                                        break
                                    }

                                    slotAssignments.add(i to stackToInsert.copy())
                                    tempInputs.removeAt(matchIndex)
                                }
                            }

                            if (!canFit) continue

                            // Actual insertion pass (atomic: we verified all slots above)
                            for ((crafterIndex, stack) in slotAssignments) {
                                val singleStack = stack.copy()
                                singleStack.count = 1
                                crafters[crafterIndex].inventory.insertItem(0, singleStack, false)
                            }

                            // Call onPushPatternSuccess via reflection to handle lock crafting
                            try {
                                onPushPatternSuccessMethod?.invoke(this, patternDetails)
                            } catch (e: Exception) {
                                // If reflection fails, at least reset the lock manually
                                resetCraftingLock()
                            }

                            return true
                        }
                    }
                }
            }
        }

        // Fall back to standard AE2 pattern provider behavior
        // (handles generic inventories, blocking mode, round-robin, etc.)
        return super.pushPattern(patternDetails, inputHolder)
    }
}
