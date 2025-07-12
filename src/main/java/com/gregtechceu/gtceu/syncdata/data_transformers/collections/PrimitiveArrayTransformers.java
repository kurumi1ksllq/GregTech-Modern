package com.gregtechceu.gtceu.syncdata.data_transformers.collections;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;

public class PrimitiveArrayTransformers {

    public static class IntArrayTransformer implements IValueTransformer<int[]> {

        @Override
        public int[] readBufferPayload(FriendlyByteBuf buffer, int[] currentVal) {
            return buffer.readVarIntArray();
        }

        @Override
        public void writeBufferPayload(FriendlyByteBuf buffer, int[] value) {
            buffer.writeVarIntArray(value);
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
        public long[] readBufferPayload(FriendlyByteBuf buffer, long[] currentVal) {
            return buffer.readLongArray();
        }

        @Override
        public void writeBufferPayload(FriendlyByteBuf buffer, long[] value) {
            buffer.writeLongArray(value);
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
        public byte[] readBufferPayload(FriendlyByteBuf buffer, byte[] currentVal) {
            return buffer.readByteArray();
        }

        @Override
        public void writeBufferPayload(FriendlyByteBuf buffer, byte[] value) {
            buffer.writeByteArray(value);
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
