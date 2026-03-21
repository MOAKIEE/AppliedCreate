package com.loliball.appliedcreate.compat.jei

import appeng.api.integrations.jei.IngredientConverters
import com.loliball.appliedcreate.AppliedCreate
import com.loliball.appliedcreate.storage.StressKey
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.registration.IModIngredientRegistration
import mezz.jei.api.runtime.IJeiRuntime
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@JeiPlugin
@OnlyIn(Dist.CLIENT)
class AppliedCreateJeiPlugin : IModPlugin {

    override fun getPluginUid(): ResourceLocation =
        ResourceLocation(AppliedCreate.MOD_ID, "jei_plugin")

    override fun registerIngredients(registration: IModIngredientRegistration) {
        registration.register(
            StressIngredientType,
            listOf(StressKey.INSTANCE),
            StressIngredientHelper,
            StressIngredientRenderer
        )
    }

    override fun onRuntimeAvailable(runtime: IJeiRuntime) {
        IngredientConverters.register(StressIngredientConverter)
    }
}
