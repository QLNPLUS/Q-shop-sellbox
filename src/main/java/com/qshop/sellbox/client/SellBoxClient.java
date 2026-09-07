package com.qshop.sellbox.client;

import com.qshop.sellbox.SellBoxMod;
import com.qshop.sellbox.SellBoxNetwork;
import com.qshop.sellbox.SellBoxPrices;
import com.qshop.sellbox.SellBoxScreen;
import com.qshop.sellbox.PriceQuote;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@EventBusSubscriber(modid = SellBoxMod.MODID, value = Dist.CLIENT)
public final class SellBoxClient {
    private static boolean showPriceTooltip = true;
    private static boolean hasDynamicPriceFunction;
    private static int nextPriceRequestId;
    private static final Map<String, String> CURRENCY_DISPLAY_NAMES = new HashMap<>();
    private static final Map<String, PriceQuote> DYNAMIC_PRICE_CACHE = new HashMap<>();
    private static final Set<String> DYNAMIC_NO_PRICE = new HashSet<>();
    private static final Map<Integer, String> PENDING_PRICE_REQUESTS = new HashMap<>();
    private static final Map<String, List<Consumer<PriceQuote>>> PRICE_CALLBACKS = new HashMap<>();

    private SellBoxClient() {}

    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(SellBoxMod.SELL_BOX_MENU.get(), SellBoxScreen::new);
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
        PRICE_CALLBACKS.clear();
    }

    public static void applyPriceResult(SellBoxNetwork.PriceResultPacket packet) {
        String key = PENDING_PRICE_REQUESTS.remove(packet.requestId());
        if (key == null) return;
        PriceQuote quote = null;
        if (packet.found()) {
            quote = new PriceQuote(packet.price(), packet.currency());
            DYNAMIC_PRICE_CACHE.put(key, quote);
        } else {
            DYNAMIC_NO_PRICE.add(key);
        }
        List<Consumer<PriceQuote>> callbacks = PRICE_CALLBACKS.remove(key);
        if (callbacks != null) {
            for (Consumer<PriceQuote> callback : callbacks) callback.accept(quote);
        }
    }

    /** Queries a client-visible price, using the local cache or the server when necessary. */
    public static void queryPrice(ItemStack stack, Consumer<PriceQuote> callback) {
        if (stack == null || stack.isEmpty() || callback == null) return;
        ItemStack copy = stack.copy();
        String key = priceKey(copy);
        if (!hasDynamicPriceFunction) {
            callback.accept(SellBoxPrices.resolveClient(copy));
            return;
        }
        PriceQuote cached = DYNAMIC_PRICE_CACHE.get(key);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        if (DYNAMIC_NO_PRICE.contains(key)) {
            callback.accept(null);
            return;
        }
        if (Minecraft.getInstance().getConnection() == null) {
            callback.accept(null);
            return;
        }
        PRICE_CALLBACKS.computeIfAbsent(key, ignored -> new ArrayList<>()).add(callback);
        if (PENDING_PRICE_REQUESTS.containsValue(key)) {
            return;
        }
        int requestId = nextPriceRequestId++;
        if (requestId < 0) {
            nextPriceRequestId = 1;
            requestId = 0;
        }
        PENDING_PRICE_REQUESTS.put(requestId, key);
        SellBoxNetwork.sendPriceQuery(requestId, copy);
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
        return stack.save(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)).toString();
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
        PRICE_CALLBACKS.clear();
    }
}
