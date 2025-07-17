package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

public class EnumTransformer<E extends Enum<E>> implements IValueTransformer<E> {

    private final Class<E> enumClass;

    @SuppressWarnings("unchecked")
    public EnumTransformer(Class<? extends Enum<?>> enumClass) {
        this.enumClass = (Class<E>) enumClass;
    }

    @Override
    public void writeBufferPayload(FriendlyByteBuf buffer, E value) {
        buffer.writeVarInt(value.ordinal());
    }

    @Override
    public E readBufferPayload(FriendlyByteBuf buffer, E currentVal) {
        int ordinal = buffer.readVarInt();
        return enumClass.getEnumConstants()[ordinal];
    }

    @Override
    public Tag serializeNBT(E value) {
        return StringTag.valueOf(value.name());
    }

    @Override
    public E deserializeNBT(Tag tag, E currentVal) {
        return Enum.valueOf(enumClass, tag.getAsString());
    }
}
