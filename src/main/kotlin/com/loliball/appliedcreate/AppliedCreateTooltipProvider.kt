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
import com.loliball.appliedcreate.energy.KineticEnergyAcceptorBlock
import com.loliball.appliedcreate.energy.KineticEnergyAcceptorBlockEntity
import com.loliball.appliedcreate.energy.MEGearboxBlock
import com.loliball.appliedcreate.energy.MEGearboxBlockEntity
import net.minecraft.resources.ResourceLocation

/**
 * Registers AE2 igtooltip (Jade/WTHIT) providers for our custom blocks.
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

        // Andesite Pattern Provider
        registration.addBlockEntityData(AndesitePatternProviderBlockEntity::class.java, gridNodeProvider)
        registration.addBlockEntityData(AndesitePatternProviderBlockEntity::class.java, powerProvider)

        // Brass Pattern Provider
        registration.addBlockEntityData(BrassPatternProviderBlockEntity::class.java, gridNodeProvider)
        registration.addBlockEntityData(BrassPatternProviderBlockEntity::class.java, powerProvider)

        // Kinetic Energy Acceptor
        registration.addBlockEntityData(KineticEnergyAcceptorBlockEntity::class.java, gridNodeProvider)

        // ME Gearbox
        registration.addBlockEntityData(MEGearboxBlockEntity::class.java, gridNodeProvider)
    }

    override fun registerClient(registration: ClientRegistration) {
        val gridNodeProvider = GridNodeStateDataProvider()
        val powerProvider = PowerStorageDataProvider()

        // Andesite Pattern Provider
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

        // Brass Pattern Provider
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

        // Kinetic Energy Acceptor
        registration.addBlockEntityBody(
            KineticEnergyAcceptorBlockEntity::class.java,
            KineticEnergyAcceptorBlock::class.java,
            ResourceLocation(AppliedCreate.MOD_ID, "grid_node_state"),
            gridNodeProvider
        )

        // ME Gearbox
        registration.addBlockEntityBody(
            MEGearboxBlockEntity::class.java,
            MEGearboxBlock::class.java,
            ResourceLocation(AppliedCreate.MOD_ID, "grid_node_state"),
            gridNodeProvider
        )
    }
}
