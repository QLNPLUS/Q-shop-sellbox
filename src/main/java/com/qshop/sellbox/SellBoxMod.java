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
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

@Mod(SellBoxMod.MODID)
public final class SellBoxMod {
    public static final String MODID = "qshop_sellbox";

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<SellBoxBlock> SELL_BOX = BLOCKS.registerBlock("sell_box",
            SellBoxBlock::new,
            properties -> properties
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava());
    public static final DeferredItem<BlockItem> SELL_BOX_ITEM = ITEMS.registerSimpleBlockItem("sell_box", SELL_BOX);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SellBoxBlockEntity>> SELL_BOX_ENTITY =
            BLOCK_ENTITIES.register("sell_box", () ->
                    new BlockEntityType<>(SellBoxBlockEntity::new, SELL_BOX.get()));
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
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            bus.addListener(com.qshop.sellbox.client.SellBoxClient::registerMenuScreens);
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 26.x 移除了旧的 IItemHandler 能力：物品能力现在是 Capabilities.Item.BLOCK，
        // 承载类型为 ResourceHandler<ItemResource>。方块实体实现原版 Container（内部仍用 ItemStackHandler），
        // 这里用 NeoForge 提供的 Container -> ResourceHandler 桥接，保持漏斗/管道自动化行为不变。
        event.registerBlockEntity(Capabilities.Item.BLOCK, SELL_BOX_ENTITY.get(),
                (box, side) -> VanillaContainerWrapper.of(box));
    }
}
