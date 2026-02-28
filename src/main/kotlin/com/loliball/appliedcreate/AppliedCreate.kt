package com.loliball.appliedcreate

import com.loliball.appliedcreate.cannon.MEBlueprintCannonBlock
import com.loliball.appliedcreate.cannon.MEBlueprintCannonBlockEntity
import com.loliball.appliedcreate.cannon.MEBlueprintCannonMenu
import com.loliball.appliedcreate.cannon.ConfigureMECannonPayload
import com.loliball.appliedcreate.energy.KineticEnergyAcceptorBlock
import com.loliball.appliedcreate.energy.KineticEnergyAcceptorBlockEntity
import com.loliball.appliedcreate.energy.MEGearboxBlock
import com.loliball.appliedcreate.energy.MEGearboxBlockEntity
import com.loliball.appliedcreate.block.BlockFumo
import net.minecraft.world.level.block.state.BlockBehaviour
import com.loliball.appliedcreate.block.AndesitePatternProviderBlock
import com.loliball.appliedcreate.block.BrassPatternProviderBlock
import com.loliball.appliedcreate.block.entity.AndesitePatternProviderBlockEntity
import com.loliball.appliedcreate.block.entity.BrassPatternProviderBlockEntity

import com.loliball.appliedcreate.gui.BrassPatternProviderMenu
import com.loliball.appliedcreate.item.BrassPatternProviderUpgradeItem
import com.loliball.appliedcreate.item.MechanicalCraftingPartItem
import com.loliball.appliedcreate.item.StressP2PPartItem
import com.loliball.appliedcreate.kinetic.StressP2PCompanionBlock
import com.loliball.appliedcreate.kinetic.StressP2PCompanionBlockEntity
import com.loliball.appliedcreate.part.AndesitePatternProviderPart
import com.loliball.appliedcreate.part.BrassPatternProviderPart
import com.loliball.appliedcreate.p2p.StressP2PTunnelPart
import com.loliball.appliedcreate.storage.StressKeyType
import com.loliball.appliedcreate.storage.StressStorageCell
import appeng.api.AECapabilities
import appeng.api.features.P2PTunnelAttunement
import appeng.api.stacks.AEKeyTypes
import appeng.blockentity.AEBaseBlockEntity
import appeng.helpers.patternprovider.PatternProviderLogicHost
import appeng.core.definitions.AEItems
import appeng.menu.MenuOpener
import appeng.menu.locator.MenuHostLocator
import appeng.menu.locator.MenuLocators
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS

@Mod(AppliedCreate.MOD_ID)
class AppliedCreate {
    companion object {
        const val MOD_ID = "appliedcreate"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)

