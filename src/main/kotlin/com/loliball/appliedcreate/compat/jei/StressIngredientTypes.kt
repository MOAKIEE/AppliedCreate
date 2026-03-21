package com.loliball.appliedcreate.compat.jei

import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.storage.StressKey
import com.loliball.appliedcreate.client.StressKeyRenderHandler
import com.loliball.appliedcreate.storage.StressKeyType
import mezz.jei.api.ingredients.IIngredientHelper
import mezz.jei.api.ingredients.IIngredientRenderer
import mezz.jei.api.ingredients.IIngredientType
import mezz.jei.api.ingredients.subtypes.UidContext
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.TooltipFlag

object StressIngredientTypes {

    @JvmField
    val STRESS_TYPE: IIngredientType<StressKeyType> = IIngredientType { StressKeyType::class.java }

    class StressStackHelper : IIngredientHelper<StressKeyType> {

        override fun getIngredientType(): IIngredientType<StressKeyType> = STRESS_TYPE

        override fun getDisplayName(ingredient: StressKeyType): String =
            Component.translatable("key.${AppliedCreate.MOD_ID}.stress").string

        @Deprecated("Deprecated in JEI API", replaceWith = ReplaceWith(""))
        @Suppress("removal")
        override fun getUniqueId(ingredient: StressKeyType, context: UidContext): String =
            "${ingredient.id.namespace}:stress"

        override fun getResourceLocation(ingredient: StressKeyType): ResourceLocation =
            ResourceLocation(ingredient.id.namespace, "stress")

        override fun copyIngredient(ingredient: StressKeyType): StressKeyType = ingredient

        override fun getErrorInfo(ingredient: StressKeyType?): String = "stress"
    }

    class StressStackRenderer : IIngredientRenderer<StressKeyType> {

        override fun render(guiGraphics: GuiGraphics, ingredient: StressKeyType) {
            StressKeyRenderHandler.drawInGui(
                Minecraft.getInstance(),
                guiGraphics,
                0, 0,
                StressKey.INSTANCE
            )
        }

        override fun getTooltip(ingredient: StressKeyType, tooltipFlag: TooltipFlag): List<Component> {
            return listOf(Component.translatable("key.${AppliedCreate.MOD_ID}.stress"))
        }
    }
}
