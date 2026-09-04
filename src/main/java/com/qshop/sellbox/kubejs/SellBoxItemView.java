package com.qshop.sellbox.kubejs;

import dev.latvian.mods.rhino.BaseFunction;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeArray;
import dev.latvian.mods.rhino.NativeObject;
import dev.latvian.mods.rhino.Scriptable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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
        put(context, "hasTag", this, new BaseFunction() {
            @Override
            public Object call(Context callContext, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length == 0 || args[0] == null
                        || args[0] == Context.getUndefinedValue()) {
                    return false;
                }
                return hasTag(String.valueOf(args[0]));
            }
        });
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

    /** Returns whether this item belongs to the supplied item tag. */
    public boolean hasTag(String tagId) {
        if (tagId == null || tagId.isBlank()) return false;
        String normalized = tagId.charAt(0) == '#' ? tagId.substring(1) : tagId;
        ResourceLocation location = ResourceLocation.tryParse(normalized);
        if (location == null) return false;
        return stack.is(TagKey.create(Registries.ITEM, location));
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