        val BLOCKS: DeferredRegister<Block> = DeferredRegister.createBlocks(MOD_ID)
        val ITEMS: DeferredRegister<Item> = DeferredRegister.createItems(MOD_ID)
        val BLOCK_ENTITY_TYPES: DeferredRegister<BlockEntityType<*>> = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID)
        val MENU_TYPES: DeferredRegister<MenuType<*>> = DeferredRegister.create(Registries.MENU, MOD_ID)
        val CREATIVE_TABS: DeferredRegister<CreativeModeTab> = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID)


        // ── Andesite Pattern Provider ──
        val ANDESITE_PATTERN_PROVIDER_BLOCK: DeferredHolder<Block, Block> = BLOCKS.register("andesite_pattern_provider") { ->
            AndesitePatternProviderBlock()
        }

        val ANDESITE_PATTERN_PROVIDER_ITEM: DeferredHolder<Item, Item> = ITEMS.register("andesite_pattern_provider") { ->
            BlockItem(ANDESITE_PATTERN_PROVIDER_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val ANDESITE_PATTERN_PROVIDER_BE: DeferredHolder<BlockEntityType<*>, BlockEntityType<AndesitePatternProviderBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("andesite_pattern_provider") { ->
                BlockEntityType.Builder.of(
                    ::AndesitePatternProviderBlockEntity,
                    ANDESITE_PATTERN_PROVIDER_BLOCK.get()
                ).build(null)
            }

        // ── Brass Pattern Provider ──
        val BRASS_PATTERN_PROVIDER_BLOCK: DeferredHolder<Block, Block> = BLOCKS.register("brass_pattern_provider") { ->
            BrassPatternProviderBlock()
        }

        val BRASS_PATTERN_PROVIDER_ITEM: DeferredHolder<Item, Item> = ITEMS.register("brass_pattern_provider") { ->
            BlockItem(BRASS_PATTERN_PROVIDER_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val BRASS_PATTERN_PROVIDER_BE: DeferredHolder<BlockEntityType<*>, BlockEntityType<BrassPatternProviderBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("brass_pattern_provider") { ->
                BlockEntityType.Builder.of(
                    ::BrassPatternProviderBlockEntity,
                    BRASS_PATTERN_PROVIDER_BLOCK.get()
                ).build(null)
            }

        val BRASS_PATTERN_PROVIDER_MENU: DeferredHolder<MenuType<*>, MenuType<BrassPatternProviderMenu>> =
            MENU_TYPES.register("brass_pattern_provider") { ->
                createPatternProviderMenuType { menuType, windowId, inv, host ->
                    BrassPatternProviderMenu(menuType, windowId, inv, host)
                }
            }

        // ── Items ──
        val BRASS_PATTERN_PROVIDER_UPGRADE_ITEM: DeferredHolder<Item, Item> = ITEMS.register("brass_pattern_provider_upgrade") { ->
            BrassPatternProviderUpgradeItem()
        }

        val ANDESITE_PATTERN_PROVIDER_PART_ITEM: DeferredHolder<Item, Item> = ITEMS.register("andesite_pattern_provider_part") { ->
            MechanicalCraftingPartItem(
                Item.Properties(),
                AndesitePatternProviderPart::class.java
            ) { partItem -> AndesitePatternProviderPart(partItem) }
        }

        val BRASS_PATTERN_PROVIDER_PART_ITEM: DeferredHolder<Item, Item> = ITEMS.register("brass_pattern_provider_part") { ->
            MechanicalCraftingPartItem(
                Item.Properties(),
                BrassPatternProviderPart::class.java
            ) { partItem -> BrassPatternProviderPart(partItem) }
        }

        // ── Stress P2P Tunnel ──
        val STRESS_P2P_TUNNEL_PART_ITEM: DeferredHolder<Item, Item> = ITEMS.register("stress_p2p_tunnel") { ->
            StressP2PPartItem(
                Item.Properties(),
                StressP2PTunnelPart::class.java
            ) { partItem -> StressP2PTunnelPart(partItem) }
        }

        // ── Stress P2P Companion Block ──
        val STRESS_P2P_COMPANION_BLOCK: DeferredHolder<Block, Block> = BLOCKS.register("stress_p2p_companion") { ->
            StressP2PCompanionBlock()
        }

        val STRESS_P2P_COMPANION_ITEM: DeferredHolder<Item, Item> = ITEMS.register("stress_p2p_companion") { ->
            BlockItem(STRESS_P2P_COMPANION_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val STRESS_P2P_COMPANION_BE: DeferredHolder<BlockEntityType<*>, BlockEntityType<StressP2PCompanionBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("stress_p2p_companion") { ->
                BlockEntityType.Builder.of(
                    { pos, state -> StressP2PCompanionBlockEntity(STRESS_P2P_COMPANION_BE.get(), pos, state) },
                    STRESS_P2P_COMPANION_BLOCK.get()
                ).build(null)
            }

        // ── ME Blueprint Cannon ──
        val ME_BLUEPRINT_CANNON_BLOCK: DeferredHolder<Block, Block> = BLOCKS.register("me_blueprint_cannon") { ->
            MEBlueprintCannonBlock(BlockBehaviour.Properties.of().strength(3.5f).noOcclusion())
        }

        val ME_BLUEPRINT_CANNON_ITEM: DeferredHolder<Item, Item> = ITEMS.register("me_blueprint_cannon") { ->
            BlockItem(ME_BLUEPRINT_CANNON_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val ME_BLUEPRINT_CANNON_BE: DeferredHolder<BlockEntityType<*>, BlockEntityType<MEBlueprintCannonBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("me_blueprint_cannon") { ->
                BlockEntityType.Builder.of(
                    { pos, state -> MEBlueprintCannonBlockEntity(ME_BLUEPRINT_CANNON_BE.get(), pos, state) },
                    ME_BLUEPRINT_CANNON_BLOCK.get()
                ).build(null)
            }

        val ME_BLUEPRINT_CANNON_MENU: DeferredHolder<MenuType<*>, MenuType<MEBlueprintCannonMenu>> =
            MENU_TYPES.register("me_blueprint_cannon") { ->
                net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create { windowId, inv, buf ->
                    // Correctly handle the late initialization of MENU_TYPES
                    MEBlueprintCannonMenu(MENU_TYPES.getEntries().find { it.key!!.location().path == "me_blueprint_cannon" }!!.get() as MenuType<*>, windowId, inv, buf)
                }
            }

        // ── Kinetic Energy Acceptor ──
        val KINETIC_ENERGY_ACCEPTOR_BLOCK: DeferredHolder<Block, Block> = BLOCKS.register("kinetic_energy_acceptor") { ->
            KineticEnergyAcceptorBlock()
        }

        val KINETIC_ENERGY_ACCEPTOR_ITEM: DeferredHolder<Item, Item> = ITEMS.register("kinetic_energy_acceptor") { ->
            BlockItem(KINETIC_ENERGY_ACCEPTOR_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val KINETIC_ENERGY_ACCEPTOR_BE: DeferredHolder<BlockEntityType<*>, BlockEntityType<KineticEnergyAcceptorBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("kinetic_energy_acceptor") { ->
                BlockEntityType.Builder.of(
                    { pos, state -> KineticEnergyAcceptorBlockEntity(KINETIC_ENERGY_ACCEPTOR_BE.get(), pos, state) },
                    KINETIC_ENERGY_ACCEPTOR_BLOCK.get()
                ).build(null)
            }

        // ── ME Gearbox ──
        val ME_GEARBOX_BLOCK: DeferredHolder<Block, Block> = BLOCKS.register("me_gearbox") { ->
            MEGearboxBlock()
        }

        val ME_GEARBOX_ITEM: DeferredHolder<Item, Item> = ITEMS.register("me_gearbox") { ->
            BlockItem(ME_GEARBOX_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val ME_GEARBOX_BE: DeferredHolder<BlockEntityType<*>, BlockEntityType<MEGearboxBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("me_gearbox") { ->
                BlockEntityType.Builder.of(
                    { pos, state -> MEGearboxBlockEntity(ME_GEARBOX_BE.get(), pos, state) },
                    ME_GEARBOX_BLOCK.get()
                ).build(null)
            }

        // ── 小萝卜 (Fumo Doll) ──
        val WHICHBALL_SKIN_DOLL_BLOCK: DeferredHolder<Block, Block> = BLOCKS.register("whichball_skin_doll") { ->
            BlockFumo()
        }

        val WHICHBALL_SKIN_DOLL_ITEM: DeferredHolder<Item, Item> = ITEMS.register("whichball_skin_doll") { ->
            BlockItem(WHICHBALL_SKIN_DOLL_BLOCK.get(), Item.Properties())
        }
        // ── Stress Storage Cells ──
        val STRESS_CELL_1K: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_cell_1k") { ->
            StressStorageCell(Item.Properties(), 0.5, 1, 8, 1)
        }
        val STRESS_CELL_4K: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_cell_4k") { ->
            StressStorageCell(Item.Properties(), 1.0, 4, 8, 1)
        }
        val STRESS_CELL_16K: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_cell_16k") { ->
            StressStorageCell(Item.Properties(), 1.5, 16, 8, 1)
        }
        val STRESS_CELL_64K: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_cell_64k") { ->
            StressStorageCell(Item.Properties(), 2.0, 64, 8, 1)
        }
        val STRESS_CELL_256K: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_cell_256k") { ->
            StressStorageCell(Item.Properties(), 2.5, 256, 8, 1)
        }
        val STRESS_CELL_1M: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_cell_1m") { ->
            StressStorageCell(Item.Properties(), 3.0, 1024, 8, 1)
        }
        val STRESS_CELL_4M: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_cell_4m") { ->
            StressStorageCell(Item.Properties(), 3.5, 4096, 8, 1)
        }
        val STRESS_CELL_16M: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_cell_16m") { ->
            StressStorageCell(Item.Properties(), 4.0, 16384, 8, 1)
        }
        val STRESS_CELL_64M: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_cell_64m") { ->
            StressStorageCell(Item.Properties(), 4.5, 65536, 8, 1)
        }
        val STRESS_CELL_256M: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_cell_256m") { ->
            StressStorageCell(Item.Properties(), 5.0, 262144, 8, 1)
        }

        val STRESS_CELLS: List<DeferredHolder<Item, Item>> by lazy {
            listOf(
                STRESS_CELL_1K, STRESS_CELL_4K, STRESS_CELL_16K, STRESS_CELL_64K, STRESS_CELL_256K,
                STRESS_CELL_1M, STRESS_CELL_4M, STRESS_CELL_16M, STRESS_CELL_64M, STRESS_CELL_256M
            )
        }

        // ── Stress Crafting Items ──
        // Circuit boards (inscribed)
        val STRESS_CIRCUIT_BOARD: DeferredHolder<Item, Item> = ITEMS.register("stress_circuit_board") { ->
            Item(Item.Properties())
        }
        val ADVANCED_STRESS_CIRCUIT_BOARD: DeferredHolder<Item, Item> = ITEMS.register("advanced_stress_circuit_board") { ->
            Item(Item.Properties())
        }

        // Processors (assembled from circuit board + silicon + redstone)
        val STRESS_PROCESSOR: DeferredHolder<Item, Item> = ITEMS.register("stress_processor") { ->
            Item(Item.Properties())
        }
        val ADVANCED_STRESS_PROCESSOR: DeferredHolder<Item, Item> = ITEMS.register("advanced_stress_processor") { ->
            Item(Item.Properties())
        }

        // Storage components (crafted with processors, following AE2 component pattern)
        val STRESS_COMPONENT_1K: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_component_1k") { ->
            Item(Item.Properties())
        }
        val STRESS_COMPONENT_4K: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_component_4k") { ->
            Item(Item.Properties())
        }
        val STRESS_COMPONENT_16K: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_component_16k") { ->
            Item(Item.Properties())
        }
        val STRESS_COMPONENT_64K: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_component_64k") { ->
            Item(Item.Properties())
        }
        val STRESS_COMPONENT_256K: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_component_256k") { ->
            Item(Item.Properties())
        }
        val STRESS_COMPONENT_1M: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_component_1m") { ->
            Item(Item.Properties())
        }
        val STRESS_COMPONENT_4M: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_component_4m") { ->
            Item(Item.Properties())
        }
        val STRESS_COMPONENT_16M: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_component_16m") { ->
            Item(Item.Properties())
        }
        val STRESS_COMPONENT_64M: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_component_64m") { ->
            Item(Item.Properties())
        }
        val STRESS_COMPONENT_256M: DeferredHolder<Item, Item> = ITEMS.register("stress_storage_component_256m") { ->
            Item(Item.Properties())
        }

        // Cell housings
        val ANDESITE_STRESS_CELL_HOUSING: DeferredHolder<Item, Item> = ITEMS.register("andesite_stress_cell_housing") { ->
            Item(Item.Properties())
        }
        val BRASS_STRESS_CELL_HOUSING: DeferredHolder<Item, Item> = ITEMS.register("brass_stress_cell_housing") { ->
            Item(Item.Properties())
        }

        val STRESS_COMPONENTS: List<DeferredHolder<Item, Item>> by lazy {
            listOf(
                STRESS_COMPONENT_1K, STRESS_COMPONENT_4K, STRESS_COMPONENT_16K, STRESS_COMPONENT_64K, STRESS_COMPONENT_256K,
                STRESS_COMPONENT_1M, STRESS_COMPONENT_4M, STRESS_COMPONENT_16M, STRESS_COMPONENT_64M, STRESS_COMPONENT_256M
            )
        }

        // ── Creative Tab ──
        val CREATIVE_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> = CREATIVE_TABS.register("main") { ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.$MOD_ID"))
                .icon { ANDESITE_PATTERN_PROVIDER_ITEM.get().defaultInstance }
                .displayItems { _, output ->
                    output.accept(ANDESITE_PATTERN_PROVIDER_ITEM.get())
                    output.accept(BRASS_PATTERN_PROVIDER_ITEM.get())
                    output.accept(ANDESITE_PATTERN_PROVIDER_PART_ITEM.get())
                    output.accept(BRASS_PATTERN_PROVIDER_PART_ITEM.get())
                    output.accept(BRASS_PATTERN_PROVIDER_UPGRADE_ITEM.get())
                    output.accept(STRESS_P2P_TUNNEL_PART_ITEM.get())
                    // Companion block hidden from creative tab — auto-placed by P2P tunnel
                    output.accept(ME_BLUEPRINT_CANNON_ITEM.get())
                    output.accept(KINETIC_ENERGY_ACCEPTOR_ITEM.get())
                    output.accept(ME_GEARBOX_ITEM.get())
                    output.accept(WHICHBALL_SKIN_DOLL_ITEM.get())
                    // Crafting items
                    output.accept(STRESS_CIRCUIT_BOARD.get())
                    output.accept(ADVANCED_STRESS_CIRCUIT_BOARD.get())
                    output.accept(STRESS_PROCESSOR.get())
                    output.accept(ADVANCED_STRESS_PROCESSOR.get())
                    output.accept(ANDESITE_STRESS_CELL_HOUSING.get())
                    output.accept(BRASS_STRESS_CELL_HOUSING.get())
                    // Storage components
                    STRESS_COMPONENTS.forEach { output.accept(it.get()) }
                    // Storage cells
                    STRESS_CELLS.forEach { output.accept(it.get()) }
                }
                .build()
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : appeng.menu.AEBaseMenu> createPatternProviderMenuType(
            factory: (MenuType<T>, Int, net.minecraft.world.entity.player.Inventory, PatternProviderLogicHost) -> T
        ): MenuType<T> {
            var menuTypeHolder: MenuType<T>? = null
            val menuType = net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create { windowId, inv, buf ->
                val locator = MenuLocators.readFromPacket(buf)
                val host = locator.locate(inv.player, PatternProviderLogicHost::class.java)
                    ?: throw IllegalStateException("Could not find PatternProviderLogicHost")
                val menu = factory(menuTypeHolder!!, windowId, inv, host)
                menu.setLocator(locator)
                menu.setReturnedFromSubScreen(buf.readBoolean())
                menu
            }
            menuTypeHolder = menuType as MenuType<T>
            return menuType
        }

        private fun <T : appeng.menu.AEBaseMenu> registerPatternProviderOpener(
            menuType: MenuType<T>,
            menuFactory: (Int, net.minecraft.world.entity.player.Inventory, PatternProviderLogicHost) -> T
        ) {
            MenuOpener.addOpener(menuType) { player: Player, locator: MenuHostLocator, fromSubMenu: Boolean ->
                if (player !is ServerPlayer) return@addOpener false
                val host = locator.locate(player, PatternProviderLogicHost::class.java) ?: return@addOpener false
                val title = Component.empty()
                val menuProvider = SimpleMenuProvider({ wnd, p, _ ->
                    val m = menuFactory(wnd, p, host)
                    m.setLocator(locator)
                    m
                }, title)
                player.openMenu(menuProvider) { buffer ->
                    MenuLocators.writeToPacket(buffer, locator)
                    buffer.writeBoolean(fromSubMenu)
                }
                true
            }
        }
    }

    init {
        val bus: IEventBus = MOD_BUS
        BLOCKS.register(bus)
        ITEMS.register(bus)
        BLOCK_ENTITY_TYPES.register(bus)
        MENU_TYPES.register(bus)
        CREATIVE_TABS.register(bus)
        bus.addListener { event: net.neoforged.neoforge.registries.RegisterEvent ->
            // Register after AE2 creates its keytypes registry (NewRegistryEvent)
            // but before registries freeze
            event.register(appeng.api.stacks.AEKeyType.REGISTRY_KEY) {
                AEKeyTypes.register(StressKeyType.TYPE)
            }
        }
        bus.addListener { event: RegisterPayloadHandlersEvent ->
            val registrar = event.registrar(MOD_ID)
            registrar.playToServer(
                ConfigureMECannonPayload.TYPE,
                ConfigureMECannonPayload.STREAM_CODEC,
                ConfigureMECannonPayload::handle
            )
        }

        // Register part models manually since Kotlin companion object @PartModels annotations
        // are not discoverable by AE2's Java reflection-based PartModelsHelper scanner
        appeng.api.parts.PartModels.registerModels(
            AndesitePatternProviderPart.ANDESITE_MODEL_BASE
        )
        appeng.api.parts.PartModels.registerModels(
            *AndesitePatternProviderPart.ANDESITE_MODELS_OFF.models.toTypedArray()
        )
        appeng.api.parts.PartModels.registerModels(
            *AndesitePatternProviderPart.ANDESITE_MODELS_ON.models.toTypedArray()
        )
        appeng.api.parts.PartModels.registerModels(
            *AndesitePatternProviderPart.ANDESITE_MODELS_HAS_CHANNEL.models.toTypedArray()
        )
        appeng.api.parts.PartModels.registerModels(
            BrassPatternProviderPart.BRASS_MODEL_BASE
        )
        appeng.api.parts.PartModels.registerModels(
            *BrassPatternProviderPart.BRASS_MODELS_OFF.models.toTypedArray()
        )
        appeng.api.parts.PartModels.registerModels(
            *BrassPatternProviderPart.BRASS_MODELS_ON.models.toTypedArray()
        )
        appeng.api.parts.PartModels.registerModels(
            *BrassPatternProviderPart.BRASS_MODELS_HAS_CHANNEL.models.toTypedArray()
        )

        // Register Stress P2P Tunnel part models
        for (model in StressP2PTunnelPart.getModels()) {
            appeng.api.parts.PartModels.registerModels(*model.models.toTypedArray())
        }

        bus.addListener(::onCommonSetup)
        bus.addListener(::onRegisterCapabilities)

        if (FMLEnvironment.dist.isClient) {
            ClientSetup.register(bus)
        }

        LOGGER.info("Applied Create loaded")
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        // Register representative items so AE2 network status and Jade show our devices correctly
        AEBaseBlockEntity.registerBlockEntityItem(
            ANDESITE_PATTERN_PROVIDER_BE.get(),
            ANDESITE_PATTERN_PROVIDER_ITEM.get().asItem()
        )
        AEBaseBlockEntity.registerBlockEntityItem(
            BRASS_PATTERN_PROVIDER_BE.get(),
            BRASS_PATTERN_PROVIDER_ITEM.get().asItem()
        )
        AEBaseBlockEntity.registerBlockEntityItem(
            ME_BLUEPRINT_CANNON_BE.get(),
            ME_BLUEPRINT_CANNON_ITEM.get().asItem()
        )

        // Register representative items for Kinetic Energy Acceptor and ME Gearbox
        // (fixes AE2 network status page showing correct device icons)
        AEBaseBlockEntity.registerBlockEntityItem(
            KINETIC_ENERGY_ACCEPTOR_BE.get(),
            KINETIC_ENERGY_ACCEPTOR_ITEM.get().asItem()
        )
        AEBaseBlockEntity.registerBlockEntityItem(
            ME_GEARBOX_BE.get(),
            ME_GEARBOX_ITEM.get().asItem()
        )

        // Reload AE2's igtooltip ServiceLoader to ensure our AppliedCreateTooltipProvider
        // is discovered (NeoForge module layers may cache ServiceLoader before our mod loads)
        try {
            appeng.integration.modules.igtooltip.TooltipProviders.LOADER.reload()
            LOGGER.debug("Reloaded AE2 igtooltip ServiceLoader")
        } catch (e: Exception) {
            LOGGER.warn("Failed to reload AE2 igtooltip ServiceLoader: {}", e.message)
        }

        event.enqueueWork {
            // Register P2P attunement tag — now works because StressP2PPartItem extends PartItem
            P2PTunnelAttunement.registerAttunementTag(STRESS_P2P_TUNNEL_PART_ITEM.get())

            registerPatternProviderOpener(BRASS_PATTERN_PROVIDER_MENU.get()) { wnd, inv, host ->
                BrassPatternProviderMenu(
                    BRASS_PATTERN_PROVIDER_MENU.get(),
                    wnd, inv, host
                )
            }

            // Register stress value for companion block
            com.simibubi.create.api.stress.BlockStressValues.IMPACTS.register(
                STRESS_P2P_COMPANION_BLOCK.get(), { 0.0 }
            )

            // Register stress value for kinetic energy acceptor
            com.simibubi.create.api.stress.BlockStressValues.IMPACTS.register(
                KINETIC_ENERGY_ACCEPTOR_BLOCK.get(), { KineticEnergyAcceptorBlockEntity.MAX_STRESS_SU / 256.0 }
            )

            // Register stress values for ME Gearbox
            // Import mode: stress impact (consumes kinetic energy)
            com.simibubi.create.api.stress.BlockStressValues.IMPACTS.register(
                ME_GEARBOX_BLOCK.get(), { MEGearboxBlockEntity.BASE_STRESS_IMPACT_PER_RPM.toDouble() }
            )
            // Export mode: stress capacity (generates kinetic energy)
            com.simibubi.create.api.stress.BlockStressValues.CAPACITIES.register(
                ME_GEARBOX_BLOCK.get(), { MEGearboxBlockEntity.BASE_STRESS_CAPACITY_PER_RPM.toDouble() }
            )

            // Register upgrade cards for ME Blueprint Cannon
            appeng.api.upgrades.Upgrades.add(AEItems.SPEED_CARD, ME_BLUEPRINT_CANNON_ITEM.get(), 4)
            appeng.api.upgrades.Upgrades.add(AEItems.CRAFTING_CARD, ME_BLUEPRINT_CANNON_ITEM.get(), 1)
        }
    }

    private fun onRegisterCapabilities(event: RegisterCapabilitiesEvent) {
        // Register IN_WORLD_GRID_NODE_HOST so AE2 cables recognize our blocks as grid devices
        event.registerBlockEntity(
            AECapabilities.IN_WORLD_GRID_NODE_HOST,
            ANDESITE_PATTERN_PROVIDER_BE.get()
        ) { be, _ -> be }
        event.registerBlockEntity(
            AECapabilities.IN_WORLD_GRID_NODE_HOST,
            BRASS_PATTERN_PROVIDER_BE.get()
        ) { be, _ -> be }

        // Note: StressP2PCompanion does NOT need IN_WORLD_GRID_NODE_HOST — it has no AE2 grid node
        event.registerBlockEntity(
            AECapabilities.IN_WORLD_GRID_NODE_HOST,
            ME_BLUEPRINT_CANNON_BE.get()
        ) { be, _ -> be }

        // Kinetic Energy Acceptor — AE2 grid node for energy generation
        event.registerBlockEntity(
            AECapabilities.IN_WORLD_GRID_NODE_HOST,
            KINETIC_ENERGY_ACCEPTOR_BE.get()
        ) { be, _ -> be }

        // ME Gearbox — AE2 grid node for stress transfer
        event.registerBlockEntity(
            AECapabilities.IN_WORLD_GRID_NODE_HOST,
            ME_GEARBOX_BE.get()
        ) { be, _ -> be }

        // Register GENERIC_INTERNAL_INV so AE2's registerGenericAdapters auto-registers
        // Capabilities.ItemHandler.BLOCK and FluidHandler.BLOCK adapters for our blocks,
        // enabling item insertion from hoppers, Create mechanical crafters, etc.
        event.registerBlockEntity(
            AECapabilities.GENERIC_INTERNAL_INV,
            ANDESITE_PATTERN_PROVIDER_BE.get()
        ) { be, _ -> be.logic.returnInv }
        event.registerBlockEntity(
            AECapabilities.GENERIC_INTERNAL_INV,
            BRASS_PATTERN_PROVIDER_BE.get()
        ) { be, _ -> be.logic.returnInv }
    }
}