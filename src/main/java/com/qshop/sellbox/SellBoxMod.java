package com.qshop.sellbox;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(SellBoxMod.MODID)
public final class SellBoxMod {
    public static final String MODID = "qshop_sellbox";

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<Block, Block> SELL_BOX = BLOCKS.register("sell_box",
            () -> new SellBoxBlock(Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));
    public static final DeferredHolder<Item, Item> SELL_BOX_ITEM = ITEMS.register("sell_box",
            () -> new BlockItem(SELL_BOX.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SellBoxBlockEntity>> SELL_BOX_ENTITY =
            BLOCK_ENTITIES.register("sell_box", () -> BlockEntityType.Builder.of(
                    SellBoxBlockEntity::new, SELL_BOX.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SellBoxMenu>> SELL_BOX_MENU = MENUS.register(
            "sell_box", () -> {
                IContainerFactory<SellBoxMenu> factory = SellBoxMenu::new;
                return new MenuType<>(factory, net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
            });
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("sell_box",
            () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.qshop_sellbox"))
                    .icon(() -> SELL_BOX_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(SELL_BOX_ITEM.get()))
                    .build());

    public SellBoxMod(ModContainer modContainer, IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
        modContainer.registerConfig(ModConfig.Type.COMMON, SellBoxConfig.SPEC,
                "qshop_sellbox-common.toml");
        SellBoxNetwork.init(bus);
        bus.addListener(SellBoxMod::registerCapabilities);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            bus.addListener(com.qshop.sellbox.client.SellBoxClient::registerMenuScreens);
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SELL_BOX_ENTITY.get(),
                (box, side) -> box.items());
    }
}
