package com.loliball.appliedcreate

import appeng.api.integrations.igtooltip.ClientRegistration
import appeng.api.integrations.igtooltip.CommonRegistration
import appeng.api.integrations.igtooltip.TooltipProvider
import appeng.integration.modules.igtooltip.blocks.GridNodeStateDataProvider
import appeng.integration.modules.igtooltip.blocks.PowerStorageDataProvider
import com.loliball.appliedcreate.block.AndesitePatternProviderBlock
import com.loliball.appliedcreate.block.BrassPatternProviderBlock
import com.loliball.appliedcreate.block.entity.AndesitePatternProviderBlockEntity
import com.loliball.appliedcreate.block.entity.BrassPatternProviderBlockEntity
import net.minecraft.resources.ResourceLocation

class AppliedCreateTooltipProvider : TooltipProvider {
    override fun registerCommon(registration: CommonRegistration) {
        val gridNodeProvider = GridNodeStateDataProvider()
        val powerProvider = PowerStorageDataProvider()
        registration.addBlockEntityData(AndesitePatternProviderBlockEntity::class.java, gridNodeProvider)
        registration.addBlockEntityData(AndesitePatternProviderBlockEntity::class.java, powerProvider)
        registration.addBlockEntityData(BrassPatternProviderBlockEntity::class.java, gridNodeProvider)
        registration.addBlockEntityData(BrassPatternProviderBlockEntity::class.java, powerProvider)
    }

    override fun registerClient(registration: ClientRegistration) {
        val gridNodeProvider = GridNodeStateDataProvider()
        val powerProvider = PowerStorageDataProvider()
        registration.addBlockEntityBody(
            AndesitePatternProviderBlockEntity::class.java,
            AndesitePatternProviderBlock::class.java,
            ResourceLocation(AppliedCreate.MOD_ID, "grid_node_state"),
            gridNodeProvider
        )
        registration.addBlockEntityBody(
            AndesitePatternProviderBlockEntity::class.java,
            AndesitePatternProviderBlock::class.java,
            ResourceLocation(AppliedCreate.MOD_ID, "power_storage"),
            powerProvider
        )
        registration.addBlockEntityBody(
            BrassPatternProviderBlockEntity::class.java,
            BrassPatternProviderBlock::class.java,
            ResourceLocation(AppliedCreate.MOD_ID, "grid_node_state"),
            gridNodeProvider
        )
        registration.addBlockEntityBody(
            BrassPatternProviderBlockEntity::class.java,
            BrassPatternProviderBlock::class.java,
            ResourceLocation(AppliedCreate.MOD_ID, "power_storage"),
            powerProvider
        )
    }
}
