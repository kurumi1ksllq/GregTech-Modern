package com.gregtechceu.gtceu.syncdata.data_transformers.collections;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class ListTransformer<T> implements IValueTransformer<List<T>> {

    private final IValueTransformer<T> elementTransformer;

    public ListTransformer(IValueTransformer<T> elementTransformer) {
        this.elementTransformer = elementTransformer;
    }

    @Override
    public void writeToBuffer(List<T> value, FriendlyByteBuf buf) {
        if (elementTransformer == null) return;
        buf.writeInt(value.size());
        for (var obj : value) {
            elementTransformer.writeToBuffer(obj, buf);
        }
    }

    @Override
    public List<T> readFromBuffer(FriendlyByteBuf buf, List<T> currentValue) {
        if (elementTransformer == null) return currentValue;
        var size = buf.readInt();
        List<T> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            var newVal = elementTransformer.readFromBuffer(buf, null);
            list.add(newVal);
        }
        return list;
    }

    @Override
    public Tag serializeNBT(List<T> value) {
        ListTag list = new ListTag();
        if (elementTransformer == null) return list;
        for (var obj : value) {
            list.add(elementTransformer.serializeNBT(obj));
        }
        return list;
    }

    @Override
    public List<T> deserializeNBT(Tag tag, List<T> current) {
        if (!(tag instanceof ListTag listTag) || elementTransformer == null) return List.of();
        return listTag.stream().map((t) -> elementTransformer.deserializeNBT(t, null)).toList();
    }
}
