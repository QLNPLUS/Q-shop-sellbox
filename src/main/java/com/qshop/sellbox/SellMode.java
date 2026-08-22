package com.qshop.sellbox;

public enum SellMode {
    INTERVAL("qshop_sellbox.mode.interval"),
    CLOSED_GUI("qshop_sellbox.mode.closed_gui");

    private final String translationKey;

    SellMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public static SellMode fromId(int id) {
        return id >= 0 && id < values().length ? values()[id] : INTERVAL;
    }
}
