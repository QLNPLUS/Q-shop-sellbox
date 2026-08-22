package com.qshop.sellbox;

import com.qshop.api.QShopAddonApi;
import com.qshop.currency.CurrencyRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class SellBoxSaleService {
    public static final ResourceLocation SOURCE = ResourceLocation.fromNamespaceAndPath(
            SellBoxMod.MODID, "auto_sell");

    private SellBoxSaleService() {}

    public static void sellContents(SellBoxBlockEntity box) {
        if (box.owner() == null || box.getLevel() == null || box.getLevel().getServer() == null) return;
        Map<String, Double> earnings = new LinkedHashMap<>();
        int itemCount = 0;
        for (int slot = 0; slot < box.items().getSlots(); slot++) {
            ItemStack stack = box.items().getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            PriceQuote quote = SellBoxPrices.resolve(stack);
            if (quote == null || quote.price() <= 0) continue;
            ItemStack removed = box.items().extractItem(slot, stack.getCount(), false);
            if (removed.isEmpty()) continue;
            itemCount += removed.getCount();
            earnings.merge(quote.currency(), quote.price() * removed.getCount(), Double::sum);
        }
        if (earnings.isEmpty()) return;

        UUID owner = box.owner();
        MinecraftServer server = box.getLevel().getServer();
        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        for (var entry : earnings.entrySet()) {
            // QShop's UUID overload writes directly to an offline player's wallet data.
            QShopAddonApi.currency().deposit(server, owner, entry.getKey(), entry.getValue(),
                    SOURCE, box.getBlockPos());
        }
        if (itemCount > 0 && !earnings.isEmpty()) {
            var first = earnings.entrySet().iterator().next();
            Component message = Component.translatable("qshop_sellbox.message.sold",
                    itemCount, format(first.getValue()),
                    CurrencyRegistry.displayName(first.getKey()));
            if (online != null && box.showActionBarNotification()) online.displayClientMessage(message, true);
            if (online != null && box.showChatNotification()) online.sendSystemMessage(message);
        }
    }

    private static String format(double value) {
        return Math.abs(value - Math.rint(value)) < 0.000001D
                ? String.format(java.util.Locale.ROOT, "%.0f", value)
                : String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
