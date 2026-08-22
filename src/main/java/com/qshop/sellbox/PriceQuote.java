package com.qshop.sellbox;

import java.util.Locale;

public record PriceQuote(double price, String currency) {
    public String formattedPrice() {
        if (Math.abs(price - Math.rint(price)) < 0.000001D) {
            return String.format(Locale.ROOT, "%.0f", price);
        }
        return String.format(Locale.ROOT, "%.2f", price);
    }
}
