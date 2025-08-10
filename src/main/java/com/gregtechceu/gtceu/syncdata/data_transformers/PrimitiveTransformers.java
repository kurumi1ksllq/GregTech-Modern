package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public class PrimitiveTransformers {
    public static class IntTransformer implements IValueTransformer<Integer> {
        @Override
        public void writeToBuffer(Integer value, FriendlyByteBuf buf) {
            buf.writeVarInt(value);
        }

        @Override
        public Integer readFromBuffer(FriendlyByteBuf buf, Integer currentValue) {
            return buf.readVarInt();
        }

        @Override
        public Tag serializeNBT(Integer value) {
            return IntTag.valueOf(value);
        }

        @Override
        public Integer deserializeNBT(Tag tag, Integer currentVal) {
            return (tag instanceof IntTag intTag) ? intTag.getAsInt() : 0;
        }
    }

    public static class LongTransformer implements IValueTransformer<Long> {
        @Override
        public void writeToBuffer(Long value, FriendlyByteBuf buf) {
            buf.writeLong(value);
        }

        @Override
        public Long readFromBuffer(FriendlyByteBuf buf, Long currentValue) {
            return buf.readLong();
        }

        @Override
        public Tag serializeNBT(Long value) {
            return LongTag.valueOf(value);
        }

        @Override
        public Long deserializeNBT(Tag tag, @Nullable Long currentVal) {
            return (tag instanceof LongTag longTag) ? longTag.getAsLong() : 0L;
        }
    }

    public static class FloatTransformer implements IValueTransformer<Float> {
        @Override
        public void writeToBuffer(Float value, FriendlyByteBuf buf) {
            buf.writeFloat(value);
        }

        @Override
        public Float readFromBuffer(FriendlyByteBuf buf, Float currentValue) {
            return buf.readFloat();
        }

        @Override
        public Tag serializeNBT(Float value) {
            return FloatTag.valueOf(value);
        }

        @Override
        public Float deserializeNBT(Tag tag, @Nullable Float currentVal) {
            return (tag instanceof FloatTag floatTag) ? floatTag.getAsFloat() : 0f;
        }
    }

    public static class DoubleTransformer implements IValueTransformer<Double> {
        @Override
        public void writeToBuffer(Double value, FriendlyByteBuf buf) {
            buf.writeDouble(value);
        }

        @Override
        public Double readFromBuffer(FriendlyByteBuf buf, Double currentValue) {
            return buf.readDouble();
        }

        @Override
        public Tag serializeNBT(Double value) {
            return DoubleTag.valueOf(value);
        }

        @Override
        public Double deserializeNBT(Tag tag, @Nullable Double currentVal) {
            return (tag instanceof DoubleTag doubleTag) ? doubleTag.getAsDouble() : 0.0;
        }
    }

    public static class ShortTransformer implements IValueTransformer<Short> {
        @Override
        public void writeToBuffer(Short value, FriendlyByteBuf buf) {
            buf.writeShort(value);
        }

        @Override
        public Short readFromBuffer(FriendlyByteBuf buf, Short currentValue) {
            return buf.readShort();
        }

        @Override
        public Tag serializeNBT(Short value) {
            return ShortTag.valueOf(value);
        }

        @Override
        public Short deserializeNBT(Tag tag, @Nullable Short currentVal) {
            return (tag instanceof ShortTag shortTag) ? shortTag.getAsShort() : 0;
        }
    }

    public static class ByteTransformer implements IValueTransformer<Byte> {
        @Override
        public void writeToBuffer(Byte value, FriendlyByteBuf buf) {
            buf.writeByte(value);
        }

        @Override
        public Byte readFromBuffer(FriendlyByteBuf buf, Byte currentValue) {
            return buf.readByte();
        }

        @Override
        public Tag serializeNBT(Byte value) {
            return ByteTag.valueOf(value);
        }

        @Override
        public Byte deserializeNBT(Tag tag, @Nullable Byte currentVal) {
            return (tag instanceof ByteTag byteTag) ? byteTag.getAsByte() : 0;
        }
    }

    public static class CharacterTransformer implements IValueTransformer<Character> {
        @Override
        public void writeToBuffer(Character value, FriendlyByteBuf buf) {
            buf.writeChar(value);
        }

        @Override
        public Character readFromBuffer(FriendlyByteBuf buf, Character currentValue) {
            return buf.readChar();
        }

        @Override
        public Tag serializeNBT(Character value) {
            return IntTag.valueOf((int)value);
        }

        @Override
        public Character deserializeNBT(Tag tag, @Nullable Character currentVal) {
            return (tag instanceof IntTag intTag) ? (char)intTag.getAsInt() : 0;
        }
    }

    public static class BooleanTransformer implements IValueTransformer<Boolean> {
        @Override
        public void writeToBuffer(Boolean value, FriendlyByteBuf buf) {
            buf.writeBoolean(value);
        }

        @Override
        public Boolean readFromBuffer(FriendlyByteBuf buf, Boolean currentValue) {
            return buf.readBoolean();
        }

        @Override
        public Tag serializeNBT(Boolean value) {
            return ByteTag.valueOf(value);
        }

        @Override
        public Boolean deserializeNBT(Tag tag, @Nullable Boolean currentVal) {
            return tag instanceof ByteTag byteTag && byteTag.getAsByte() != 0;
        }
    }
}
