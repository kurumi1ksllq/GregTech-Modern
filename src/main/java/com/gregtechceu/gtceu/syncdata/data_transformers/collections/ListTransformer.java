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
    public void writeBufferPayload(FriendlyByteBuf buffer, List<T> value) {
        buffer.writeInt(value.size());
        for (var obj : value) {
            elementTransformer.writeBufferPayload(buffer, obj);
        }
    }

    @Override
    public List<T> readBufferPayload(FriendlyByteBuf buffer, List<T> current) {
        var count = buffer.readInt();
        List<T> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(elementTransformer.readBufferPayload(buffer, null));
        }
        return list;
    }

    @Override
    public Tag serializeNBT(List<T> value) {
        ListTag list = new ListTag();
        for (var obj : value) {
            list.add(elementTransformer.serializeNBT(obj));
        }
        return list;
    }

    @Override
    public List<T> deserializeNBT(Tag tag, List<T> current) {
        if (!(tag instanceof ListTag listTag)) return List.of();
        return listTag.stream().map((t) -> elementTransformer.deserializeNBT(t, null)).toList();
    }
}
