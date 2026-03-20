package com.loliball.appliedcreate.compat.jei

import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.storage.StressKeyType
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.registration.IModIngredientRegistration
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.resources.ResourceLocation
import tamaized.ae2jeiintegration.api.integrations.jei.IngredientConverters

@JeiPlugin
class AppliedCreateJeiPlugin : IModPlugin {

    override fun getPluginUid(): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "jei")

    @Suppress("removal")
    override fun registerIngredients(registration: IModIngredientRegistration) {
        registration.register(
            StressIngredientTypes.STRESS_TYPE,
            listOf(StressKeyType.TYPE),
            StressIngredientTypes.StressStackHelper(),
            StressIngredientTypes.StressStackRenderer()
        )
    }

    override fun onRuntimeAvailable(jeiRuntime: IJeiRuntime) {
        IngredientConverters.register(
            StressIngredientConverter(StressIngredientTypes.STRESS_TYPE)
        )
    }
}
