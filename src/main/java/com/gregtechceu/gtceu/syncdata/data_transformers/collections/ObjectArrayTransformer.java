package com.gregtechceu.gtceu.syncdata.data_transformers.collections;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.IntFunction;

public class ObjectArrayTransformer<T> implements IValueTransformer<T[]> {

    private final IValueTransformer<T> elementTransformer;
    private final IntFunction<Object[]> arrayFactory;

    public ObjectArrayTransformer(IValueTransformer<T> elementTransformer, IntFunction<Object[]> arrayFactory) {
        this.elementTransformer = elementTransformer;
        this.arrayFactory = arrayFactory;
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
        @SuppressWarnings("unchecked")
        T[] array = (T[]) arrayFactory.apply(length);
        for (int i = 0; i < length; i++) {
            array[i] = elementTransformer.readBufferPayload(buffer, null);
        }
        return array;
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
        @SuppressWarnings("unchecked")
        T[] array = (T[]) arrayFactory.apply(listTag.size());
        for (int i = 0; i < listTag.size(); i++) {
            array[i] = elementTransformer.deserializeNBT(listTag.get(i), null);
        }
        return array;
    }
}
