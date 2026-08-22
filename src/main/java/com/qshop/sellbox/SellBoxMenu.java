package com.qshop.sellbox;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SellBoxMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final IItemHandler handler;
    private UUID owner;
    private String ownerName = "";
    private SellMode sellMode = SellMode.INTERVAL;
    private int saleIntervalTicks = 1200;
    private boolean showActionBarNotification = true;
    private boolean showChatNotification = true;
    private List<PlayerChoice> playerChoices = List.of();

    public SellBoxMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, readClientData(inventory, data));
    }

    private static ClientData readClientData(Inventory inventory, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        if (inventory.player.level().getBlockEntity(pos) instanceof SellBoxBlockEntity box) {
            return new ClientData(pos, box.items());
        }
        return new ClientData(pos, new net.minecraftforge.items.ItemStackHandler(27));
    }

    private SellBoxMenu(int id, Inventory inventory, ClientData data) {
        this(id, inventory, data.pos(), data.handler());
    }

    public SellBoxMenu(int id, Inventory inventory, SellBoxBlockEntity box) {
        this(id, inventory, box.getBlockPos(), box.items());
        this.owner = box.owner();
        this.ownerName = box.ownerName();
        this.sellMode = box.sellMode();
        this.saleIntervalTicks = box.saleIntervalTicks();
        this.showActionBarNotification = box.showActionBarNotification();
        this.showChatNotification = box.showChatNotification();
    }

    private SellBoxMenu(int id, Inventory inventory, BlockPos pos, IItemHandler handler) {
        super(SellBoxMod.SELL_BOX_MENU.get(), id);
        this.pos = pos;
        this.handler = handler;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(handler, col + row * 9,
                        8 + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public BlockPos pos() { return pos; }
    public UUID owner() { return owner; }
    public String ownerName() { return ownerName; }
    public SellMode sellMode() { return sellMode; }
    public int saleIntervalTicks() { return saleIntervalTicks; }
    public boolean showActionBarNotification() { return showActionBarNotification; }
    public boolean showChatNotification() { return showChatNotification; }
    public List<PlayerChoice> playerChoices() { return playerChoices; }

    public void setOwnerData(UUID owner, String ownerName, List<PlayerChoice> choices) {
        this.owner = owner;
        this.ownerName = ownerName == null ? "" : ownerName;
        this.playerChoices = List.copyOf(new ArrayList<>(choices));
    }

    public void setSettingsData(SellMode mode, int intervalTicks,
                                boolean showActionBarNotification, boolean showChatNotification) {
        this.sellMode = mode == null ? SellMode.INTERVAL : mode;
        this.saleIntervalTicks = Math.max(20, Math.min(intervalTicks, SellBoxBlockEntity.MAX_INTERVAL_TICKS));
        this.showActionBarNotification = showActionBarNotification;
        this.showChatNotification = showChatNotification;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(pos) instanceof SellBoxBlockEntity box
                && box.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return empty;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < 27) {
            if (!moveItemStackTo(original, 27, slots.size(), true)) return empty;
        } else if (!moveItemStackTo(original, 0, 27, false)) {
            return empty;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        if (!player.level().isClientSide
                && player.level().getBlockEntity(pos) instanceof SellBoxBlockEntity box) {
            box.onMenuClosed();
        }
        super.removed(player);
    }

    public record PlayerChoice(UUID uuid, String name) {}
    private record ClientData(BlockPos pos, IItemHandler handler) {}
}
