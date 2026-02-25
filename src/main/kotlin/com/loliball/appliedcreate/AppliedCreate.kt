package com.loliball.appliedcreate

import com.loliball.appliedcreate.block.AndesitePatternProviderBlock
import com.loliball.appliedcreate.block.BrassPatternProviderBlock
import com.loliball.appliedcreate.block.entity.AndesitePatternProviderBlockEntity
import com.loliball.appliedcreate.block.entity.BrassPatternProviderBlockEntity
import appeng.blockentity.AEBaseBlockEntity

import com.loliball.appliedcreate.gui.BrassPatternProviderMenu
import com.loliball.appliedcreate.item.BrassPatternProviderUpgradeItem
import com.loliball.appliedcreate.item.MechanicalCraftingPartItem
import com.loliball.appliedcreate.kinetic.StressAcceptorBlock
import com.loliball.appliedcreate.kinetic.StressAcceptorBlockEntity
import com.loliball.appliedcreate.kinetic.StressProviderBlock
import com.loliball.appliedcreate.kinetic.StressProviderBlockEntity
import com.loliball.appliedcreate.part.AndesitePatternProviderPart
import com.loliball.appliedcreate.part.BrassPatternProviderPart
import com.loliball.appliedcreate.p2p.StressP2PTunnelPart
import com.loliball.appliedcreate.storage.StressKeyType
import com.loliball.appliedcreate.storage.StressStorageCell
import appeng.api.features.P2PTunnelAttunement
import appeng.api.stacks.AEKeyTypes
import appeng.helpers.patternprovider.PatternProviderLogicHost
import appeng.menu.MenuOpener
import appeng.menu.locator.MenuLocator
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
import net.minecraftforge.common.extensions.IForgeMenuType
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.network.NetworkHooks
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(AppliedCreate.MOD_ID)
class AppliedCreate {
    companion object {
        const val MOD_ID = "appliedcreate"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)

