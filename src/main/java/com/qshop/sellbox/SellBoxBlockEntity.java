package com.qshop.sellbox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public final class SellBoxBlockEntity extends BlockEntity implements Container {
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
    private boolean onlyOwnerCanOpen;
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

    // ---- 原版容器接口 ----
    // 26.x 移除了旧的 IItemHandler 能力；物品能力现在是 Capabilities.Item.BLOCK（承载 ResourceHandler）。
    // 实现 Container 后由 SellBoxMod 用 VanillaContainerWrapper 桥接，漏斗/管道行为与 1.21.1 保持一致。

    @Override
    public int getContainerSize() {
        return items.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return items.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = items.getStackInSlot(slot);
        items.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.setStackInSlot(slot, stack);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            items.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }
    @Nullable public UUID owner() { return owner; }
    public String ownerName() { return ownerName; }
    public SellMode sellMode() { return sellMode; }
    public int saleIntervalTicks() { return saleIntervalTicks; }
    public boolean showActionBarNotification() { return actionBarNotifications; }
    public boolean showChatNotification() { return chatNotifications; }
    public boolean onlyOwnerCanOpen() { return onlyOwnerCanOpen; }

    public boolean canEditOwner(Player player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                || (owner != null && owner.equals(player.getUUID()));
    }

    public boolean canOpen(Player player) {
        return !onlyOwnerCanOpen || owner == null || owner.equals(player.getUUID());
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
                            boolean showActionBarNotification, boolean showChatNotification,
                            boolean onlyOwnerCanOpen) {
        sellMode = mode == null ? SellMode.INTERVAL : mode;
        saleIntervalTicks = Math.max(20, Math.min(intervalTicks, MAX_INTERVAL_TICKS));
        actionBarNotifications = showActionBarNotification;
        chatNotifications = showChatNotification;
        this.onlyOwnerCanOpen = onlyOwnerCanOpen;
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

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockState(worldPosition).is(SellBoxMod.SELL_BOX.get())
                && player.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64D;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putChild("inventory", items);
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
        output.putString("ownerName", ownerName);
        output.putInt("sellMode", sellMode.ordinal());
        output.putInt("saleIntervalTicks", saleIntervalTicks);
        output.putBoolean("showActionBarNotification", actionBarNotifications);
        output.putBoolean("showChatNotification", chatNotifications);
        output.putBoolean("onlyOwnerCanOpen", onlyOwnerCanOpen);
        if (nextSaleTick >= 0L) output.putLong("nextSaleTick", nextSaleTick);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.readChild("inventory", items);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("ownerName", "");
        sellMode = SellMode.fromId(input.getIntOr("sellMode", 0));
        saleIntervalTicks = Math.max(20, Math.min(input.getIntOr("saleIntervalTicks", 1200), MAX_INTERVAL_TICKS));
        actionBarNotifications = input.getBooleanOr("showActionBarNotification", true);
        chatNotifications = input.getBooleanOr("showChatNotification", true);
        onlyOwnerCanOpen = input.getBooleanOr("onlyOwnerCanOpen", false);
        nextSaleTick = input.getLongOr("nextSaleTick", -1L);
    }

    /**
     * Drops the inventory when the box is removed. Replaces the old Block#onRemove override:
     * this hook runs before the level discards the block entity, so the contents are still readable.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            net.minecraft.world.item.ItemStack stack = items.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        items.extractItem(slot, stack.getCount(), false));
            }
        }
        super.preRemoveSideEffects(pos, state);
    }

}
