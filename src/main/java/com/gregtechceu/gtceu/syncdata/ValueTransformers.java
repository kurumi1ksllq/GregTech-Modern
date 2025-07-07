package com.gregtechceu.gtceu.syncdata;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class ValueTransformers {

    private static final ConcurrentMap<Class<?>, IValueTransformer<?>> DATA_TRANSFORMERS = new ConcurrentHashMap<>();

    public static <T> void register(Class<T> type, IValueTransformer<T> transformer) {
        DATA_TRANSFORMERS.putIfAbsent(type, transformer);
    }

    @SuppressWarnings("unchecked")
    public static <T> IValueTransformer<T> get(Class<T> type) {
        var tx = DATA_TRANSFORMERS.get(type);
        if (tx != null) return (IValueTransformer<T>) tx;

        throw new IllegalArgumentException("No IValueTransformer registered for " + type);
    }

    private static <T> Function<Tag, T> castNumericTag(T defaultV, Function<NumericTag, T> func) {
        return (t) -> (t instanceof NumericTag num) ? func.apply(num) : defaultV;
    }

    public static <A> Function<A, Tag> createNewCompound(BiFunction<A, CompoundTag, CompoundTag> bi) {
        return a -> bi.apply(a, new CompoundTag());
    }

    public static <T> IValueTransformer<T> createSimpleTransformer(BiConsumer<FriendlyByteBuf, T> bufWrite,
                                                                   Function<FriendlyByteBuf, T> bufRead,
                                                                   Function<T, Tag> nbtSave, Function<Tag, T> nbtLoad) {
        return new IValueTransformer<>() {

            @Override
            public void writeBufferPayload(FriendlyByteBuf buffer, T value) {
                bufWrite.accept(buffer, value);
            }

            @Override
            public T readBufferPayload(FriendlyByteBuf buffer) {
                return bufRead.apply(buffer);
            }

            @Override
            public Tag serializeNBT(T value) {
                return nbtSave.apply(value);
            }

            @Override
            public T deserializeNBT(Tag tag) {
                return nbtLoad.apply(tag);
            }
        };
    }

    static {

        // Primitives

        IValueTransformer<Integer> intTransformer = createSimpleTransformer(FriendlyByteBuf::writeInt,
                FriendlyByteBuf::readInt, IntTag::valueOf, castNumericTag(0, NumericTag::getAsInt));
        IValueTransformer<Long> longTransformer = createSimpleTransformer(FriendlyByteBuf::writeLong,
                FriendlyByteBuf::readLong, LongTag::valueOf, castNumericTag(0L, NumericTag::getAsLong));
        IValueTransformer<Float> floatTransformer = createSimpleTransformer(FriendlyByteBuf::writeFloat,
                FriendlyByteBuf::readFloat, FloatTag::valueOf, castNumericTag(0f, NumericTag::getAsFloat));
        IValueTransformer<Double> doubleTransformer = createSimpleTransformer(FriendlyByteBuf::writeDouble,
                FriendlyByteBuf::readDouble, DoubleTag::valueOf, castNumericTag(0.0, NumericTag::getAsDouble));
        IValueTransformer<Short> shortTransformer = createSimpleTransformer((buf, s) -> buf.writeShort(s),
                FriendlyByteBuf::readShort, ShortTag::valueOf, castNumericTag((short) 0, NumericTag::getAsShort));
        IValueTransformer<Byte> byteTransformer = createSimpleTransformer((buf, b) -> buf.writeByte(b),
                FriendlyByteBuf::readByte, ByteTag::valueOf, castNumericTag((byte) 0, NumericTag::getAsByte));
        IValueTransformer<Character> charTransformer = createSimpleTransformer((buf, c) -> buf.writeChar(c),
                FriendlyByteBuf::readChar,
                (c) -> IntTag.valueOf((int) c), (t) -> t instanceof NumericTag num ? (char) num.getAsInt() : 0);
        IValueTransformer<Boolean> boolTransformer = createSimpleTransformer(FriendlyByteBuf::writeBoolean,
                FriendlyByteBuf::readBoolean, ByteTag::valueOf,
                (t) -> t instanceof NumericTag num && num.getAsByte() != 0);

        register(Integer.class, intTransformer);
        register(int.class, intTransformer);
        register(Long.class, longTransformer);
        register(long.class, longTransformer);

        register(Float.class, floatTransformer);
        register(float.class, floatTransformer);

        register(Double.class, doubleTransformer);
        register(double.class, doubleTransformer);

        register(Short.class, shortTransformer);
        register(short.class, shortTransformer);

        register(Byte.class, byteTransformer);
        register(byte.class, byteTransformer);

        register(Character.class, charTransformer);
        register(char.class, charTransformer);

        register(Boolean.class, boolTransformer);
        register(boolean.class, boolTransformer);

        // Objects

        register(ItemStack.class, createSimpleTransformer(FriendlyByteBuf::writeItem, FriendlyByteBuf::readItem,
                ItemStack::serializeNBT, (t) -> t instanceof CompoundTag comp ? ItemStack.of(comp) : ItemStack.EMPTY));

        register(FluidStack.class, createSimpleTransformer((b, f) -> f.writeToPacket(b), FluidStack::readFromPacket,
                createNewCompound(FluidStack::writeToNBT),
                (t) -> t instanceof CompoundTag comp ? FluidStack.loadFluidStackFromNBT(comp) : FluidStack.EMPTY));

        register(UUID.class, createSimpleTransformer(FriendlyByteBuf::writeUUID, FriendlyByteBuf::readUUID,
                NbtUtils::createUUID, NbtUtils::loadUUID));

        register(BlockPos.class, createSimpleTransformer(FriendlyByteBuf::writeBlockPos, FriendlyByteBuf::readBlockPos,
                NbtUtils::writeBlockPos,
                (t) -> t instanceof CompoundTag comp ? NbtUtils.readBlockPos(comp) : BlockPos.ZERO));

        register(CompoundTag.class, createSimpleTransformer(FriendlyByteBuf::writeNbt, FriendlyByteBuf::readNbt,
                (v) -> v, (t) -> t instanceof CompoundTag comp ? comp : new CompoundTag()));
    }
}
