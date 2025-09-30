package com.gregtechceu.gtceu.syncdata.data_transformers.collections;

import com.gregtechceu.gtceu.syncdata.ISyncManaged;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

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
    public Tag serializeNBT(T[] value, ISyncManaged holder) {
        ListTag listTag = new ListTag();
        for (T element : value) {
            listTag.add(elementTransformer.serializeNBT(element, null));
        }
        return listTag;
    }

    @Override
    public T[] deserializeNBT(Tag tag, ISyncManaged holder, T[] currentVal) {
        if (!(tag instanceof ListTag listTag)) throw new IllegalArgumentException("Expected ListTag");

        for (int i = 0; i < listTag.size(); i++) {
            if (elementTransformer.mustProvideObject())
                elementTransformer.deserializeNBT(listTag.get(i), null, currentVal[i]);
            else currentVal[i] = elementTransformer.deserializeNBT(listTag.get(i), null, null);
        }
        return currentVal;
    }
}
