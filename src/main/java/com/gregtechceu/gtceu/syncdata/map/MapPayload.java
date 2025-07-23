package com.gregtechceu.gtceu.syncdata.map;

import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.ObjectTypedPayload;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class MapPayload extends ObjectTypedPayload<Map.Entry<ITypedPayload<?>, ITypedPayload<?>>[]> {

    public MapPayload() {}

    protected MapPayload(Map.Entry<ITypedPayload<?>, ITypedPayload<?>>[] payload) {
        setPayload(payload);
    }

    public static MapPayload of(Map.Entry<ITypedPayload<?>, ITypedPayload<?>>[] payload) {
        return new MapPayload(payload);
    }

    @Override
    public @Nullable Tag serializeNBT() {
        ListTag list = new ListTag();
        for (var entry : getPayload()) {
            ITypedPayload<?> keyPayload = entry.getKey();
            CompoundTag keyTag = new CompoundTag();
            keyTag.putByte("t", keyPayload.getType());
            Tag key = keyPayload.serializeNBT();
            if (key != null) {
                keyTag.put("p", key);
            }

            ITypedPayload<?> valuePayload = entry.getValue();
            CompoundTag valueTag = new CompoundTag();
            valueTag.putByte("t", valuePayload.getType());
            Tag value = valuePayload.serializeNBT();
            if (value != null) {
                valueTag.put("p", value);
            }

            CompoundTag tag = new CompoundTag();
            tag.put("k", keyTag);
            tag.put("v", valueTag);
            list.add(tag);
        }
        return list;
    }

    @Override
    public void deserializeNBT(Tag input) {
        if (!(input instanceof ListTag list)) {
            throw new IllegalArgumentException("Tag %s is not a ListTag".formatted(input));
        }
        // noinspection unchecked
        this.payload = new Map.Entry[list.size()];

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.isEmpty()) continue;

            CompoundTag keyTag = entry.getCompound("k");
            byte keyType = keyTag.getByte("t");
            Tag keyNbt = keyTag.get("p");

            ITypedPayload<?> key = TypedPayloadRegistries.create(keyType);
            key.deserializeNBT(keyNbt);

            CompoundTag valueTag = entry.getCompound("v");
            byte valueType = valueTag.getByte("t");
            Tag valueNbt = valueTag.get("p");

            ITypedPayload<?> value = TypedPayloadRegistries.create(valueType);
            value.deserializeNBT(valueNbt);

            payload[i] = Map.entry(key, value);
        }
    }

    @Override
    public void writePayload(FriendlyByteBuf buf) {
        buf.writeVarInt(payload.length);
        for (var entry : payload) {
            buf.writeByte(entry.getKey().getType());
            entry.getKey().writePayload(buf);

            buf.writeByte(entry.getValue().getType());
            entry.getValue().writePayload(buf);
        }
    }

    @Override
    public void readPayload(FriendlyByteBuf buf) {
        // noinspection unchecked
        payload = new Map.Entry[buf.readVarInt()];

        for (int i = 0; i < payload.length; i++) {
            byte keyType = buf.readByte();
            ITypedPayload<?> key = TypedPayloadRegistries.create(keyType);
            key.readPayload(buf);

            byte valueType = buf.readByte();
            ITypedPayload<?> value = TypedPayloadRegistries.create(valueType);
            value.readPayload(buf);

            payload[i] = Map.entry(key, value);
        }
    }
}
