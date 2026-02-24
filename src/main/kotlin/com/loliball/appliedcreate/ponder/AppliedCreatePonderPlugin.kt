package com.loliball.appliedcreate.ponder

import com.loliball.appliedcreate.AppliedCreate
import net.createmod.ponder.api.registration.PonderPlugin
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper
import net.minecraft.resources.ResourceLocation

class AppliedCreatePonderPlugin : PonderPlugin {

    companion object {
        @Suppress("DEPRECATION")
        val APPLIED_CREATE_TAG: ResourceLocation = ResourceLocation(AppliedCreate.MOD_ID, "applied_create")
    }

    override fun getModId(): String = AppliedCreate.MOD_ID

    override fun registerScenes(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
        helper.forComponents(AppliedCreate.ANDESITE_PATTERN_PROVIDER_BLOCK.id)
            .addStoryBoard("andesite_pattern_provider", PatternProviderScenes::andesitePatternProvider, APPLIED_CREATE_TAG)

        helper.forComponents(AppliedCreate.BRASS_PATTERN_PROVIDER_BLOCK.id)
            .addStoryBoard("brass_pattern_provider", PatternProviderScenes::brassPatternProvider, APPLIED_CREATE_TAG)
    }

    override fun registerTags(helper: PonderTagRegistrationHelper<ResourceLocation>) {
        helper.registerTag(APPLIED_CREATE_TAG)
            .title("Applied Create")
            .description("Blocks from the Applied Create mod")
            .item(AppliedCreate.ANDESITE_PATTERN_PROVIDER_ITEM.get())
            .addToIndex()
            .register()

        helper.addToTag(APPLIED_CREATE_TAG)
            .add(AppliedCreate.ANDESITE_PATTERN_PROVIDER_BLOCK.id)
            .add(AppliedCreate.BRASS_PATTERN_PROVIDER_BLOCK.id)
    }
}
