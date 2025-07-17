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
    public void writeBufferPayload(FriendlyByteBuf buffer, T[] value) {
        buffer.writeVarInt(value.length);
        for (T element : value) {
            elementTransformer.writeBufferPayload(buffer, element);
        }
    }

    @Override
    public T[] readBufferPayload(FriendlyByteBuf buffer, T[] currentVal) {
        int length = buffer.readVarInt();
        if (currentVal == null) return null;

        for (int i = 0; i < length; i++) {
            if (elementTransformer.mustProvideObject()) elementTransformer.readBufferPayload(buffer, currentVal[i]);
            else currentVal[i] = elementTransformer.readBufferPayload(buffer, null);
        }
        return currentVal;
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
