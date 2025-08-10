package com.gregtechceu.gtceu.syncdata.data_transformers.collections;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;

public class PrimitiveArrayTransformers {

    public static class IntArrayTransformer implements IValueTransformer<int[]> {

        @Override
        public void writeToBuffer(int[] value, FriendlyByteBuf buf) {
            buf.writeVarIntArray(value);
        }

        @Override
        public int[] readFromBuffer(FriendlyByteBuf buf, int[] currentValue) {
            return buf.readVarIntArray();
        }

        @Override
        public Tag serializeNBT(int[] value) {
            return new IntArrayTag(value);
        }

        @Override
        public int[] deserializeNBT(Tag tag, int[] currentVal) {
            if (tag instanceof IntArrayTag arr) return arr.getAsIntArray();
            return new int[0];
        }
    }

    public static class LongArrayTransformer implements IValueTransformer<long[]> {

        @Override
        public void writeToBuffer(long[] value, FriendlyByteBuf buf) {
            buf.writeLongArray(value);
        }

        @Override
        public long[] readFromBuffer(FriendlyByteBuf buf, long[] currentValue) {
            return buf.readLongArray();
        }

        @Override
        public Tag serializeNBT(long[] value) {
            return new LongArrayTag(value);
        }

        @Override
        public long[] deserializeNBT(Tag tag, long[] currentVal) {
            if (tag instanceof LongArrayTag arr) return arr.getAsLongArray();
            return new long[0];
        }
    }

    public static class ByteArrayTransformer implements IValueTransformer<byte[]> {

        @Override
        public void writeToBuffer(byte[] value, FriendlyByteBuf buf) {
            buf.writeByteArray(value);
        }

        @Override
        public byte[] readFromBuffer(FriendlyByteBuf buf, byte[] currentValue) {
            return buf.readByteArray();
        }

        @Override
        public Tag serializeNBT(byte[] value) {
            return new ByteArrayTag(value);
        }

        @Override
        public byte[] deserializeNBT(Tag tag, byte[] currentVal) {
            if (tag instanceof ByteArrayTag arr) return arr.getAsByteArray();
            return new byte[0];
        }
    }
}
