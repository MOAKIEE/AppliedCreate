package com.loliball.appliedcreate

import com.loliball.appliedcreate.block.BrassPatternProviderBlock
import com.loliball.appliedcreate.block.MechanicalCraftEncoderBlock
import com.loliball.appliedcreate.block.entity.BrassPatternProviderBlockEntity
import com.loliball.appliedcreate.block.entity.MechanicalCraftEncoderBlockEntity
import com.loliball.appliedcreate.gui.BrassPatternProviderMenu
import com.loliball.appliedcreate.gui.MechanicalCraftEncoderMenu
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.common.extensions.IForgeMenuType
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.loading.FMLEnvironment
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

        val MECHANICAL_CRAFT_ENCODER_BLOCK: RegistryObject<Block> = BLOCKS.register("mechanical_craft_encoder") {
            MechanicalCraftEncoderBlock()
        }

        val MECHANICAL_CRAFT_ENCODER_ITEM: RegistryObject<Item> = ITEMS.register("mechanical_craft_encoder") {
            BlockItem(MECHANICAL_CRAFT_ENCODER_BLOCK.get(), Item.Properties())
        }

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val MECHANICAL_CRAFT_ENCODER_BE: RegistryObject<BlockEntityType<MechanicalCraftEncoderBlockEntity>> =
            BLOCK_ENTITY_TYPES.register("mechanical_craft_encoder") {
                BlockEntityType.Builder.of(
                    ::MechanicalCraftEncoderBlockEntity,
                    MECHANICAL_CRAFT_ENCODER_BLOCK.get()
                ).build(null)
            }

        val MECHANICAL_CRAFT_ENCODER_MENU: RegistryObject<MenuType<MechanicalCraftEncoderMenu>> =
            MENU_TYPES.register("mechanical_craft_encoder") {
                IForgeMenuType.create { windowId, inv, data ->
                    MechanicalCraftEncoderMenu(windowId, inv, data)
                }
            }

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
                IForgeMenuType.create { windowId, inv, data ->
                    BrassPatternProviderMenu(windowId, inv, data)
                }
            }

        val CREATIVE_TAB: RegistryObject<CreativeModeTab> = CREATIVE_TABS.register("main") {
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.$MOD_ID"))
                .icon { MECHANICAL_CRAFT_ENCODER_ITEM.get().defaultInstance }
                .displayItems { _, output ->
                    output.accept(MECHANICAL_CRAFT_ENCODER_ITEM.get())
                    output.accept(BRASS_PATTERN_PROVIDER_ITEM.get())
                }
                .build()
        }
    }

    init {
        val bus: IEventBus = MOD_BUS
        BLOCKS.register(bus)
        ITEMS.register(bus)
        BLOCK_ENTITY_TYPES.register(bus)
        MENU_TYPES.register(bus)
        CREATIVE_TABS.register(bus)

        if (FMLEnvironment.dist.isClient) {
            ClientSetup.register(bus)
        }

        LOGGER.info("Applied Create loaded - Mechanical Craft Encoder standalone mod")
    }
}
