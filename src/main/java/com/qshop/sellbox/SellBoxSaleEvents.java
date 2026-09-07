package com.qshop.sellbox;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Loader-neutral hook used by the optional KubeJS sale event bridge. */
public final class SellBoxSaleEvents {
    public interface AfterSellHook {
        void call(ItemStack item, @Nullable ServerPlayer player, ServerLevel level,
                  double itemPrice, String currency);
    }

    public static volatile AfterSellHook afterSell;

    private SellBoxSaleEvents() {}

    public static void post(ItemStack item, @Nullable ServerPlayer player, ServerLevel level,
                            double itemPrice, String currency) {
        AfterSellHook hook = afterSell;
        if (hook != null) {
            hook.call(item.copy(), player, level, itemPrice, currency);
        }
    }
}
