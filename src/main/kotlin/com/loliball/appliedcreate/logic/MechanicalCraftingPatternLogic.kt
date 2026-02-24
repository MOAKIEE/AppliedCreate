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
import net.minecraft.world.item.crafting.RecipeType
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
                            if (crafters.isNullOrEmpty()) continue

                            val recipeWidth = recipe.width
                            val recipeHeight = recipe.height

                            val gridMap = computeCrafterGridPositions(crafters) ?: continue

                            val gridWidth = (gridMap.keys.maxOfOrNull { it.first } ?: 0) + 1
                            val gridHeight = (gridMap.keys.maxOfOrNull { it.second } ?: 0) + 1
                            if (gridWidth != recipeWidth || gridHeight != recipeHeight) continue

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

                            // Coordinate mapping: recipe ingredients[col + row*width] (row=0 = top)
                            // vs Create grid where UP→yOffset=-1 (higher crafters = lower gridY).
                            // MechanicalCraftingInventory inverts Y: slot = x + (height-1-y)*width
                            // Result: gridY = (recipeHeight - 1) - row, gridX = col

                            data class SlotAssignment(
                                val crafter: MechanicalCrafterBlockEntity,
                                val stack: ItemStack
                            )

                            val slotAssignments = ArrayList<SlotAssignment>()
                            val tempInputs = ArrayList(inputStacks)
                            var canFit = true

                            for (row in 0 until recipeHeight) {
                                for (col in 0 until recipeWidth) {
                                    val ingredientIndex = col + row * recipeWidth
                                    val ingredient = recipe.ingredients.getOrNull(ingredientIndex)

                                    if (ingredient == null || ingredient.isEmpty) continue

                                    val gridX = col
                                    val gridY = (recipeHeight - 1) - row

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

                            for (assignment in slotAssignments) {
                                assignment.crafter.inventory.insertItem(0, assignment.stack, false)
                            }

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

        return super.pushPattern(patternDetails, inputHolder)
    }
}
