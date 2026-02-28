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
import com.loliball.appliedcreate.kinetic.StressP2PCompanionBlock
import com.loliball.appliedcreate.p2p.StressP2PTunnelPart
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

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

        // Kinetic Energy Acceptor
        registration.addBlockEntityData(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "grid_node"),
            KineticEnergyAcceptorBlockEntity::class.java,
            gridNodeProvider
        )

        // ME Gearbox
        registration.addBlockEntityData(
            ResourceLocation.fromNamespaceAndPath(AppliedCreate.MOD_ID, "grid_node"),
            MEGearboxBlockEntity::class.java,
            gridNodeProvider
        )

        // Stress P2P Tunnel — companion block status (via PartTooltips)
        PartTooltips.addServerData(StressP2PTunnelPart::class.java,
            ServerDataProvider<StressP2PTunnelPart> { _, part, serverData ->
                val level = part.blockEntity.level ?: return@ServerDataProvider
                val companionPos = part.blockEntity.blockPos.relative(part.side)
                val hasCompanion = level.getBlockState(companionPos).block is StressP2PCompanionBlock
                serverData.putBoolean("HasCompanion", hasCompanion)
            }
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

        // Stress P2P Tunnel — companion block status (via PartTooltips)
        PartTooltips.addBody(StressP2PTunnelPart::class.java,
            BodyProvider<StressP2PTunnelPart> { _, context, tooltip ->
                val hasCompanion = context.serverData().getBoolean("HasCompanion")
                val key = if (hasCompanion) "appliedcreate.stress_p2p.companion.present"
                    else "appliedcreate.stress_p2p.companion.missing"
                val color = if (hasCompanion) ChatFormatting.GREEN else ChatFormatting.RED
                tooltip.addLine(Component.translatable(key).withStyle(color))
            }
        )
    }
}
