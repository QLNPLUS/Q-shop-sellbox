package com.qshop.sellbox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** One exact price or multiplier rule. Rules are shared by server and client sync. */
public final class PriceRule {
    public enum Kind { PRICE, MULTIPLIER }

    private final String itemId;
    private final CompoundTag nbt;
    private final double value;
    private final String currency;
    private final Kind kind;
    private final boolean kubeJs;

    public PriceRule(String itemId, CompoundTag nbt, double value, String currency,
                     Kind kind, boolean kubeJs) {
        this.itemId = itemId == null ? "" : itemId;
        this.nbt = nbt == null ? new CompoundTag() : nbt.copy();
        this.value = value;
        this.currency = currency == null ? "" : currency;
        this.kind = kind == null ? Kind.PRICE : kind;
        this.kubeJs = kubeJs;
    }

    public String itemId() { return itemId; }
    public CompoundTag nbt() { return nbt.copy(); }
    public double value() { return value; }
    public String currency() { return currency; }
    public Kind kind() { return kind; }
    public boolean kubeJs() { return kubeJs; }
    public int specificity() { return nbt.getAllKeys().size(); }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String actualId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).toString();
        if (!"*".equals(itemId) && !itemId.equals(actualId)) {
            return false;
        }
        return NbtMatcher.matches(stack.getTag(), nbt);
    }

    public String key() {
        return kind + "|" + itemId + "|" + nbt;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PriceRule other)) return false;
        return Objects.equals(itemId, other.itemId)
                && Objects.equals(nbt, other.nbt)
                && Double.compare(value, other.value) == 0
                && Objects.equals(currency, other.currency)
                && kind == other.kind
                && kubeJs == other.kubeJs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, nbt, value, currency, kind, kubeJs);
    }
    
    private static final class NbtMatcher {
        private static boolean matches(CompoundTag actual, CompoundTag expected) {
            if (expected.isEmpty()) return true;
            if (actual == null) return false;
            for (String key : expected.getAllKeys()) {
                net.minecraft.nbt.Tag expectedValue = expected.get(key);
                net.minecraft.nbt.Tag actualValue = actual.get(key);
                if (expectedValue instanceof CompoundTag expectedCompound) {
                    if (!(actualValue instanceof CompoundTag actualCompound)
                            || !matches(actualCompound, expectedCompound)) {
                        return false;
                    }
                } else if (!Objects.equals(expectedValue, actualValue)) {
                    return false;
                }
            }
            return true;
        }
    }
}
