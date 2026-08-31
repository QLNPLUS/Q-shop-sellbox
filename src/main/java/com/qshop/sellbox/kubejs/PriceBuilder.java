package com.qshop.sellbox.kubejs;

import com.qshop.sellbox.NbtStrings;
import com.qshop.sellbox.PriceRule;
import com.qshop.sellbox.SellBoxPrices;
import net.minecraft.nbt.CompoundTag;

/** Fluent KubeJS price declaration. */
public final class PriceBuilder {
    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();
    private final String itemId;
    private CompoundTag nbt = new CompoundTag();
    private double value;
    private String currency = "";
    private PriceRule.Kind kind = PriceRule.Kind.PRICE;

    public PriceBuilder(String itemId) {
        this.itemId = itemId;
    }

    public PriceBuilder price(double value) {
        this.value = value;
        this.kind = PriceRule.Kind.PRICE;
        return this;
    }

    public PriceBuilder price(double value, String currency) {
        this.value = value;
        this.currency = currency == null ? "" : currency;
        this.kind = PriceRule.Kind.PRICE;
        return this;
    }

    public PriceBuilder nbt(Object nbt) {
        if (nbt != null && !(nbt instanceof String)
                && !(nbt instanceof CompoundTag)
                && !(nbt instanceof com.google.gson.JsonElement)) {
            nbt = GSON.toJsonTree(nbt);
        }
        this.nbt = NbtStrings.parse(nbt);
        return this;
    }

    public PriceBuilder whenNbt(Object nbt) {
        return nbt(nbt);
    }

    /** Adds one more NBT field to the match compound. */
    public PriceBuilder withNbt(String key, Object value) {
        if (value instanceof CompoundTag tag) {
            this.nbt.put(key, tag.copy());
            return this;
        }
        com.google.gson.JsonObject object = new com.google.gson.JsonObject();
        if (value != null && !(value instanceof String)
                && !(value instanceof CompoundTag)
                && !(value instanceof com.google.gson.JsonElement)) {
            value = GSON.toJsonTree(value);
        }
        if (value instanceof com.google.gson.JsonElement element) {
            object.add(key, element);
        } else {
            object.addProperty(key, String.valueOf(value));
        }
        this.nbt.merge(NbtStrings.parse(object));
        return this;
    }

    public PriceBuilder multiplier(double multiplier) {
        this.value = multiplier;
        this.kind = PriceRule.Kind.MULTIPLIER;
        return this;
    }

    public PriceBuilder multiply(double multiplier) {
        return multiplier(multiplier);
    }

    public boolean add() {
        if (kind == PriceRule.Kind.MULTIPLIER) {
            SellBoxPrices.setKubeJsMultiplier(itemId, nbt, value, currency);
        } else {
            SellBoxPrices.setKubeJsPrice(itemId, nbt, value, currency);
        }
        return true;
    }
}
