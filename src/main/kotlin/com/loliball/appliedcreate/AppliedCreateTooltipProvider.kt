package com.loliball.appliedcreate

import appeng.api.integrations.igtooltip.ClientRegistration
import appeng.api.integrations.igtooltip.CommonRegistration
import appeng.api.integrations.igtooltip.TooltipProvider
import appeng.api.integrations.igtooltip.PartTooltips
import appeng.api.integrations.igtooltip.TooltipBuilder
import appeng.api.integrations.igtooltip.TooltipContext
import appeng.api.integrations.igtooltip.providers.BodyProvider
import appeng.api.integrations.igtooltip.providers.ServerDataProvider
import appeng.integration.modules.igtooltip.blocks.GridNodeStateDataProvider
import appeng.integration.modules.igtooltip.blocks.PowerStorageDataProvider
import com.loliball.appliedcreate.block.AndesitePatternProviderBlock
import com.loliball.appliedcreate.block.BrassPatternProviderBlock
import com.loliball.appliedcreate.block.entity.AndesitePatternProviderBlockEntity
import com.loliball.appliedcreate.block.entity.BrassPatternProviderBlockEntity
import net.minecraft.resources.ResourceLocation
import com.loliball.appliedcreate.energy.KineticEnergyAcceptorBlock
import com.loliball.appliedcreate.energy.KineticEnergyAcceptorBlockEntity
import com.loliball.appliedcreate.energy.MEGearboxBlock
import com.loliball.appliedcreate.energy.MEGearboxBlockEntity
import com.loliball.appliedcreate.p2p.StressP2PTunnelPart

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
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "andesite_pp_grid_node"),
            AndesitePatternProviderBlockEntity::class.java,
            gridNodeProvider
        )
        registration.addBlockEntityData(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "brass_pp_grid_node"),
            BrassPatternProviderBlockEntity::class.java,
            gridNodeProvider
        )
        registration.addBlockEntityData(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "andesite_pp_power_storage"),
            AndesitePatternProviderBlockEntity::class.java,
            powerProvider
        )
        registration.addBlockEntityData(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "brass_pp_power_storage"),
            BrassPatternProviderBlockEntity::class.java,
            powerProvider
        )

        // Kinetic Energy Acceptor
        registration.addBlockEntityData(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "kea_grid_node"),
            KineticEnergyAcceptorBlockEntity::class.java,
            gridNodeProvider
        )

        // ME Gearbox
        registration.addBlockEntityData(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "me_gearbox_grid_node"),
            MEGearboxBlockEntity::class.java,
            gridNodeProvider
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

        // Kinetic Energy Acceptor
        registration.addBlockEntityBody(
            KineticEnergyAcceptorBlockEntity::class.java,
            KineticEnergyAcceptorBlock::class.java,
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "grid_node_state"),
            gridNodeProvider
        )

        // ME Gearbox
        registration.addBlockEntityBody(
            MEGearboxBlockEntity::class.java,
            MEGearboxBlock::class.java,
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "grid_node_state"),
            gridNodeProvider
        )
    }
}
