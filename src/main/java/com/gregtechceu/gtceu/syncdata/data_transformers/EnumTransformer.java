package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public class EnumTransformer<E extends Enum<E>> implements IValueTransformer<E> {

    private final Class<E> enumClass;

    @SuppressWarnings("unchecked")
    public EnumTransformer(Class<? extends Enum<?>> enumClass) {
        this.enumClass = (Class<E>) enumClass;
    }

    @Override
    public void writeToBuffer(E value, FriendlyByteBuf buf) {
        buf.writeInt(value.ordinal());
    }

    @Override
    public E readFromBuffer(FriendlyByteBuf buf, E currentValue) {
        return enumClass.getEnumConstants()[buf.readInt()];
    }

    @Override
    public Tag serializeNBT(E value) {
        return StringTag.valueOf(value.name());
    }

    @Override
    public E deserializeNBT(Tag tag, @Nullable E currentVal) {
        return Enum.valueOf(enumClass, tag.getAsString());
    }
}
