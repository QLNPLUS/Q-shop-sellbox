package com.qshop.sellbox;

import com.qshop.currency.CurrencyRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class SellBoxNetwork {
    private static final String PROTOCOL = "3";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SellBoxMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int packetId;

    private SellBoxNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(packetId++, SyncOwnersPacket.class,
                SyncOwnersPacket::encode, SyncOwnersPacket::decode, SyncOwnersPacket::handle);
        CHANNEL.registerMessage(packetId++, ClaimOwnerPacket.class,
                ClaimOwnerPacket::encode, ClaimOwnerPacket::decode, ClaimOwnerPacket::handle);
        CHANNEL.registerMessage(packetId++, SetSettingsPacket.class,
                SetSettingsPacket::encode, SetSettingsPacket::decode, SetSettingsPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncPricesPacket.class,
                SyncPricesPacket::encode, SyncPricesPacket::decode, SyncPricesPacket::handle);
        CHANNEL.registerMessage(packetId++, QueryPricePacket.class,
                QueryPricePacket::encode, QueryPricePacket::decode, QueryPricePacket::handle);
        CHANNEL.registerMessage(packetId++, PriceResultPacket.class,
                PriceResultPacket::encode, PriceResultPacket::decode, PriceResultPacket::handle);
    }

    public static void sendOwners(ServerPlayer player, SellBoxBlockEntity box) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new SyncOwnersPacket(box.getBlockPos(), box.owner(), box.ownerName(),
                        box.sellMode(), box.saleIntervalTicks(),
                        box.showActionBarNotification(), box.showChatNotification()));
    }

    public static void broadcastOwner(MinecraftServer server, SellBoxBlockEntity box) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.distanceToSqr(box.getBlockPos().getX() + 0.5D,
                    box.getBlockPos().getY() + 0.5D, box.getBlockPos().getZ() + 0.5D) <= 64D
                    && player.containerMenu instanceof SellBoxMenu menu
                    && menu.pos().equals(box.getBlockPos())) {
                sendOwners(player, box);
            }
        }
    }

    public static void sendPrices(ServerPlayer player) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new SyncPricesPacket(SellBoxConfig.defaultCurrency(), SellBoxPrices.serverRules(),
                        SellBoxConfig.showPriceTooltip(), SellBoxPrices.hasDynamicPriceFunction(),
                        currencyDisplayNames()));
    }

    public static void sendClaimOwner(BlockPos pos) {
        CHANNEL.sendToServer(new ClaimOwnerPacket(pos));
    }

    public static void sendSettings(BlockPos pos, SellMode mode, int intervalTicks,
                                    boolean showActionBarNotification, boolean showChatNotification) {
        CHANNEL.sendToServer(new SetSettingsPacket(pos, mode, intervalTicks,
                showActionBarNotification, showChatNotification));
    }

    public static void sendPriceQuery(int requestId, net.minecraft.world.item.ItemStack stack) {
        CHANNEL.sendToServer(new QueryPricePacket(requestId, stack.copy()));
    }

    public static void broadcastPrices(MinecraftServer server) {
        SyncPricesPacket packet = new SyncPricesPacket(SellBoxConfig.defaultCurrency(),
                SellBoxPrices.serverRules(), SellBoxConfig.showPriceTooltip(),
                SellBoxPrices.hasDynamicPriceFunction(), currencyDisplayNames());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    private static void enqueueClient(Runnable action) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> action.run());
    }

    private static Map<String, String> currencyDisplayNames() {
        Map<String, String> names = new LinkedHashMap<>();
        CurrencyRegistry.all().forEach(currency -> names.put(currency.id, currency.displayName));
        return names;
    }

    public record SyncOwnersPacket(BlockPos pos, UUID owner, String ownerName,
                                   SellMode sellMode, int saleIntervalTicks,
                                   boolean showActionBarNotification, boolean showChatNotification) {
        public static void encode(SyncOwnersPacket packet, FriendlyByteBuf buf) {
            buf.writeBlockPos(packet.pos);
            buf.writeBoolean(packet.owner != null);
            if (packet.owner != null) buf.writeUUID(packet.owner);
            buf.writeUtf(packet.ownerName == null ? "" : packet.ownerName, 64);
            buf.writeEnum(packet.sellMode);
            buf.writeVarInt(packet.saleIntervalTicks);
            buf.writeBoolean(packet.showActionBarNotification);
            buf.writeBoolean(packet.showChatNotification);
        }

        public static SyncOwnersPacket decode(FriendlyByteBuf buf) {
            BlockPos pos = buf.readBlockPos();
            UUID owner = buf.readBoolean() ? buf.readUUID() : null;
            String ownerName = buf.readUtf(64);
            SellMode sellMode = buf.readEnum(SellMode.class);
            int saleIntervalTicks = buf.readVarInt();
            boolean showActionBarNotification = buf.readBoolean();
            boolean showChatNotification = buf.readBoolean();
            return new SyncOwnersPacket(pos, owner, ownerName, sellMode, saleIntervalTicks,
                    showActionBarNotification, showChatNotification);
        }

        public static void handle(SyncOwnersPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> enqueueClient(() -> com.qshop.sellbox.client.SellBoxClient.applyOwners(packet)));
            context.setPacketHandled(true);
        }
    }

    public record ClaimOwnerPacket(BlockPos pos) {
        public static void encode(ClaimOwnerPacket packet, FriendlyByteBuf buf) {
            buf.writeBlockPos(packet.pos);
        }

        public static ClaimOwnerPacket decode(FriendlyByteBuf buf) {
            return new ClaimOwnerPacket(buf.readBlockPos());
        }

        public static void handle(ClaimOwnerPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                context.enqueueWork(() -> {
                    if (!(sender.serverLevel().getBlockEntity(packet.pos) instanceof SellBoxBlockEntity box)) return;
                    if (!box.stillValid(sender)) return;
                    box.setOwner(sender.getUUID(), sender.getGameProfile().getName());
                    broadcastOwner(sender.server, box);
                });
            }
            context.setPacketHandled(true);
        }
    }

    public record SetSettingsPacket(BlockPos pos, SellMode mode, int intervalTicks,
                                    boolean showActionBarNotification, boolean showChatNotification) {
        public static void encode(SetSettingsPacket packet, FriendlyByteBuf buf) {
            buf.writeBlockPos(packet.pos);
            buf.writeEnum(packet.mode);
            buf.writeVarInt(packet.intervalTicks);
            buf.writeBoolean(packet.showActionBarNotification);
            buf.writeBoolean(packet.showChatNotification);
        }

        public static SetSettingsPacket decode(FriendlyByteBuf buf) {
            return new SetSettingsPacket(buf.readBlockPos(), buf.readEnum(SellMode.class), buf.readVarInt(),
                    buf.readBoolean(), buf.readBoolean());
        }

        public static void handle(SetSettingsPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                context.enqueueWork(() -> {
                    if (!(sender.serverLevel().getBlockEntity(packet.pos) instanceof SellBoxBlockEntity box)
                            || !box.stillValid(sender) || !box.canEditOwner(sender)) return;
                    SellMode previousMode = box.sellMode();
                    boolean menuStillOpen = sender.containerMenu instanceof SellBoxMenu menu
                            && menu.pos().equals(packet.pos());
                    box.setSettings(packet.mode, packet.intervalTicks,
                            packet.showActionBarNotification, packet.showChatNotification);
                    // The server can close the menu before this client settings packet arrives.
                    // In that case the old mode missed the close event, so sell immediately
                    // when this save changes the box into GUI-close selling mode.
                    if (packet.mode == SellMode.CLOSED_GUI
                            && previousMode != SellMode.CLOSED_GUI && !menuStillOpen) {
                        SellBoxSaleService.sellContents(box);
                    }
                    broadcastOwner(sender.server, box);
                });
            }
            context.setPacketHandled(true);
        }
    }

    public record SyncPricesPacket(String defaultCurrency, List<PriceRule> rules,
                                   boolean showPriceTooltip, boolean hasDynamicPriceFunction,
                                   Map<String, String> currencyDisplayNames) {
        public static void encode(SyncPricesPacket packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.defaultCurrency, 64);
            buf.writeBoolean(packet.showPriceTooltip);
            buf.writeBoolean(packet.hasDynamicPriceFunction);
            buf.writeVarInt(packet.currencyDisplayNames.size());
            for (var entry : packet.currencyDisplayNames.entrySet()) {
                buf.writeUtf(entry.getKey(), 64);
                buf.writeUtf(entry.getValue(), 128);
            }
            buf.writeVarInt(packet.rules.size());
            for (PriceRule rule : packet.rules) {
                buf.writeUtf(rule.itemId(), 256);
                buf.writeNbt(rule.nbt());
                buf.writeDouble(rule.value());
                buf.writeUtf(rule.currency(), 64);
                buf.writeEnum(rule.kind());
                buf.writeBoolean(rule.kubeJs());
            }
        }

        public static SyncPricesPacket decode(FriendlyByteBuf buf) {
            String currency = buf.readUtf(64);
            boolean showPriceTooltip = buf.readBoolean();
            boolean hasDynamicPriceFunction = buf.readBoolean();
            int nameCount = Math.min(buf.readVarInt(), 10000);
            Map<String, String> currencyDisplayNames = new LinkedHashMap<>();
            for (int i = 0; i < nameCount; i++) {
                currencyDisplayNames.put(buf.readUtf(64), buf.readUtf(128));
            }
            int count = Math.min(buf.readVarInt(), 100000);
            List<PriceRule> rules = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String item = buf.readUtf(256);
                net.minecraft.nbt.CompoundTag nbt = buf.readNbt();
                double value = buf.readDouble();
                String ruleCurrency = buf.readUtf(64);
                PriceRule.Kind kind = buf.readEnum(PriceRule.Kind.class);
                boolean kubeJs = buf.readBoolean();
                rules.add(new PriceRule(item, nbt, value, ruleCurrency, kind, kubeJs));
            }
            return new SyncPricesPacket(currency, rules, showPriceTooltip,
                    hasDynamicPriceFunction, currencyDisplayNames);
        }

        public static void handle(SyncPricesPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> enqueueClient(() ->
                    com.qshop.sellbox.client.SellBoxClient.applyPrices(packet)));
            context.setPacketHandled(true);
        }
    }

    public record QueryPricePacket(int requestId, net.minecraft.world.item.ItemStack stack) {
        public static void encode(QueryPricePacket packet, FriendlyByteBuf buf) {
            buf.writeVarInt(packet.requestId);
            buf.writeItem(packet.stack);
        }

        public static QueryPricePacket decode(FriendlyByteBuf buf) {
            return new QueryPricePacket(buf.readVarInt(), buf.readItem());
        }

        public static void handle(QueryPricePacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                context.enqueueWork(() -> {
                    PriceQuote quote = SellBoxPrices.resolve(packet.stack);
                    CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sender),
                            PriceResultPacket.from(packet.requestId, quote));
                });
            }
            context.setPacketHandled(true);
        }
    }

    public record PriceResultPacket(int requestId, boolean found, double price, String currency) {
        private static PriceResultPacket from(int requestId, PriceQuote quote) {
            return quote == null
                    ? new PriceResultPacket(requestId, false, 0D, "")
                    : new PriceResultPacket(requestId, true, quote.price(), quote.currency());
        }

        public static void encode(PriceResultPacket packet, FriendlyByteBuf buf) {
            buf.writeVarInt(packet.requestId);
            buf.writeBoolean(packet.found);
            if (packet.found) {
                buf.writeDouble(packet.price);
                buf.writeUtf(packet.currency, 64);
            }
        }

        public static PriceResultPacket decode(FriendlyByteBuf buf) {
            int requestId = buf.readVarInt();
            if (!buf.readBoolean()) return new PriceResultPacket(requestId, false, 0D, "");
            return new PriceResultPacket(requestId, true, buf.readDouble(), buf.readUtf(64));
        }

        public static void handle(PriceResultPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> enqueueClient(() ->
                    com.qshop.sellbox.client.SellBoxClient.applyPriceResult(packet)));
            context.setPacketHandled(true);
        }
    }
}
