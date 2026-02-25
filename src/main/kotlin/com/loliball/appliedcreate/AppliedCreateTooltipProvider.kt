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

/**
 * Registers AE2 igtooltip (Jade/WTHIT) providers for our custom pattern provider blocks.
 *
 * We cannot use [addBaseBlockEntity][appeng.api.integrations.igtooltip.BaseClassRegistration.addBaseBlockEntity]
 * because our BEs extend AEBaseBlockEntity (already registered by AE2 as a base class),
 * so BaseClassRegistrationImpl.addBaseBlockEntity skips our registration.
 * However, AE2's base class registration pairs AEBaseBlockEntity with AEBaseEntityBlock,
 * and our blocks extend plain Block — so the client-side body provider (keyed by block class)
 * never matches our blocks.
 *
 * Instead, we directly register GridNodeStateDataProvider for our specific BE+Block classes
 * via registerCommon (server data) and registerClient (client body).
 */
class AppliedCreateTooltipProvider : TooltipProvider {

    override fun registerCommon(registration: CommonRegistration) {
        val gridNodeProvider = GridNodeStateDataProvider()
        val powerProvider = PowerStorageDataProvider()

        registration.addBlockEntityData(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "grid_node"),
            AndesitePatternProviderBlockEntity::class.java,
            gridNodeProvider
        )
        registration.addBlockEntityData(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "grid_node"),
            BrassPatternProviderBlockEntity::class.java,
            gridNodeProvider
        )
        registration.addBlockEntityData(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "power_storage"),
            AndesitePatternProviderBlockEntity::class.java,
            powerProvider
        )
        registration.addBlockEntityData(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "power_storage"),
            BrassPatternProviderBlockEntity::class.java,
            powerProvider
        )
    }

    override fun registerClient(registration: ClientRegistration) {
        val gridNodeProvider = GridNodeStateDataProvider()
        val powerProvider = PowerStorageDataProvider()

        registration.addBlockEntityBody(
            AndesitePatternProviderBlockEntity::class.java,
            AndesitePatternProviderBlock::class.java,
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "grid_node_state"),
            gridNodeProvider
        )
        registration.addBlockEntityBody(
            BrassPatternProviderBlockEntity::class.java,
            BrassPatternProviderBlock::class.java,
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "grid_node_state"),
            gridNodeProvider
        )
        registration.addBlockEntityBody(
            AndesitePatternProviderBlockEntity::class.java,
            AndesitePatternProviderBlock::class.java,
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "power_storage"),
            powerProvider
        )
        registration.addBlockEntityBody(
            BrassPatternProviderBlockEntity::class.java,
            BrassPatternProviderBlock::class.java,
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "power_storage"),
            powerProvider
        )
    }
}
