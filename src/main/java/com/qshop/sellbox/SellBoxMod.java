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
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(SellBoxMod.MODID)
public final class SellBoxMod {
    public static final String MODID = "qshop_sellbox";

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Block> SELL_BOX = BLOCKS.register("sell_box",
            () -> new SellBoxBlock(Block.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));
    public static final RegistryObject<Item> SELL_BOX_ITEM = ITEMS.register("sell_box",
            () -> new BlockItem(SELL_BOX.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<SellBoxBlockEntity>> SELL_BOX_ENTITY =
            BLOCK_ENTITIES.register("sell_box", () -> BlockEntityType.Builder.of(
                    SellBoxBlockEntity::new, SELL_BOX.get()).build(null));
    public static final RegistryObject<MenuType<SellBoxMenu>> SELL_BOX_MENU = MENUS.register(
            "sell_box", () -> {
                IContainerFactory<SellBoxMenu> factory = SellBoxMenu::new;
                return new MenuType<>(factory, net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
            });
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("sell_box",
            () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.qshop_sellbox"))
                    .icon(() -> SELL_BOX_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(SELL_BOX_ITEM.get()))
                    .build());

    public SellBoxMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        TABS.register(bus);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON, SellBoxConfig.SPEC, "qshop_sellbox-common.toml");
        SellBoxNetwork.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> bus.addListener(com.qshop.sellbox.client.SellBoxClient::onClientSetup));
    }
}
