package com.qshop.sellbox;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SellBoxSavedData extends SavedData {
    public static final String DATA_ID = "qshop_sellbox_data";
    private final Map<UUID, PlayerRecord> players = new LinkedHashMap<>();
    private final Map<UUID, Map<String, Double>> pending = new LinkedHashMap<>();
    private final Map<UUID, NotificationFlags> pendingNotifications = new LinkedHashMap<>();
    private static final Codec<SellBoxSavedData> CODEC = CompoundTag.CODEC.xmap(
            SellBoxSavedData::load, SellBoxSavedData::saveTag);
    private static final SavedDataType<SellBoxSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SellBoxMod.MODID, DATA_ID),
            SellBoxSavedData::new, CODEC);

    public static SellBoxSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static SellBoxSavedData load(CompoundTag tag) {
        SellBoxSavedData data = new SellBoxSavedData();
        ListTag playerList = tag.getListOrEmpty("players");
        for (Tag raw : playerList) {
            CompoundTag entry = (CompoundTag) raw;
            UUID uuid = entry.read("uuid", UUIDUtil.CODEC).orElse(null);
            if (uuid == null) continue;
            data.players.put(uuid, new PlayerRecord(
                    uuid, entry.getStringOr("name", "")));
        }
        ListTag pendingList = tag.getListOrEmpty("pending");
        for (Tag raw : pendingList) {
            CompoundTag entry = (CompoundTag) raw;
            UUID uuid = entry.read("uuid", UUIDUtil.CODEC).orElse(null);
            if (uuid == null) continue;
            CompoundTag currencies = entry.getCompoundOrEmpty("currencies");
            Map<String, Double> values = new LinkedHashMap<>();
            for (String key : currencies.keySet()) values.put(key, currencies.getDoubleOr(key, 0D));
            data.pending.put(uuid, values);
            // Old data only had a chat sync message. Preserve that behavior for migration.
            data.pendingNotifications.put(uuid, new NotificationFlags(
                    entry.getBooleanOr("showActionBarNotification", false),
                    !entry.contains("showChatNotification") || entry.getBooleanOr("showChatNotification", false)));
        }
        return data;
    }

    public void recordPlayer(UUID uuid, String name) {
        if (uuid == null) return;
        PlayerRecord old = players.put(uuid, new PlayerRecord(uuid, name == null ? "" : name));
        if (old == null || !old.name().equals(name)) setDirty();
    }

    public boolean containsPlayer(UUID uuid) { return players.containsKey(uuid); }
    public PlayerRecord player(UUID uuid) { return players.get(uuid); }
    public List<PlayerRecord> players() { return new ArrayList<>(players.values()); }

    public void addPending(UUID uuid, String currency, double amount,
                           boolean showActionBarNotification, boolean showChatNotification) {
        if (uuid == null || currency == null || currency.isBlank() || !Double.isFinite(amount) || amount <= 0) return;
        pending.computeIfAbsent(uuid, ignored -> new LinkedHashMap<>())
                .merge(currency, amount, Double::sum);
        NotificationFlags current = pendingNotifications.getOrDefault(uuid, new NotificationFlags(false, false));
        pendingNotifications.put(uuid, current.merge(showActionBarNotification, showChatNotification));
        setDirty();
    }

    public PendingResult takePending(UUID uuid) {
        Map<String, Double> values = pending.remove(uuid);
        NotificationFlags notifications = pendingNotifications.remove(uuid);
        if (values != null || notifications != null) setDirty();
        if (values == null) values = Map.of();
        if (notifications == null) notifications = new NotificationFlags(false, true);
        return new PendingResult(new LinkedHashMap<>(values),
                notifications.showActionBarNotification(), notifications.showChatNotification());
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid) && !pending.get(uuid).isEmpty();
    }

    private CompoundTag saveTag() {
        ListTag playerList = new ListTag();
        for (PlayerRecord player : players.values()) {
            CompoundTag entry = new CompoundTag();
            entry.store("uuid", UUIDUtil.CODEC, player.uuid());
            entry.putString("name", player.name());
            playerList.add(entry);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("players", playerList);

        ListTag pendingList = new ListTag();
        for (var player : pending.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.store("uuid", UUIDUtil.CODEC, player.getKey());
            CompoundTag currencies = new CompoundTag();
            player.getValue().forEach(currencies::putDouble);
            entry.put("currencies", currencies);
            NotificationFlags notifications = pendingNotifications.getOrDefault(
                    player.getKey(), new NotificationFlags(false, true));
            entry.putBoolean("showActionBarNotification", notifications.showActionBarNotification());
            entry.putBoolean("showChatNotification", notifications.showChatNotification());
            pendingList.add(entry);
        }
        tag.put("pending", pendingList);
        return tag;
    }

    public record PlayerRecord(UUID uuid, String name) {}

    public record PendingResult(Map<String, Double> earnings,
                                boolean showActionBarNotification,
                                boolean showChatNotification) {}

    private record NotificationFlags(boolean showActionBarNotification,
                                     boolean showChatNotification) {
        private NotificationFlags merge(boolean actionBar, boolean chat) {
            return new NotificationFlags(showActionBarNotification || actionBar,
                    showChatNotification || chat);
        }
    }
}
