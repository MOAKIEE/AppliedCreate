package com.loliball.appliedcreate

import com.loliball.appliedcreate.block.AndesitePatternProviderBlock
import com.loliball.appliedcreate.block.BrassPatternProviderBlock
import com.loliball.appliedcreate.block.entity.AndesitePatternProviderBlockEntity
import com.loliball.appliedcreate.block.entity.BrassPatternProviderBlockEntity

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
import appeng.api.AECapabilities
import appeng.api.features.P2PTunnelAttunement
import appeng.api.stacks.AEKeyTypes
import appeng.blockentity.AEBaseBlockEntity
import appeng.helpers.patternprovider.PatternProviderLogicHost
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
            MechanicalCraftingPartItem(
                Item.Properties(),
                StressP2PTunnelPart::class.java
            ) { partItem -> StressP2PTunnelPart(partItem) }
        }

        // ── Stress P2P Companion Blocks ──
        val STRESS_ACCEPTOR_BLOCK: DeferredHolder<Block, Block> = BLOCKS.register("stress_acceptor") { ->
            StressAcceptorBlock()
        }

        val STRESS_ACCEPTOR_ITEM: DeferredHolder<Item, Item> = ITEMS.register("stress_acceptor") { ->
            BlockItem(STRESS_ACCEPTOR_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val STRESS_ACCEPTOR_BE: DeferredHolder<BlockEntityType<*>, BlockEntityType<StressAcceptorBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("stress_acceptor") { ->
                BlockEntityType.Builder.of(
                    { pos, state -> StressAcceptorBlockEntity(STRESS_ACCEPTOR_BE.get(), pos, state) },
                    STRESS_ACCEPTOR_BLOCK.get()
                ).build(null)
            }

        val STRESS_PROVIDER_BLOCK: DeferredHolder<Block, Block> = BLOCKS.register("stress_provider") { ->
            StressProviderBlock()
        }

        val STRESS_PROVIDER_ITEM: DeferredHolder<Item, Item> = ITEMS.register("stress_provider") { ->
            BlockItem(STRESS_PROVIDER_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val STRESS_PROVIDER_BE: DeferredHolder<BlockEntityType<*>, BlockEntityType<StressProviderBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("stress_provider") { ->
                BlockEntityType.Builder.of(
                    { pos, state -> StressProviderBlockEntity(STRESS_PROVIDER_BE.get(), pos, state) },
                    STRESS_PROVIDER_BLOCK.get()
                ).build(null)
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
                    output.accept(STRESS_ACCEPTOR_ITEM.get())
                    output.accept(STRESS_PROVIDER_ITEM.get())
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

        event.enqueueWork {
            // Key type registration moved to init block (must happen before registry freeze)

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