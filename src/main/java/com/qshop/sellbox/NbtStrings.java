package com.qshop.sellbox;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

/** Converts the JSON-shaped NBT accepted by KubeJS/config into Minecraft tags. */
public final class NbtStrings {
    private NbtStrings() {}

    public static CompoundTag parse(Object value) {
        if (value == null) return new CompoundTag();
        if (value instanceof CompoundTag tag) return tag.copy();
        if (value instanceof JsonElement element) return parseElement(element);
        String text = value.toString().trim();
        if (text.isEmpty()) return new CompoundTag();
        try {
            return TagParser.parseTag(text);
        } catch (Exception ignored) {
            try {
                return parseElement(com.google.gson.JsonParser.parseString(text));
            } catch (Exception ignoredJson) {
                throw new IllegalArgumentException("Invalid NBT: " + text, ignoredJson);
            }
        }
    }

    private static CompoundTag parseElement(JsonElement element) {
        String snbt = toSnbt(element);
        try {
            return TagParser.parseTag(snbt);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid NBT: " + snbt, e);
        }
    }

    private static String toSnbt(JsonElement element) {
        if (element.isJsonObject()) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (var entry : element.getAsJsonObject().entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append(entry.getKey()).append(':').append(toSnbt(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            StringBuilder out = new StringBuilder("[");
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) out.append(',');
                out.append(toSnbt(array.get(i)));
            }
            return out.append(']').toString();
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean() || primitive.isNumber()) return primitive.toString();
        String text = primitive.getAsString().replace("\\", "\\\\").replace("\"", "\\\"");
        return '"' + text + '"';
    }
}
