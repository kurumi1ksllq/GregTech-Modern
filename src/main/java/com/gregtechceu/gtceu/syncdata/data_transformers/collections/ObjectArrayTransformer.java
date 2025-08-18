package com.gregtechceu.gtceu.syncdata.data_transformers.collections;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

public class ObjectArrayTransformer<T> implements IValueTransformer<T[]> {

    private final IValueTransformer<T> elementTransformer;

    @Override
    public boolean mustProvideObject() {
        return true;
    }

    public ObjectArrayTransformer(IValueTransformer<T> elementTransformer) {
        this.elementTransformer = elementTransformer;
    }

    @Override
    public void writeToBuffer(T[] value, FriendlyByteBuf buf) {
        buf.writeInt(value.length);
        for (T t : value) {
            elementTransformer.writeToBuffer(t, buf);
        }
    }

    @Override
    public T[] readFromBuffer(FriendlyByteBuf buf, T[] currentValue) {
        var length = buf.readInt();
        if (currentValue.length != length)
            throw new IllegalStateException("Attempting to read server sync array: Mismatch in array lengths.");
        for (int i = 0; i < length; i++) {
            currentValue[i] = elementTransformer.readFromBuffer(buf, null);
        }
        return currentValue;
    }

    @Override
    public Tag serializeNBT(T[] value) {
        ListTag listTag = new ListTag();
        for (T element : value) {
            listTag.add(elementTransformer.serializeNBT(element));
        }
        return listTag;
    }

    @Override
    public T[] deserializeNBT(Tag tag, T[] currentVal) {
        if (!(tag instanceof ListTag listTag)) throw new IllegalArgumentException("Expected ListTag");

        for (int i = 0; i < listTag.size(); i++) {
            if (elementTransformer.mustProvideObject())
                elementTransformer.deserializeNBT(listTag.get(i), currentVal[i]);
            else currentVal[i] = elementTransformer.deserializeNBT(listTag.get(i), null);
        }
        return currentVal;
    }
}
