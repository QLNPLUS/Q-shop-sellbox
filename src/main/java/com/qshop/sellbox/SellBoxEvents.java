package com.qshop.sellbox;

import com.qshop.api.QShopAddonApi;
import com.qshop.currency.CurrencyRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;

@EventBusSubscriber(modid = SellBoxMod.MODID)
public final class SellBoxEvents {
    private SellBoxEvents() {}

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        SellBoxConfig.refresh();
        SellBoxNetwork.broadcastPrices(event.getServer());
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != SellBoxConfig.SPEC) return;
        SellBoxConfig.refresh();
        if (net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null) {
            SellBoxNetwork.broadcastPrices(net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SellBoxSavedData data = SellBoxSavedData.get(player.server);
        data.recordPlayer(player.getUUID(), player.getGameProfile().getName());
        SellBoxSavedData.PendingResult pending = data.takePending(player.getUUID());
        for (var entry : pending.earnings().entrySet()) {
            QShopAddonApi.currency().deposit(player, entry.getKey(), entry.getValue(),
                    SellBoxSaleService.SOURCE, null);
            net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.translatable(
                    "qshop_sellbox.message.synced", format(entry.getValue()),
                    CurrencyRegistry.displayName(entry.getKey()));
            if (pending.showActionBarNotification()) player.displayClientMessage(message, true);
            if (pending.showChatNotification()) player.sendSystemMessage(message);
        }
        SellBoxNetwork.sendPrices(player);
    }

    private static String format(double value) {
        return Math.abs(value - Math.rint(value)) < 0.000001D
                ? String.format(java.util.Locale.ROOT, "%.0f", value)
                : String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
