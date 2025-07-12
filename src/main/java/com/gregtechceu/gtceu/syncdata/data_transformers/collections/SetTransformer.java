package com.gregtechceu.gtceu.syncdata.data_transformers.collections;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashSet;
import java.util.Set;

public class SetTransformer<T> implements IValueTransformer<Set<T>> {

    private final IValueTransformer<T> elementTransformer;

    public SetTransformer(IValueTransformer<T> elementTransformer) {
        this.elementTransformer = elementTransformer;
    }

    @Override
    public void writeBufferPayload(FriendlyByteBuf buffer, Set<T> value) {
        buffer.writeVarInt(value.size());
        for (T element : value) {
            elementTransformer.writeBufferPayload(buffer, element);
        }
    }

    @Override
    public Set<T> readBufferPayload(FriendlyByteBuf buffer, Set<T> currentVal) {
        int size = buffer.readVarInt();
        return new HashSet<>(size);
    }

    @Override
    public Tag serializeNBT(Set<T> value) {
        ListTag tag = new ListTag();
        for (T element : value) {
            tag.add(elementTransformer.serializeNBT(element));
        }
        return tag;
    }

    @Override
    public Set<T> deserializeNBT(Tag tag, Set<T> currentVal) {
        if (!(tag instanceof ListTag listTag)) return Set.of();
        Set<T> set = new HashSet<>();
        for (Tag elementTag : listTag) {
            set.add(elementTransformer.deserializeNBT(elementTag, null));
        }
        return set;
    }
}
