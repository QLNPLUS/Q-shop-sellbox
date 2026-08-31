package com.qshop.sellbox;

import com.qshop.currency.CurrencyRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SellBoxNetwork {
    private static final String PROTOCOL = "3";

    private SellBoxNetwork() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(SellBoxNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);
        registrar.playToClient(SyncOwnersPacket.TYPE, SyncOwnersPacket.STREAM_CODEC,
                SyncOwnersPacket::handle);
        registrar.playToServer(ClaimOwnerPacket.TYPE, ClaimOwnerPacket.STREAM_CODEC,
                ClaimOwnerPacket::handle);
        registrar.playToServer(SetSettingsPacket.TYPE, SetSettingsPacket.STREAM_CODEC,
                SetSettingsPacket::handle);
        registrar.playToClient(SyncPricesPacket.TYPE, SyncPricesPacket.STREAM_CODEC,
                SyncPricesPacket::handle);
        registrar.playToServer(QueryPricePacket.TYPE, QueryPricePacket.STREAM_CODEC,
                QueryPricePacket::handle);
        registrar.playToClient(PriceResultPacket.TYPE, PriceResultPacket.STREAM_CODEC,
                PriceResultPacket::handle);
    }

    public static void sendOwners(ServerPlayer player, SellBoxBlockEntity box) {
        PacketDistributor.sendToPlayer(player,
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
        PacketDistributor.sendToPlayer(player,
                new SyncPricesPacket(SellBoxConfig.defaultCurrency(), SellBoxPrices.serverRules(),
                        SellBoxConfig.showPriceTooltip(), SellBoxPrices.hasDynamicPriceFunction(),
                        currencyDisplayNames()));
    }

    public static void sendClaimOwner(BlockPos pos) {
        PacketDistributor.sendToServer(new ClaimOwnerPacket(pos));
    }

    public static void sendSettings(BlockPos pos, SellMode mode, int intervalTicks,
                                    boolean showActionBarNotification, boolean showChatNotification) {
        PacketDistributor.sendToServer(new SetSettingsPacket(pos, mode, intervalTicks,
                showActionBarNotification, showChatNotification));
    }

    public static void sendPriceQuery(int requestId, net.minecraft.world.item.ItemStack stack) {
        PacketDistributor.sendToServer(new QueryPricePacket(requestId, stack.copy()));
    }

    public static void broadcastPrices(MinecraftServer server) {
        SyncPricesPacket packet = new SyncPricesPacket(SellBoxConfig.defaultCurrency(),
                SellBoxPrices.serverRules(), SellBoxConfig.showPriceTooltip(),
                SellBoxPrices.hasDynamicPriceFunction(), currencyDisplayNames());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    private static Map<String, String> currencyDisplayNames() {
        Map<String, String> names = new LinkedHashMap<>();
        CurrencyRegistry.all().forEach(currency -> names.put(currency.id, currency.displayName));
        return names;
    }

    public record SyncOwnersPacket(BlockPos pos, UUID owner, String ownerName,
                                   SellMode sellMode, int saleIntervalTicks,
                                   boolean showActionBarNotification, boolean showChatNotification)  implements CustomPacketPayload{
        public static final CustomPacketPayload.Type<SyncOwnersPacket> TYPE = new CustomPacketPayload.Type<>(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SellBoxMod.MODID, "sync_owners"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncOwnersPacket> STREAM_CODEC =
                CustomPacketPayload.codec(SyncOwnersPacket::encode, SyncOwnersPacket::decode);

        public static void encode(SyncOwnersPacket packet, RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(packet.pos);
            buf.writeBoolean(packet.owner != null);
            if (packet.owner != null) buf.writeUUID(packet.owner);
            buf.writeUtf(packet.ownerName == null ? "" : packet.ownerName, 64);
            buf.writeEnum(packet.sellMode);
            buf.writeVarInt(packet.saleIntervalTicks);
            buf.writeBoolean(packet.showActionBarNotification);
            buf.writeBoolean(packet.showChatNotification);
        }

        public static SyncOwnersPacket decode(RegistryFriendlyByteBuf buf) {
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

        public static void handle(SyncOwnersPacket packet, IPayloadContext context) {
                        context.enqueueWork(() -> com.qshop.sellbox.client.SellBoxClient.applyOwners(packet));
        }
        @Override
        public CustomPacketPayload.Type<SyncOwnersPacket> type() {
            return TYPE;
        }

    }

    public record ClaimOwnerPacket(BlockPos pos)  implements CustomPacketPayload{
        public static final CustomPacketPayload.Type<ClaimOwnerPacket> TYPE = new CustomPacketPayload.Type<>(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SellBoxMod.MODID, "claim_owner"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClaimOwnerPacket> STREAM_CODEC =
                CustomPacketPayload.codec(ClaimOwnerPacket::encode, ClaimOwnerPacket::decode);

        public static void encode(ClaimOwnerPacket packet, RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(packet.pos);
        }

        public static ClaimOwnerPacket decode(RegistryFriendlyByteBuf buf) {
            return new ClaimOwnerPacket(buf.readBlockPos());
        }

        public static void handle(ClaimOwnerPacket packet, IPayloadContext context) {
                        ServerPlayer sender = (ServerPlayer) context.player();
            if (sender != null) {
                context.enqueueWork(() -> {
                    if (!(sender.serverLevel().getBlockEntity(packet.pos) instanceof SellBoxBlockEntity box)) return;
                    if (!box.stillValid(sender)) return;
                    box.setOwner(sender.getUUID(), sender.getGameProfile().getName());
                    broadcastOwner(sender.server, box);
                });
            }
        }
        @Override
        public CustomPacketPayload.Type<ClaimOwnerPacket> type() {
            return TYPE;
        }

    }

    public record SetSettingsPacket(BlockPos pos, SellMode mode, int intervalTicks,
                                    boolean showActionBarNotification, boolean showChatNotification)  implements CustomPacketPayload{
        public static final CustomPacketPayload.Type<SetSettingsPacket> TYPE = new CustomPacketPayload.Type<>(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SellBoxMod.MODID, "set_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetSettingsPacket> STREAM_CODEC =
                CustomPacketPayload.codec(SetSettingsPacket::encode, SetSettingsPacket::decode);

        public static void encode(SetSettingsPacket packet, RegistryFriendlyByteBuf buf) {
            buf.writeBlockPos(packet.pos);
            buf.writeEnum(packet.mode);
            buf.writeVarInt(packet.intervalTicks);
            buf.writeBoolean(packet.showActionBarNotification);
            buf.writeBoolean(packet.showChatNotification);
        }

        public static SetSettingsPacket decode(RegistryFriendlyByteBuf buf) {
            return new SetSettingsPacket(buf.readBlockPos(), buf.readEnum(SellMode.class), buf.readVarInt(),
                    buf.readBoolean(), buf.readBoolean());
        }

        public static void handle(SetSettingsPacket packet, IPayloadContext context) {
                        ServerPlayer sender = (ServerPlayer) context.player();
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
        }
        @Override
        public CustomPacketPayload.Type<SetSettingsPacket> type() {
            return TYPE;
        }

    }

    public record SyncPricesPacket(String defaultCurrency, List<PriceRule> rules,
                                   boolean showPriceTooltip, boolean hasDynamicPriceFunction,
                                   Map<String, String> currencyDisplayNames)  implements CustomPacketPayload{
        public static final CustomPacketPayload.Type<SyncPricesPacket> TYPE = new CustomPacketPayload.Type<>(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SellBoxMod.MODID, "sync_prices"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPricesPacket> STREAM_CODEC =
                CustomPacketPayload.codec(SyncPricesPacket::encode, SyncPricesPacket::decode);

        public static void encode(SyncPricesPacket packet, RegistryFriendlyByteBuf buf) {
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

        public static SyncPricesPacket decode(RegistryFriendlyByteBuf buf) {
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

        public static void handle(SyncPricesPacket packet, IPayloadContext context) {
                        context.enqueueWork(() -> com.qshop.sellbox.client.SellBoxClient.applyPrices(packet));
        }
        @Override
        public CustomPacketPayload.Type<SyncPricesPacket> type() {
            return TYPE;
        }

    }

    public record QueryPricePacket(int requestId, net.minecraft.world.item.ItemStack stack)  implements CustomPacketPayload{
        public static final CustomPacketPayload.Type<QueryPricePacket> TYPE = new CustomPacketPayload.Type<>(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SellBoxMod.MODID, "query_price"));
        public static final StreamCodec<RegistryFriendlyByteBuf, QueryPricePacket> STREAM_CODEC =
                CustomPacketPayload.codec(QueryPricePacket::encode, QueryPricePacket::decode);

        public static void encode(QueryPricePacket packet, RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(packet.requestId);
            net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.stack);
        }

        public static QueryPricePacket decode(RegistryFriendlyByteBuf buf) {
            return new QueryPricePacket(buf.readVarInt(), net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }

        public static void handle(QueryPricePacket packet, IPayloadContext context) {
                        ServerPlayer sender = (ServerPlayer) context.player();
            if (sender != null) {
                context.enqueueWork(() -> {
                    PriceQuote quote = SellBoxPrices.resolve(packet.stack);
                    PacketDistributor.sendToPlayer(sender,
                            PriceResultPacket.from(packet.requestId, quote));
                });
            }
        }
        @Override
        public CustomPacketPayload.Type<QueryPricePacket> type() {
            return TYPE;
        }

    }

    public record PriceResultPacket(int requestId, boolean found, double price, String currency)  implements CustomPacketPayload{
        public static final CustomPacketPayload.Type<PriceResultPacket> TYPE = new CustomPacketPayload.Type<>(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(SellBoxMod.MODID, "price_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PriceResultPacket> STREAM_CODEC =
                CustomPacketPayload.codec(PriceResultPacket::encode, PriceResultPacket::decode);

        private static PriceResultPacket from(int requestId, PriceQuote quote) {
            return quote == null
                    ? new PriceResultPacket(requestId, false, 0D, "")
                    : new PriceResultPacket(requestId, true, quote.price(), quote.currency());
        }

        public static void encode(PriceResultPacket packet, RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(packet.requestId);
            buf.writeBoolean(packet.found);
            if (packet.found) {
                buf.writeDouble(packet.price);
                buf.writeUtf(packet.currency, 64);
            }
        }

        public static PriceResultPacket decode(RegistryFriendlyByteBuf buf) {
            int requestId = buf.readVarInt();
            if (!buf.readBoolean()) return new PriceResultPacket(requestId, false, 0D, "");
            return new PriceResultPacket(requestId, true, buf.readDouble(), buf.readUtf(64));
        }

        public static void handle(PriceResultPacket packet, IPayloadContext context) {
                        context.enqueueWork(() -> com.qshop.sellbox.client.SellBoxClient.applyPriceResult(packet));
        }
        @Override
        public CustomPacketPayload.Type<PriceResultPacket> type() {
            return TYPE;
        }

    }
}
