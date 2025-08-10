package com.gregtechceu.gtceu.syncdata.data_transformers.collections;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashSet;
import java.util.Set;

public class SetTransformer<T> implements IValueTransformer<Set<T>> {

    private final IValueTransformer<T> elementTransformer;

    @Override
    public boolean mustProvideObject() {
        return true;
    }

    @Override
    public void writeToBuffer(Set<T> value, FriendlyByteBuf buf) {
        buf.writeInt(value.size());
        for (T elem: value) {
            elementTransformer.writeToBuffer(elem, buf);
        }
    }

    @Override
    public Set<T> readFromBuffer(FriendlyByteBuf buf, Set<T> currentValue) {
        Set<T> set = new HashSet<>();
        var size = buf.readInt();
        for (int i=0; i<size; i++) {
            set.add(elementTransformer.readFromBuffer(buf, null));
        }
        return set;
    }

    public SetTransformer(IValueTransformer<T> elementTransformer) {
        this.elementTransformer = elementTransformer;
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
