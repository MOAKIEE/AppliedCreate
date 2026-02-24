package com.loliball.appliedcreate.logic

import appeng.api.config.LockCraftingMode
import appeng.api.crafting.IPatternDetails
import appeng.api.networking.IManagedGridNode
import appeng.api.stacks.AEItemKey
import appeng.api.stacks.KeyCounter
import appeng.helpers.patternprovider.PatternProviderLogic
import appeng.helpers.patternprovider.PatternProviderLogicHost
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.ShapedRecipe
import net.minecraft.world.item.crafting.RecipeHolder
import java.lang.reflect.Method

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

    /**
     * Compute grid positions for all crafters in a chain by replicating
     * Create's GroupedItems.mergeOnto() coordinate system.
     *
     * Pointing offset formula: LEFT→(+1,0) RIGHT→(-1,0) DOWN→(0,+1) UP→(0,-1)
     *
     * Returns (gridX, gridY) → crafter map normalized to (0,0) origin,
     * or null if chain is invalid.
     */
    private fun computeCrafterGridPositions(
        crafters: List<MechanicalCrafterBlockEntity>
    ): Map<Pair<Int, Int>, MechanicalCrafterBlockEntity>? {
        if (crafters.isEmpty()) return null

        val crafterSet = crafters.toSet()
        val targetMap = HashMap<MechanicalCrafterBlockEntity, MechanicalCrafterBlockEntity?>()
        for (crafter in crafters) {
            val target = RecipeGridHandler.getTargetingCrafter(crafter)
            targetMap[crafter] = if (target != null && target in crafterSet) target else null
        }

        val terminal = crafters.firstOrNull { targetMap[it] == null } ?: return null

        val gridPositions = HashMap<MechanicalCrafterBlockEntity, Pair<Int, Int>>()
        gridPositions[terminal] = Pair(0, 0)

        val queue = ArrayDeque<MechanicalCrafterBlockEntity>()
        queue.add(terminal)
        val visited = HashSet<MechanicalCrafterBlockEntity>()
        visited.add(terminal)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val currentPos = gridPositions[current]!!

            for (crafter in crafters) {
                if (crafter in visited) continue
                if (targetMap[crafter] != current) continue

                val facing = crafter.blockState.getValue(
                    com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING
                )
                val targetDir = MechanicalCrafterBlock.getTargetDirection(crafter.blockState)
                val pointingName = when (targetDir) {
                    net.minecraft.core.Direction.UP -> "up"
                    net.minecraft.core.Direction.DOWN -> "down"
                    else -> when (facing) {
                        net.minecraft.core.Direction.SOUTH -> if (targetDir == net.minecraft.core.Direction.EAST) "left" else "right"
                        net.minecraft.core.Direction.NORTH -> if (targetDir == net.minecraft.core.Direction.WEST) "left" else "right"
                        net.minecraft.core.Direction.EAST -> if (targetDir == net.minecraft.core.Direction.SOUTH) "left" else "right"
                        net.minecraft.core.Direction.WEST -> if (targetDir == net.minecraft.core.Direction.NORTH) "left" else "right"
                        else -> "up"
                    }
                }
                val xOffset = when (pointingName) {
                    "left" -> 1
                    "right" -> -1
                    else -> 0
                }
                val yOffset = when (pointingName) {
                    "down" -> 1
                    "up" -> -1
                    else -> 0
                }

                gridPositions[crafter] = Pair(currentPos.first + xOffset, currentPos.second + yOffset)
                visited.add(crafter)
                queue.add(crafter)
            }
        }

        if (gridPositions.size != crafters.size) return null

        val minX = gridPositions.values.minOf { it.first }
        val minY = gridPositions.values.minOf { it.second }

        val result = HashMap<Pair<Int, Int>, MechanicalCrafterBlockEntity>()
        for ((crafter, pos) in gridPositions) {
            val normalizedPos = Pair(pos.first - minX, pos.second - minY)
            if (normalizedPos in result) return null
            result[normalizedPos] = crafter
        }

        return result
    }

    override fun pushPattern(patternDetails: IPatternDetails, inputHolder: Array<KeyCounter>): Boolean {
        val sendList = try {
            sendListField?.get(this) as? List<*>
        } catch (e: Exception) {
            null
        }
        if (sendList != null && sendList.isNotEmpty()) {
            return false
        }

        val mainNode = try {
            mainNodeField?.get(this) as? IManagedGridNode
        } catch (e: Exception) {
            null
        }
        if (mainNode != null && !mainNode.isActive) {
            return false
        }

        if (!this.availablePatterns.contains(patternDetails)) {
            return false
        }

        if (getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false
        }

        val be = hostRef.blockEntity
        val level = be.level ?: return super.pushPattern(patternDetails, inputHolder)

        val outputKey = patternDetails.primaryOutput?.what as? AEItemKey
        if (outputKey != null) {
            val outputStack = outputKey.toStack()
            val registryAccess = level.registryAccess()

            // Data class to hold recipe info uniformly for both recipe types
            data class RecipeInfo(
                val width: Int,
                val height: Int,
                val ingredients: List<Ingredient> // size = width * height, may contain EMPTY
            )

            val candidateRecipes = ArrayList<RecipeInfo>()

            // 1. Search MechanicalCraftingRecipe
            @Suppress("UNCHECKED_CAST")
            val mcRecipeType = BuiltInRegistries.RECIPE_TYPE
                .get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_crafting"))
                    as? RecipeType<MechanicalCraftingRecipe>

            if (mcRecipeType != null) {
                val mcRecipes = level.recipeManager.getAllRecipesFor(mcRecipeType)
                for (recipeHolder in mcRecipes) {
                    val recipe = recipeHolder.value()
                    if (ItemStack.isSameItemSameComponents(recipe.getResultItem(registryAccess), outputStack)) {
                        candidateRecipes.add(RecipeInfo(
                            width = recipe.width,
                            height = recipe.height,
                            ingredients = recipe.ingredients
                        ))
                    }
                }
            }

            // 2. Search vanilla ShapedRecipe (covers iron boots, iron trapdoor, etc.)
            // 2. Search vanilla ShapedRecipe (covers iron boots, iron trapdoor, etc.)
            val craftingRecipes = level.recipeManager.getAllRecipesFor(RecipeType.CRAFTING)
            for (recipeHolder in craftingRecipes) {
                val recipe = recipeHolder.value()
                if (recipe is ShapedRecipe) {
                    if (ItemStack.isSameItemSameComponents(recipe.getResultItem(registryAccess), outputStack)) {
                        candidateRecipes.add(RecipeInfo(
                            width = recipe.width,
                            height = recipe.height,
                            ingredients = recipe.ingredients
                        ))
                    }
                }
            }

            if (candidateRecipes.isNotEmpty()) {
                // Build input stacks from AE2 inputHolder
                val inputStacks = ArrayList<ItemStack>()
                for (input in inputHolder) {
                    for (entry in input) {
                        val key = entry.key
                        if (key is AEItemKey) {
                            repeat(entry.longValue.toInt()) {
                                inputStacks.add(key.toStack(1))
                            }
                        }
                    }
                }

                data class SlotAssignment(
                    val crafter: MechanicalCrafterBlockEntity,
                    val stack: ItemStack
                )

                // Try each adjacent crafter chain
                val targets = hostRef.targets
                for (direction in targets) {
                    val neighborPos = be.blockPos.relative(direction)
                    val neighbor = level.getBlockEntity(neighborPos)
                    if (neighbor !is MechanicalCrafterBlockEntity) continue

                    val crafters = RecipeGridHandler.getAllCraftersOfChain(neighbor)
                    if (crafters.isNullOrEmpty()) continue

                    val gridMap = computeCrafterGridPositions(crafters) ?: continue

                    val gridWidth = (gridMap.keys.maxOfOrNull { it.first } ?: 0) + 1
                    val gridHeight = (gridMap.keys.maxOfOrNull { it.second } ?: 0) + 1

                    // Try each candidate recipe at each valid sliding offset
                    for (recipeInfo in candidateRecipes) {
                        val rw = recipeInfo.width
                        val rh = recipeInfo.height

                        // Recipe must fit within grid
                        if (rw > gridWidth || rh > gridHeight) continue

                        // Try all valid placement offsets
                        for (offsetX in 0..(gridWidth - rw)) {
                            for (offsetY in 0..(gridHeight - rh)) {
                                val slotAssignments = ArrayList<SlotAssignment>()
                                val tempInputs = ArrayList(inputStacks)
                                var canFit = true

                                for (row in 0 until rh) {
                                    for (col in 0 until rw) {
                                        val ingredientIndex = col + row * rw
                                        val ingredient = recipeInfo.ingredients.getOrNull(ingredientIndex)

                                        if (ingredient == null || ingredient.isEmpty) continue

                                        // Coordinate mapping:
                                        // recipe (col, row) where row=0 is top
                                        // grid (gridX, gridY) where gridY=0 is bottom (Create's Y inversion)
                                        // MechanicalCraftingInventory: slot = x + (height-1-y)*width
                                        // So recipe row 0 (top) → gridY = gridHeight-1-offsetY
                                        //    recipe row rh-1 (bottom) → gridY = gridHeight-1-offsetY-(rh-1)
                                        val gridX = col + offsetX
                                        val gridY = (gridHeight - 1) - (row + offsetY)

                                        val crafter = gridMap[Pair(gridX, gridY)]
                                        if (crafter == null) {
                                            canFit = false
                                            break
                                        }

                                        val matchIndex = tempInputs.indexOfFirst { ingredient.test(it) }
                                        if (matchIndex == -1) {
                                            canFit = false
                                            break
                                        }

                                        val stackToInsert = tempInputs[matchIndex]

                                        val remainder = crafter.inventory.insertItem(0, stackToInsert.copy(), true)
                                        if (!remainder.isEmpty) {
                                            canFit = false
                                            break
                                        }

                                        slotAssignments.add(SlotAssignment(crafter, stackToInsert.copy()))
                                        tempInputs.removeAt(matchIndex)
                                    }
                                    if (!canFit) break
                                }

                                if (!canFit) continue

                                // All ingredients placed successfully — commit
                                for (assignment in slotAssignments) {
                                    assignment.crafter.inventory.insertItem(0, assignment.stack, false)
                                }

                                // Trigger crafting
                                crafters.firstOrNull()?.checkCompletedRecipe(true)

                                try {
                                    onPushPatternSuccessMethod?.invoke(this, patternDetails)
                                } catch (e: Exception) {
                                    resetCraftingLock()
                                }

                                return true
                            }
                        }
                    }
                }
            }

                // Found recipes matching this output but none fit any adjacent crafter grid.
                // Return false to prevent fallback to super.pushPattern() which would
                // dump items sequentially and potentially craft the wrong item.
                return false
        }

        return super.pushPattern(patternDetails, inputHolder)
    }

}