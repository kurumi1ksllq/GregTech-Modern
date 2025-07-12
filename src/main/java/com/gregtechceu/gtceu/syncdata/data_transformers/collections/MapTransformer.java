package com.gregtechceu.gtceu.syncdata.data_transformers.collections;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

public class MapTransformer<K, V> implements IValueTransformer<Map<K, V>> {

    private final IValueTransformer<K> keyTransformer;
    private final IValueTransformer<V> valueTransformer;

    public MapTransformer(IValueTransformer<K> keyTransformer, IValueTransformer<V> valueTransformer) {
        this.keyTransformer = keyTransformer;
        this.valueTransformer = valueTransformer;
    }

    @Override
    public void writeBufferPayload(FriendlyByteBuf buffer, Map<K, V> map) {
        buffer.writeVarInt(map.size());
        for (var entry : map.entrySet()) {
            keyTransformer.writeBufferPayload(buffer, entry.getKey());
            valueTransformer.writeBufferPayload(buffer, entry.getValue());
        }
    }

    @Override
    public Map<K, V> readBufferPayload(FriendlyByteBuf buffer, Map<K, V> current) {
        int size = buffer.readVarInt();
        Map<K, V> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            K key = keyTransformer.readBufferPayload(buffer, null);
            V value = valueTransformer.readBufferPayload(buffer, null);
            map.put(key, value);
        }
        return map;
    }

    @Override
    public Tag serializeNBT(Map<K, V> value) {
        ListTag entries = new ListTag();
        for (var entry : value.entrySet()) {
            CompoundTag compound = new CompoundTag();
            compound.put("k", keyTransformer.serializeNBT(entry.getKey()));
            compound.put("v", valueTransformer.serializeNBT(entry.getValue()));
            entries.add(compound);
        }
        return entries;
    }

    @Override
    public Map<K, V> deserializeNBT(Tag tag, Map<K, V> current) {
        if (!(tag instanceof ListTag listTag)) return Map.of();
        Map<K, V> map = new HashMap<>();
        for (Tag entryTag : listTag) {
            CompoundTag compound = (CompoundTag) entryTag;
            K key = keyTransformer.deserializeNBT(compound.get("k"), null);
            V value = valueTransformer.deserializeNBT(compound.get("v"), null);
            map.put(key, value);
        }
        return map;
    }
}
