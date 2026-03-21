package com.loliball.appliedcreate.compat.jei

import appeng.api.integrations.jei.IngredientConverter
import appeng.api.stacks.GenericStack
import com.loliball.appliedcreate.storage.StressKey
import mezz.jei.api.ingredients.IIngredientType

object StressIngredientConverter : IngredientConverter<StressKey> {

    override fun getIngredientType(): IIngredientType<StressKey> = StressIngredientType

    override fun getIngredientFromStack(stack: GenericStack): StressKey? {
        val key = stack.what()
        return if (key is StressKey) key else null
    }

    override fun getStackFromIngredient(ingredient: StressKey): GenericStack =
        GenericStack(ingredient, 1L)
}
