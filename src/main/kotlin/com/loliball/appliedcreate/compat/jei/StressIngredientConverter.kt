package com.loliball.appliedcreate.compat.jei

import appeng.api.stacks.GenericStack
import com.loliball.appliedcreate.storage.StressKey
import com.loliball.appliedcreate.storage.StressKeyType
import mezz.jei.api.ingredients.IIngredientType
import tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverter

class StressIngredientConverter(
    private val type: IIngredientType<StressKeyType>
) : IngredientConverter<StressKeyType> {

    override fun getIngredientType(): IIngredientType<StressKeyType> = type

    override fun getIngredientFromStack(stack: GenericStack): StressKeyType? {
        if (stack.what() is StressKey && type.ingredientClass.isInstance(StressKeyType.TYPE)) {
            @Suppress("UNCHECKED_CAST")
            return StressKeyType.TYPE
        }
        return null
    }

    override fun getStackFromIngredient(ingredient: StressKeyType): GenericStack {
        return GenericStack(StressKey.INSTANCE, 1)
    }
}
