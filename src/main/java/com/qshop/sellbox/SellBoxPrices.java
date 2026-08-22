package com.qshop.sellbox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SellBoxPrices {
    private static final Map<String, PriceRule> KJS_RULES = new LinkedHashMap<>();
    private static DynamicPriceResolver KJS_DYNAMIC_RESOLVER;
    private static List<PriceRule> CLIENT_RULES = List.of();
    private static String CLIENT_DEFAULT_CURRENCY = "coins";

    private SellBoxPrices() {}

    public static synchronized void setKubeJsPrice(String itemId, CompoundTag nbt,
                                                    double price, String currency) {
        if (!valid(itemId, price)) return;
        PriceRule rule = new PriceRule(itemId, nbt, price,
                currency == null || currency.isBlank() ? SellBoxConfig.defaultCurrency() : currency,
                PriceRule.Kind.PRICE, true);
        KJS_RULES.put(rule.key(), rule);
        broadcast();
    }

    public static synchronized void setKubeJsMultiplier(String itemId, CompoundTag nbt,
                                                         double multiplier, String currency) {
        if (!valid(itemId, multiplier) || multiplier < 0) return;
        PriceRule rule = new PriceRule(itemId, nbt, multiplier,
                currency == null ? "" : currency, PriceRule.Kind.MULTIPLIER, true);
        KJS_RULES.put(rule.key(), rule);
        broadcast();
    }

    public static synchronized void setKubeJsPriceFunction(DynamicPriceResolver resolver) {
        KJS_DYNAMIC_RESOLVER = resolver;
        broadcast();
    }

    public static synchronized boolean hasDynamicPriceFunction() {
        return KJS_DYNAMIC_RESOLVER != null;
    }

    public static synchronized List<PriceRule> serverRules() {
        List<PriceRule> rules = new ArrayList<>(SellBoxConfig.rules());
        rules.addAll(KJS_RULES.values());
        return List.copyOf(rules);
    }

    public static synchronized void setClientRules(String currency, List<PriceRule> rules) {
        CLIENT_DEFAULT_CURRENCY = currency == null || currency.isBlank() ? "coins" : currency;
        CLIENT_RULES = List.copyOf(rules);
    }

    public static synchronized void clearClientRules() {
        CLIENT_RULES = List.of();
    }

    public static PriceQuote resolve(ItemStack stack) {
        DynamicPriceResolver resolver;
        synchronized (SellBoxPrices.class) {
            resolver = KJS_DYNAMIC_RESOLVER;
        }
        if (resolver != null) {
            return resolver.resolve(stack.copy());
        }
        return resolve(stack, serverRules(), SellBoxConfig.defaultCurrency());
    }

    public static synchronized PriceQuote resolveClient(ItemStack stack) {
        return resolve(stack, CLIENT_RULES, CLIENT_DEFAULT_CURRENCY);
    }

    public static PriceQuote resolve(ItemStack stack, List<PriceRule> rules, String defaultCurrency) {
        PriceRule base = bestPrice(stack, rules, true);
        if (base == null) return null;
        double value = base.value();
        String currency = base.currency().isBlank() ? defaultCurrency : base.currency();
        List<PriceRule> multipliers = matchingMultipliers(stack, rules);
        boolean hasKubeJsMultiplier = multipliers.stream().anyMatch(PriceRule::kubeJs);
        for (PriceRule multiplier : multipliers) {
            // KubeJS multipliers replace config multipliers, while all matching
            // rules within the selected source compose in declaration order.
            if (multiplier.kubeJs() != hasKubeJsMultiplier) continue;
            value *= multiplier.value();
            if (!multiplier.currency().isBlank()) currency = multiplier.currency();
        }
        if (!Double.isFinite(value) || value <= 0) return null;
        return new PriceQuote(value, currency);
    }

    private static PriceRule bestPrice(ItemStack stack, List<PriceRule> rules, boolean preferKjs) {
        PriceRule best = null;
        for (PriceRule rule : rules) {
            if (rule.kind() != PriceRule.Kind.PRICE || !rule.matches(stack)) continue;
            if (best == null || compare(rule, best, preferKjs) > 0) best = rule;
        }
        return best;
    }

    private static List<PriceRule> matchingMultipliers(ItemStack stack, List<PriceRule> rules) {
        List<PriceRule> matches = new ArrayList<>();
        for (PriceRule rule : rules) {
            if (rule.kind() != PriceRule.Kind.MULTIPLIER || !rule.matches(stack)) continue;
            matches.add(rule);
        }
        return matches;
    }

    private static int compare(PriceRule left, PriceRule right, boolean preferKjs) {
        if (preferKjs && left.kubeJs() != right.kubeJs()) return left.kubeJs() ? 1 : -1;
        return Integer.compare(left.specificity(), right.specificity());
    }

    private static boolean valid(String itemId, double value) {
        return itemId != null && !itemId.isBlank() && Double.isFinite(value) && value >= 0;
    }

    private static void broadcast() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) SellBoxNetwork.broadcastPrices(server);
    }
}
