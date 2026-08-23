package com.qshop.sellbox.kubejs;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeArray;
import dev.latvian.mods.rhino.NativeObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Script-friendly view of an item stack used by the dynamic price callback. */
public final class SellBoxItemView extends NativeObject {
    private final ItemStack stack;
    private final Context context;

    public SellBoxItemView(ItemStack stack) {
        this(stack, Context.enter());
    }

    SellBoxItemView(ItemStack stack, Context context) {
        super(context);
        this.stack = stack.copy();
        this.context = context;
        exposeProperties();
    }

    private void exposeProperties() {
        put(context, "id", this, getId());
        put(context, "count", this, getCount());
        put(context, "damage", this, getDamage());
        put(context, "maxDamage", this, getMaxDamage());
        put(context, "isDamaged", this, isDamaged());
        put(context, "nbt", this, getNbt());
    }

    public String getId() {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public int getCount() {
        return stack.getCount();
    }

    public int getDamage() {
        return stack.getDamageValue();
    }

    public int getMaxDamage() {
        return stack.getMaxDamage();
    }

    public boolean isDamaged() {
        return stack.isDamaged();
    }

    public ItemStack getStack() {
        return stack.copy();
    }

    /** Returns NBT as ordinary JavaScript objects, arrays, strings, and numbers. */
    public Object getNbt() {
        CompoundTag tag = stack.getTag();
        if (tag == null || tag.isEmpty()) return null;
        return toJsNbt(context, tag);
    }

    static Object toJsNbt(Context context, CompoundTag tag) {
        return toJsValue(context, tag);
    }

    private static Object toJsValue(Context context, Tag tag) {
        if (tag == null) return null;
        if (tag instanceof CompoundTag compound) {
            NativeObject object = new NativeObject(context);
            for (String key : compound.getAllKeys()) {
                object.put(context, key, object, toJsValue(context, compound.get(key)));
            }
            return object;
        }
        if (tag instanceof ListTag list) {
            Object[] values = new Object[list.size()];
            for (int index = 0; index < list.size(); index++) {
                values[index] = toJsValue(context, list.get(index));
            }
            return new NativeArray(context, values);
        }
        if (tag instanceof ByteArrayTag bytes) {
            byte[] values = bytes.getAsByteArray();
            Object[] result = new Object[values.length];
            for (int index = 0; index < values.length; index++) result[index] = values[index];
            return new NativeArray(context, result);
        }
        if (tag instanceof IntArrayTag ints) {
            int[] values = ints.getAsIntArray();
            Object[] result = new Object[values.length];
            for (int index = 0; index < values.length; index++) result[index] = values[index];
            return new NativeArray(context, result);
        }
        if (tag instanceof LongArrayTag longs) {
            long[] values = longs.getAsLongArray();
            Object[] result = new Object[values.length];
            for (int index = 0; index < values.length; index++) result[index] = values[index];
            return new NativeArray(context, result);
        }
        if (tag instanceof NumericTag numeric) return numeric.getAsNumber();
        if (tag instanceof StringTag string) return string.getAsString();
        return tag.getAsString();
    }
}
