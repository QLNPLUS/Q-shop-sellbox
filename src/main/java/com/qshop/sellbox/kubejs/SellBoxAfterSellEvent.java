package com.qshop.sellbox.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/** Event fired after a sell-box item stack has been sold and paid out. */
public final class SellBoxAfterSellEvent implements KubeEvent {
    private final ItemStack item;
    @Nullable
    private final ServerPlayer player;
    @Nullable
    private final ServerLevel level;
    private final double itemPrice;
    private final String currency;

    public SellBoxAfterSellEvent() {
        this(ItemStack.EMPTY, null, null, 0D, "");
    }

    public SellBoxAfterSellEvent(ItemStack item, @Nullable ServerPlayer player, @Nullable ServerLevel level,
                                 double itemPrice, String currency) {
        this.item = item.copy();
        this.player = player;
        this.level = level;
        this.itemPrice = itemPrice;
        this.currency = currency == null ? "" : currency;
    }

    public SellBoxItemView getItem() {
        return new SellBoxItemView(item);
    }

    @Nullable
    public ServerPlayer getPlayer() {
        return player;
    }

    @Nullable
    public ServerLevel getLevel() {
        return level;
    }

    /** Unit price of one item in this sold stack. */
    public double getItemPrice() {
        return itemPrice;
    }

    public String getCurrency() {
        return currency;
    }
}
