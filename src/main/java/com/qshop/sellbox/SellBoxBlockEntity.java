package com.qshop.sellbox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public final class SellBoxBlockEntity extends BlockEntity {
    public static final int MAX_INTERVAL_TICKS = 20 * 60 * 60 * 24 * 7;
    private static final int CLOSE_SALE_DELAY_TICKS = 2;

    private final ItemStackHandler items = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    @Nullable
    private UUID owner;
    private String ownerName = "";
    private SellMode sellMode = SellMode.INTERVAL;
    private int saleIntervalTicks = 1200;
    private boolean actionBarNotifications = true;
    private boolean chatNotifications = true;
    private long nextSaleTick = -1L;
    private long pendingCloseSaleTick = -1L;

    public SellBoxBlockEntity(BlockPos pos, BlockState state) {
        super(SellBoxMod.SELL_BOX_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SellBoxBlockEntity box) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        long now = serverLevel.getGameTime();
        if (box.pendingCloseSaleTick >= 0L && now >= box.pendingCloseSaleTick) {
            box.pendingCloseSaleTick = -1L;
            if (box.sellMode == SellMode.CLOSED_GUI) {
                SellBoxSaleService.sellContents(box);
            }
        }
        if (box.sellMode != SellMode.INTERVAL) return;
        if (box.nextSaleTick < 0L) {
            box.nextSaleTick = now + box.saleIntervalTicks;
            return;
        }
        if (now >= box.nextSaleTick) {
            box.nextSaleTick = now + box.saleIntervalTicks;
            SellBoxSaleService.sellContents(box);
        }
    }

    public IItemHandler items() { return items; }
    @Nullable public UUID owner() { return owner; }
    public String ownerName() { return ownerName; }
    public SellMode sellMode() { return sellMode; }
    public int saleIntervalTicks() { return saleIntervalTicks; }
    public boolean showActionBarNotification() { return actionBarNotifications; }
    public boolean showChatNotification() { return chatNotifications; }

    public boolean canEditOwner(Player player) {
        return player.hasPermissions(2) || (owner != null && owner.equals(player.getUUID()));
    }

    public void setOwner(UUID uuid, String name) {
        owner = uuid;
        ownerName = name == null ? "" : name;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setSettings(SellMode mode, int intervalTicks,
                            boolean showActionBarNotification, boolean showChatNotification) {
        sellMode = mode == null ? SellMode.INTERVAL : mode;
        saleIntervalTicks = Math.max(20, Math.min(intervalTicks, MAX_INTERVAL_TICKS));
        actionBarNotifications = showActionBarNotification;
        chatNotifications = showChatNotification;
        if (sellMode != SellMode.CLOSED_GUI) pendingCloseSaleTick = -1L;
        nextSaleTick = level == null ? -1L : level.getGameTime() + saleIntervalTicks;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void onMenuClosed() {
        if (sellMode == SellMode.CLOSED_GUI && level instanceof ServerLevel serverLevel) {
            pendingCloseSaleTick = serverLevel.getGameTime() + CLOSE_SALE_DELAY_TICKS;
        }
    }

    public boolean stillValid(Player player) {
        return level != null && level.getBlockState(worldPosition).is(SellBoxMod.SELL_BOX.get())
                && player.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64D;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", items.serializeNBT(registries));
        if (owner != null) tag.putUUID("owner", owner);
        tag.putString("ownerName", ownerName);
        tag.putInt("sellMode", sellMode.ordinal());
        tag.putInt("saleIntervalTicks", saleIntervalTicks);
        tag.putBoolean("showActionBarNotification", actionBarNotifications);
        tag.putBoolean("showChatNotification", chatNotifications);
        if (nextSaleTick >= 0L) tag.putLong("nextSaleTick", nextSaleTick);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) items.deserializeNBT(registries, tag.getCompound("inventory"));
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        ownerName = tag.getString("ownerName");
        sellMode = SellMode.fromId(tag.getInt("sellMode"));
        saleIntervalTicks = Math.max(20, Math.min(tag.getInt("saleIntervalTicks"), MAX_INTERVAL_TICKS));
        if (!tag.contains("saleIntervalTicks")) saleIntervalTicks = 1200;
        actionBarNotifications = !tag.contains("showActionBarNotification")
                || tag.getBoolean("showActionBarNotification");
        chatNotifications = !tag.contains("showChatNotification")
                || tag.getBoolean("showChatNotification");
        nextSaleTick = tag.contains("nextSaleTick") ? tag.getLong("nextSaleTick") : -1L;
    }

}
