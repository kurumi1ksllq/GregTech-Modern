package com.gregtechceu.gtceu.syncdata.data_transformers.collections;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Map;

public class MapTransformer<K, V> implements IValueTransformer<Map<K, V>> {

    private final IValueTransformer<K> keyTransformer;
    private final IValueTransformer<V> valueTransformer;

    public MapTransformer(IValueTransformer<K> keyTransformer, IValueTransformer<V> valueTransformer) {
        this.keyTransformer = keyTransformer;
        this.valueTransformer = valueTransformer;
    }

    @Override
    public boolean mustProvideObject() {
        return true;
    }

    @Override
    public void writeToBuffer(Map<K, V> value, FriendlyByteBuf buf) {
        buf.writeInt(value.size());
        for (var entry : value.entrySet()) {
            keyTransformer.writeToBuffer(entry.getKey(), buf);
            valueTransformer.writeToBuffer(entry.getValue(), buf);
        }
    }

    @Override
    public Map<K, V> readFromBuffer(FriendlyByteBuf buf, Map<K, V> currentValue) {
        if (currentValue == null) return null;
        currentValue.clear();
        var size = buf.readInt();
        for (int i=0; i<size; i++) {
            var key = keyTransformer.readFromBuffer(buf, null);
            var value = valueTransformer.readFromBuffer(buf, null);
            currentValue.put(key, value);
        }
        return currentValue;
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
        if (!(tag instanceof ListTag listTag)) return current;
        current.clear();
        for (Tag entryTag : listTag) {
            CompoundTag compound = (CompoundTag) entryTag;
            K key = keyTransformer.deserializeNBT(compound.get("k"), null);
            V value = valueTransformer.deserializeNBT(compound.get("v"), null);
            current.put(key, value);
        }
        return current;
    }
}
