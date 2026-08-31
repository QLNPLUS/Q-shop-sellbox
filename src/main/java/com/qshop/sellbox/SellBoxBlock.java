package com.qshop.sellbox;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public final class SellBoxBlock extends BaseEntityBlock {
    public SellBoxBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SellBoxBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SellBoxBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof SellBoxBlockEntity box) {
            box.setOwner(player.getUUID(), player.getGameProfile().getName());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof SellBoxBlockEntity box) {
            serverPlayer.openMenu(new net.minecraft.world.MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.qshop_sellbox.sell_box");
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                        int id, net.minecraft.world.entity.player.Inventory inventory, Player p) {
                    return new SellBoxMenu(id, inventory, box);
                }
            }, pos);
            SellBoxNetwork.sendOwners(serverPlayer, box);
            SellBoxNetwork.sendPrices(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(
            net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        useWithoutItem(state, level, pos, player, hit);
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, SellBoxMod.SELL_BOX_ENTITY.get(), SellBoxBlockEntity::serverTick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SellBoxBlockEntity box) {
            for (int slot = 0; slot < box.items().getSlots(); slot++) {
                net.minecraft.world.item.ItemStack stack = box.items().getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                            box.items().extractItem(slot, stack.getCount(), false));
                }
            }
            level.removeBlockEntity(pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
