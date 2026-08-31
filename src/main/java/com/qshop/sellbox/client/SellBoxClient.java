package com.qshop.sellbox.client;

import com.qshop.sellbox.SellBoxMod;
import com.qshop.sellbox.SellBoxNetwork;
import com.qshop.sellbox.SellBoxPrices;
import com.qshop.sellbox.SellBoxScreen;
import com.qshop.sellbox.PriceQuote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = SellBoxMod.MODID, value = Dist.CLIENT)
public final class SellBoxClient {
    private static boolean showPriceTooltip = true;
    private static boolean hasDynamicPriceFunction;
    private static int nextPriceRequestId;
    private static final Map<String, String> CURRENCY_DISPLAY_NAMES = new HashMap<>();
    private static final Map<String, PriceQuote> DYNAMIC_PRICE_CACHE = new HashMap<>();
    private static final Set<String> DYNAMIC_NO_PRICE = new HashSet<>();
    private static final Map<Integer, String> PENDING_PRICE_REQUESTS = new HashMap<>();

    private SellBoxClient() {}

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(SellBoxMod.SELL_BOX_MENU.get(), SellBoxScreen::new));
    }

    public static void applyOwners(SellBoxNetwork.SyncOwnersPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SellBoxScreen screen
                && screen.getMenu().pos().equals(packet.pos())) {
            screen.getMenu().setOwnerData(packet.owner(), packet.ownerName());
            screen.getMenu().setSettingsData(packet.sellMode(), packet.saleIntervalTicks(),
                    packet.showActionBarNotification(), packet.showChatNotification());
            screen.refreshIntervalInput();
        }
    }

    public static void applyPrices(SellBoxNetwork.SyncPricesPacket packet) {
        SellBoxPrices.setClientRules(packet.defaultCurrency(), packet.rules());
        showPriceTooltip = packet.showPriceTooltip();
        hasDynamicPriceFunction = packet.hasDynamicPriceFunction();
        CURRENCY_DISPLAY_NAMES.clear();
        CURRENCY_DISPLAY_NAMES.putAll(packet.currencyDisplayNames());
        DYNAMIC_PRICE_CACHE.clear();
        DYNAMIC_NO_PRICE.clear();
        PENDING_PRICE_REQUESTS.clear();
    }

    public static void applyPriceResult(SellBoxNetwork.PriceResultPacket packet) {
        String key = PENDING_PRICE_REQUESTS.remove(packet.requestId());
        if (key == null) return;
        if (packet.found()) {
            DYNAMIC_PRICE_CACHE.put(key, new PriceQuote(packet.price(), packet.currency()));
        } else {
            DYNAMIC_NO_PRICE.add(key);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!showPriceTooltip) return;
        if (hasDynamicPriceFunction) {
            addDynamicPriceTooltip(event);
            return;
        }
        var quote = SellBoxPrices.resolveClient(event.getItemStack());
        if (quote != null) {
            event.getToolTip().add(net.minecraft.network.chat.Component.translatable(
                    "qshop_sellbox.tooltip.price", quote.formattedPrice(),
                    currencyDisplayName(quote.currency())));
        }
    }

    private static void addDynamicPriceTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String key = priceKey(stack);
        PriceQuote quote = DYNAMIC_PRICE_CACHE.get(key);
        if (quote != null) {
            addPriceTooltip(event, quote);
            return;
        }
        if (DYNAMIC_NO_PRICE.contains(key) || Minecraft.getInstance().getConnection() == null) return;
        if (PENDING_PRICE_REQUESTS.containsValue(key)) return;
        int requestId = nextPriceRequestId++;
        if (requestId < 0) {
            nextPriceRequestId = 1;
            requestId = 0;
        }
        PENDING_PRICE_REQUESTS.put(requestId, key);
        SellBoxNetwork.sendPriceQuery(requestId, stack);
    }

    private static void addPriceTooltip(ItemTooltipEvent event, PriceQuote quote) {
        event.getToolTip().add(net.minecraft.network.chat.Component.translatable(
                "qshop_sellbox.tooltip.price", quote.formattedPrice(),
                currencyDisplayName(quote.currency())));
    }

    private static String currencyDisplayName(String currencyId) {
        String displayName = CURRENCY_DISPLAY_NAMES.get(currencyId);
        return displayName == null || displayName.isBlank() ? currencyId : displayName;
    }

    private static String priceKey(ItemStack stack) {
        return stack.save(new CompoundTag()).toString();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SellBoxPrices.clearClientRules();
        showPriceTooltip = true;
        hasDynamicPriceFunction = false;
        CURRENCY_DISPLAY_NAMES.clear();
        DYNAMIC_PRICE_CACHE.clear();
        DYNAMIC_NO_PRICE.clear();
        PENDING_PRICE_REQUESTS.clear();
    }
}