        val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID)
        val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID)
        val BLOCK_ENTITY_TYPES: DeferredRegister<BlockEntityType<*>> = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID)
        val MENU_TYPES: DeferredRegister<MenuType<*>> = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID)
        val CREATIVE_TABS: DeferredRegister<CreativeModeTab> = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID)


        // ── Andesite Pattern Provider ──
        val ANDESITE_PATTERN_PROVIDER_BLOCK: RegistryObject<Block> = BLOCKS.register("andesite_pattern_provider") {
            AndesitePatternProviderBlock()
        }

        val ANDESITE_PATTERN_PROVIDER_ITEM: RegistryObject<Item> = ITEMS.register("andesite_pattern_provider") {
            BlockItem(ANDESITE_PATTERN_PROVIDER_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val ANDESITE_PATTERN_PROVIDER_BE: RegistryObject<BlockEntityType<AndesitePatternProviderBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("andesite_pattern_provider") {
                BlockEntityType.Builder.of(
                    ::AndesitePatternProviderBlockEntity,
                    ANDESITE_PATTERN_PROVIDER_BLOCK.get()
                ).build(null)
            }

        // ── Brass Pattern Provider ──
        val BRASS_PATTERN_PROVIDER_BLOCK: RegistryObject<Block> = BLOCKS.register("brass_pattern_provider") {
            BrassPatternProviderBlock()
        }

        val BRASS_PATTERN_PROVIDER_ITEM: RegistryObject<Item> = ITEMS.register("brass_pattern_provider") {
            BlockItem(BRASS_PATTERN_PROVIDER_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val BRASS_PATTERN_PROVIDER_BE: RegistryObject<BlockEntityType<BrassPatternProviderBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("brass_pattern_provider") {
                BlockEntityType.Builder.of(
                    ::BrassPatternProviderBlockEntity,
                    BRASS_PATTERN_PROVIDER_BLOCK.get()
                ).build(null)
            }

        val BRASS_PATTERN_PROVIDER_MENU: RegistryObject<MenuType<BrassPatternProviderMenu>> =
            MENU_TYPES.register("brass_pattern_provider") {
                createPatternProviderMenuType { menuType, windowId, inv, host ->
                    BrassPatternProviderMenu(menuType, windowId, inv, host)
                }
            }

        // ── Items ──
        val BRASS_PATTERN_PROVIDER_UPGRADE_ITEM: RegistryObject<Item> = ITEMS.register("brass_pattern_provider_upgrade") {
            BrassPatternProviderUpgradeItem()
        }

        val ANDESITE_PATTERN_PROVIDER_PART_ITEM: RegistryObject<Item> = ITEMS.register("andesite_pattern_provider_part") {
            MechanicalCraftingPartItem(
                Item.Properties(),
                AndesitePatternProviderPart::class.java
            ) { partItem -> AndesitePatternProviderPart(partItem) }
        }

        val BRASS_PATTERN_PROVIDER_PART_ITEM: RegistryObject<Item> = ITEMS.register("brass_pattern_provider_part") {
            MechanicalCraftingPartItem(
                Item.Properties(),
                BrassPatternProviderPart::class.java
            ) { partItem -> BrassPatternProviderPart(partItem) }
        }

        // ── Stress P2P Tunnel ──
        val STRESS_P2P_TUNNEL_PART_ITEM: RegistryObject<Item> = ITEMS.register("stress_p2p_tunnel") {
            MechanicalCraftingPartItem(
                Item.Properties(),
                StressP2PTunnelPart::class.java
            ) { partItem -> StressP2PTunnelPart(partItem) }
        }

        // ── Stress P2P Companion Blocks ──
        val STRESS_ACCEPTOR_BLOCK: RegistryObject<Block> = BLOCKS.register("stress_acceptor") {
            StressAcceptorBlock()
        }

        val STRESS_ACCEPTOR_ITEM: RegistryObject<Item> = ITEMS.register("stress_acceptor") {
            BlockItem(STRESS_ACCEPTOR_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val STRESS_ACCEPTOR_BE: RegistryObject<BlockEntityType<StressAcceptorBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("stress_acceptor") {
                BlockEntityType.Builder.of(
                    ::StressAcceptorBlockEntity,
                    STRESS_ACCEPTOR_BLOCK.get()
                ).build(null)
            }

        val STRESS_PROVIDER_BLOCK: RegistryObject<Block> = BLOCKS.register("stress_provider") {
            StressProviderBlock()
        }

        val STRESS_PROVIDER_ITEM: RegistryObject<Item> = ITEMS.register("stress_provider") {
            BlockItem(STRESS_PROVIDER_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val STRESS_PROVIDER_BE: RegistryObject<BlockEntityType<StressProviderBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("stress_provider") {
                BlockEntityType.Builder.of(
                    ::StressProviderBlockEntity,
                    STRESS_PROVIDER_BLOCK.get()
                ).build(null)
            }

        // ── Stress Storage Crafting Items ──
        val STRESS_CIRCUIT_BOARD: RegistryObject<Item> = ITEMS.register("stress_circuit_board") {
            Item(Item.Properties())
        }
        val ADVANCED_STRESS_CIRCUIT_BOARD: RegistryObject<Item> = ITEMS.register("advanced_stress_circuit_board") {
            Item(Item.Properties())
        }
        val STRESS_PROCESSOR: RegistryObject<Item> = ITEMS.register("stress_processor") {
            Item(Item.Properties())
        }
        val ADVANCED_STRESS_PROCESSOR: RegistryObject<Item> = ITEMS.register("advanced_stress_processor") {
            Item(Item.Properties())
        }

        // ── Stress Storage Components ──
        val STRESS_STORAGE_COMPONENT_1K: RegistryObject<Item> = ITEMS.register("stress_storage_component_1k") {
            Item(Item.Properties())
        }
        val STRESS_STORAGE_COMPONENT_4K: RegistryObject<Item> = ITEMS.register("stress_storage_component_4k") {
            Item(Item.Properties())
        }
        val STRESS_STORAGE_COMPONENT_16K: RegistryObject<Item> = ITEMS.register("stress_storage_component_16k") {
            Item(Item.Properties())
        }
        val STRESS_STORAGE_COMPONENT_64K: RegistryObject<Item> = ITEMS.register("stress_storage_component_64k") {
            Item(Item.Properties())
        }
        val STRESS_STORAGE_COMPONENT_256K: RegistryObject<Item> = ITEMS.register("stress_storage_component_256k") {
            Item(Item.Properties())
        }
        val STRESS_STORAGE_COMPONENT_1M: RegistryObject<Item> = ITEMS.register("stress_storage_component_1m") {
            Item(Item.Properties())
        }
        val STRESS_STORAGE_COMPONENT_4M: RegistryObject<Item> = ITEMS.register("stress_storage_component_4m") {
            Item(Item.Properties())
        }
        val STRESS_STORAGE_COMPONENT_16M: RegistryObject<Item> = ITEMS.register("stress_storage_component_16m") {
            Item(Item.Properties())
        }
        val STRESS_STORAGE_COMPONENT_64M: RegistryObject<Item> = ITEMS.register("stress_storage_component_64m") {
            Item(Item.Properties())
        }
        val STRESS_STORAGE_COMPONENT_256M: RegistryObject<Item> = ITEMS.register("stress_storage_component_256m") {
            Item(Item.Properties())
        }

        // ── Cell Housings ──
        val ANDESITE_STRESS_CELL_HOUSING: RegistryObject<Item> = ITEMS.register("andesite_stress_cell_housing") {
            Item(Item.Properties())
        }
        val BRASS_STRESS_CELL_HOUSING: RegistryObject<Item> = ITEMS.register("brass_stress_cell_housing") {
            Item(Item.Properties())
        }

        // ── Stress Storage Cells ──
        val STRESS_CELL_1K: RegistryObject<Item> = ITEMS.register("stress_storage_cell_1k") {
            StressStorageCell(Item.Properties(), 0.5, 1, 8, 1)
        }
        val STRESS_CELL_4K: RegistryObject<Item> = ITEMS.register("stress_storage_cell_4k") {
            StressStorageCell(Item.Properties(), 1.0, 4, 8, 1)
        }
        val STRESS_CELL_16K: RegistryObject<Item> = ITEMS.register("stress_storage_cell_16k") {
            StressStorageCell(Item.Properties(), 1.5, 16, 8, 1)
        }
        val STRESS_CELL_64K: RegistryObject<Item> = ITEMS.register("stress_storage_cell_64k") {
            StressStorageCell(Item.Properties(), 2.0, 64, 8, 1)
        }
        val STRESS_CELL_256K: RegistryObject<Item> = ITEMS.register("stress_storage_cell_256k") {
            StressStorageCell(Item.Properties(), 2.5, 256, 8, 1)
        }
        val STRESS_CELL_1M: RegistryObject<Item> = ITEMS.register("stress_storage_cell_1m") {
            StressStorageCell(Item.Properties(), 3.0, 1024, 8, 1)
        }
        val STRESS_CELL_4M: RegistryObject<Item> = ITEMS.register("stress_storage_cell_4m") {
            StressStorageCell(Item.Properties(), 3.5, 4096, 8, 1)
        }
        val STRESS_CELL_16M: RegistryObject<Item> = ITEMS.register("stress_storage_cell_16m") {
            StressStorageCell(Item.Properties(), 4.0, 16384, 8, 1)
        }
        val STRESS_CELL_64M: RegistryObject<Item> = ITEMS.register("stress_storage_cell_64m") {
            StressStorageCell(Item.Properties(), 4.5, 65536, 8, 1)
        }
        val STRESS_CELL_256M: RegistryObject<Item> = ITEMS.register("stress_storage_cell_256m") {
            StressStorageCell(Item.Properties(), 5.0, 262144, 8, 1)
        }

        val STRESS_CELLS: List<RegistryObject<Item>> by lazy {
            listOf(
                STRESS_CELL_1K, STRESS_CELL_4K, STRESS_CELL_16K, STRESS_CELL_64K, STRESS_CELL_256K,
                STRESS_CELL_1M, STRESS_CELL_4M, STRESS_CELL_16M, STRESS_CELL_64M, STRESS_CELL_256M
            )
        }

        // ── Creative Tab ──
        val CREATIVE_TAB: RegistryObject<CreativeModeTab> = CREATIVE_TABS.register("main") {
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
                    output.accept(STRESS_ACCEPTOR_ITEM.get())
                    output.accept(STRESS_PROVIDER_ITEM.get())
                    // Crafting items
                    output.accept(STRESS_CIRCUIT_BOARD.get())
                    output.accept(ADVANCED_STRESS_CIRCUIT_BOARD.get())
                    output.accept(STRESS_PROCESSOR.get())
                    output.accept(ADVANCED_STRESS_PROCESSOR.get())
                    // Components
                    output.accept(STRESS_STORAGE_COMPONENT_1K.get())
                    output.accept(STRESS_STORAGE_COMPONENT_4K.get())
                    output.accept(STRESS_STORAGE_COMPONENT_16K.get())
                    output.accept(STRESS_STORAGE_COMPONENT_64K.get())
                    output.accept(STRESS_STORAGE_COMPONENT_256K.get())
                    output.accept(STRESS_STORAGE_COMPONENT_1M.get())
                    output.accept(STRESS_STORAGE_COMPONENT_4M.get())
                    output.accept(STRESS_STORAGE_COMPONENT_16M.get())
                    output.accept(STRESS_STORAGE_COMPONENT_64M.get())
                    output.accept(STRESS_STORAGE_COMPONENT_256M.get())
                    // Housings
                    output.accept(ANDESITE_STRESS_CELL_HOUSING.get())
                    output.accept(BRASS_STRESS_CELL_HOUSING.get())
                    // Cells
                    STRESS_CELLS.forEach { output.accept(it.get()) }
                }
                .build()
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : appeng.menu.AEBaseMenu> createPatternProviderMenuType(
            factory: (MenuType<T>, Int, net.minecraft.world.entity.player.Inventory, PatternProviderLogicHost) -> T
        ): MenuType<T> {
            var menuTypeHolder: MenuType<T>? = null
            val menuType = IForgeMenuType.create { windowId, inv, buf ->
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
            MenuOpener.addOpener(menuType) { player: Player, locator: MenuLocator, fromSubMenu: Boolean ->
                if (player !is ServerPlayer) return@addOpener false
                val host = locator.locate(player, PatternProviderLogicHost::class.java) ?: return@addOpener false
                val title = Component.empty()
                val menuProvider = SimpleMenuProvider({ wnd, p, _ ->
                    val m = menuFactory(wnd, p, host)
                    m.setLocator(locator)
                    m
                }, title)
                NetworkHooks.openScreen(player, menuProvider) { buffer ->
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

        if (FMLEnvironment.dist.isClient) {
            ClientSetup.register(bus)
        }

        LOGGER.info("Applied Create loaded")
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        // Register representative items so devices appear in ME network status and Jade
        AEBaseBlockEntity.registerBlockEntityItem(
            ANDESITE_PATTERN_PROVIDER_BE.get(),
            ANDESITE_PATTERN_PROVIDER_ITEM.get()
        )
        AEBaseBlockEntity.registerBlockEntityItem(
            BRASS_PATTERN_PROVIDER_BE.get(),
            BRASS_PATTERN_PROVIDER_ITEM.get()
        )

        event.enqueueWork {
            // Register our custom AE key type so AE2 recognizes StressKey in storage
            AEKeyTypes.register(StressKeyType.TYPE)

            // Register P2P attunement for stress tunnel
            P2PTunnelAttunement.registerAttunementTag(STRESS_P2P_TUNNEL_PART_ITEM.get())

            registerPatternProviderOpener(BRASS_PATTERN_PROVIDER_MENU.get()) { wnd, inv, host ->
                BrassPatternProviderMenu(
                    BRASS_PATTERN_PROVIDER_MENU.get(),
                    wnd, inv, host
                )
            }

            // Register stress values for companion blocks
            com.simibubi.create.api.stress.BlockStressValues.IMPACTS.register(
                STRESS_ACCEPTOR_BLOCK.get(), { 0.0 }
            )
            com.simibubi.create.api.stress.BlockStressValues.CAPACITIES.register(
                STRESS_PROVIDER_BLOCK.get(), { 2048.0 }
            )
        }
    }
}
