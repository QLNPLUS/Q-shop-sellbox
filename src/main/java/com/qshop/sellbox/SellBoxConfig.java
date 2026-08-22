package com.qshop.sellbox;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/** Forge COMMON config stored at config/qshop_sellbox-common.toml. */
public final class SellBoxConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> DEFAULT_CURRENCY = BUILDER
            .comment("Currency used when a price rule does not specify one.")
            .define("defaultCurrency", "coins", value -> value instanceof String s && !s.isBlank());
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PRICE_RULES = BUILDER
            .comment("item|price|currency[|nbt], for example minecraft:diamond|100|coins")
            .defineList("priceRules", List.of(), value -> value instanceof String s && !s.isBlank());
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> NBT_MULTIPLIERS = BUILDER
            .comment("nbt|multiplier applies to every item; item|nbt|multiplier targets one item")
            .defineList("nbtMultipliers", List.of(), value -> value instanceof String s && !s.isBlank());
    public static final ForgeConfigSpec.BooleanValue SHOW_PRICE_TOOLTIP = BUILDER
            .comment("Whether item tooltips show the configured sell price.")
            .define("showPriceTooltip", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAYOUT_DEBUG = BUILDER
            .comment("DEBUG ONLY. Enables the F8 GUI layout editor. Disabled by default.")
            .define("enableLayoutDebug", false);
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private static List<PriceRule> cachedRules = List.of();

    private SellBoxConfig() {}

    public static synchronized void refresh() {
        List<PriceRule> rules = new ArrayList<>();
        for (String raw : PRICE_RULES.get()) parsePriceRule(raw, rules);
        for (String raw : NBT_MULTIPLIERS.get()) parseMultiplier(raw, rules);
        cachedRules = List.copyOf(rules);
    }

    public static synchronized List<PriceRule> rules() {
        return cachedRules;
    }

    public static String defaultCurrency() {
        String currency = DEFAULT_CURRENCY.get();
        return currency == null || currency.isBlank() ? "coins" : currency;
    }

    public static boolean showPriceTooltip() {
        return SHOW_PRICE_TOOLTIP.get();
    }

    public static boolean layoutDebugEnabled() {
        return ENABLE_LAYOUT_DEBUG.get();
    }

    private static void parsePriceRule(String raw, List<PriceRule> rules) {
        try {
            String[] parts = raw.split("\\|", 4);
            if (parts.length < 2) return;
            String itemId = parts[0].trim();
            double price = Double.parseDouble(parts[1].trim());
            String currency = parts.length >= 3 && !parts[2].isBlank()
                    ? parts[2].trim() : defaultCurrency();
            CompoundTag nbt = parts.length == 4 ? NbtStrings.parse(parts[3].trim()) : new CompoundTag();
            if (valid(itemId, price)) {
                rules.add(new PriceRule(itemId, nbt, price, currency, PriceRule.Kind.PRICE, false));
            }
        } catch (Exception ignored) {
            // Ignore one malformed optional rule and keep the remaining config usable.
        }
    }

    private static void parseMultiplier(String raw, List<PriceRule> rules) {
        try {
            String[] parts = raw.split("\\|", 3);
            if (parts.length == 2) {
                addMultiplier(rules, "*", parts[0], parts[1]);
            } else if (parts.length == 3) {
                addMultiplier(rules, parts[0].trim(), parts[1], parts[2]);
            }
        } catch (Exception ignored) {
            // Ignore one malformed optional rule and keep the remaining config usable.
        }
    }

    private static void addMultiplier(List<PriceRule> rules, String itemId, String nbtText,
                                      String multiplierText) {
        double multiplier = Double.parseDouble(multiplierText.trim());
        CompoundTag nbt = NbtStrings.parse(nbtText.trim());
        if (valid(itemId, multiplier) && multiplier >= 0) {
            rules.add(new PriceRule(itemId, nbt, multiplier, "",
                    PriceRule.Kind.MULTIPLIER, false));
        }
    }

    private static boolean valid(String itemId, double value) {
        return itemId != null && !itemId.isBlank() && Double.isFinite(value) && value >= 0;
    }
}
