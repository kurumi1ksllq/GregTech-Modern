package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.syncdata.ISyncManaged;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;
import com.gregtechceu.gtceu.syncdata.data_transformers.collections.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class ValueTransformers {

    private static final Map<Class<?>, IValueTransformer<?>> REGISTERED = new ConcurrentHashMap<>();

    // Logic for determining which IValueTransformer should be used to serialise a value
    private static final ClassValue<IValueTransformer<?>> TRANSFORMERS = new ClassValue<>() {

        @Override
        protected IValueTransformer<?> computeValue(@NotNull Class<?> type) {
            IValueTransformer<?> tx = REGISTERED.get(type);
            if (tx != null) return tx;

            if (type.isEnum()) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) type;
                return new EnumTransformer<>(enumClass);
            }

            if (type.isArray()) {
                Class<?> componentType = type.getComponentType();
                IValueTransformer<?> componentTx = get(componentType);
                if (componentTx != null) return new ObjectArrayTransformer<>(componentTx,
                        length -> (Object[]) Array.newInstance(componentType, length));
            }

            if (List.class.isAssignableFrom(type)) {
                IValueTransformer<?> elementTx = findTypeArgumentTransformer(type, 0);
                if (elementTx != null) return new ListTransformer<>(elementTx);
            } else if (Set.class.isAssignableFrom(type)) {
                IValueTransformer<?> elementTx = findTypeArgumentTransformer(type, 0);
                if (elementTx != null) return new SetTransformer<>(elementTx);
            } else if (Map.class.isAssignableFrom(type)) {
                IValueTransformer<?> keyTx = findTypeArgumentTransformer(type, 0);
                IValueTransformer<?> valTx = findTypeArgumentTransformer(type, 1);
                if (keyTx != null && valTx != null) return new MapTransformer<>(keyTx, valTx);
            }

            for (Class<?> iface : type.getInterfaces()) {
                IValueTransformer<?> ifaceTx = REGISTERED.get(iface);
                if (ifaceTx != null) return ifaceTx;
            }

            throw new IllegalStateException("No transformer registered for type: " + type);
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> IValueTransformer<T> get(Class<T> type) {
        return (IValueTransformer<T>) TRANSFORMERS.get(type);
    }

    public static void registerClassTransformer(Class<?> type, IValueTransformer<?> transformer) {
        REGISTERED.putIfAbsent(type, transformer);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Tag, V> Function<Tag, V> castTag(V defaultV, Class<T> cls, Function<T, V> func) {
        return (tag) -> cls.isInstance(tag) ? func.apply((T) tag) : defaultV;
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
            public T readBufferPayload(FriendlyByteBuf buffer, @Nullable T current) {
                return bufRead.apply(buffer);
            }

            @Override
            public Tag serializeNBT(T value) {
                return nbtSave.apply(value);
            }

            @Override
            public T deserializeNBT(Tag tag, @Nullable T current) {
                return nbtLoad.apply(tag);
            }
        };
    }

    private static IValueTransformer<?> findTypeArgumentTransformer(Class<?> clazz, int index) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length > index && args[index] instanceof Class<?> actualClass) {
                return TRANSFORMERS.get(actualClass);
            }
        }
        return null;
    }

    static {

        // Primitives

        IValueTransformer<Integer> intTransformer = createSimpleTransformer(FriendlyByteBuf::writeInt,
                FriendlyByteBuf::readInt, IntTag::valueOf, castTag(0, NumericTag.class, NumericTag::getAsInt));
        IValueTransformer<Long> longTransformer = createSimpleTransformer(FriendlyByteBuf::writeLong,
                FriendlyByteBuf::readLong, LongTag::valueOf, castTag(0L, NumericTag.class, NumericTag::getAsLong));
        IValueTransformer<Float> floatTransformer = createSimpleTransformer(FriendlyByteBuf::writeFloat,
                FriendlyByteBuf::readFloat, FloatTag::valueOf, castTag(0f, NumericTag.class, NumericTag::getAsFloat));
        IValueTransformer<Double> doubleTransformer = createSimpleTransformer(FriendlyByteBuf::writeDouble,
                FriendlyByteBuf::readDouble, DoubleTag::valueOf,
                castTag(0.0, NumericTag.class, NumericTag::getAsDouble));
        IValueTransformer<Short> shortTransformer = createSimpleTransformer((buf, s) -> buf.writeShort(s),
                FriendlyByteBuf::readShort, ShortTag::valueOf,
                castTag((short) 0, NumericTag.class, NumericTag::getAsShort));
        IValueTransformer<Byte> byteTransformer = createSimpleTransformer((buf, b) -> buf.writeByte(b),
                FriendlyByteBuf::readByte, ByteTag::valueOf,
                castTag((byte) 0, NumericTag.class, NumericTag::getAsByte));
        IValueTransformer<Character> charTransformer = createSimpleTransformer((buf, c) -> buf.writeChar(c),
                FriendlyByteBuf::readChar,
                (c) -> IntTag.valueOf((int) c), castTag((char) 0, NumericTag.class, (tag) -> (char) tag.getAsInt()));
        IValueTransformer<Boolean> boolTransformer = createSimpleTransformer(FriendlyByteBuf::writeBoolean,
                FriendlyByteBuf::readBoolean, ByteTag::valueOf,
                (t) -> t instanceof NumericTag num && num.getAsByte() != 0);

        registerClassTransformer(Integer.class, intTransformer);
        registerClassTransformer(int.class, intTransformer);

        registerClassTransformer(Long.class, longTransformer);
        registerClassTransformer(long.class, longTransformer);

        registerClassTransformer(Float.class, floatTransformer);
        registerClassTransformer(float.class, floatTransformer);

        registerClassTransformer(Double.class, doubleTransformer);
        registerClassTransformer(double.class, doubleTransformer);

        registerClassTransformer(Short.class, shortTransformer);
        registerClassTransformer(short.class, shortTransformer);

        registerClassTransformer(Byte.class, byteTransformer);
        registerClassTransformer(byte.class, byteTransformer);

        registerClassTransformer(Character.class, charTransformer);
        registerClassTransformer(char.class, charTransformer);

        registerClassTransformer(Boolean.class, boolTransformer);
        registerClassTransformer(boolean.class, boolTransformer);

        // Primtive arrays
        registerClassTransformer(int[].class, new PrimitiveArrayTransformers.IntArrayTransformer());
        registerClassTransformer(long[].class, new PrimitiveArrayTransformers.LongArrayTransformer());
        registerClassTransformer(byte[].class, new PrimitiveArrayTransformers.ByteArrayTransformer());

        // Classes

        registerClassTransformer(String.class, createSimpleTransformer(FriendlyByteBuf::writeUtf,
                FriendlyByteBuf::readUtf, StringTag::valueOf, castTag("", StringTag.class, StringTag::getAsString)));

        registerClassTransformer(ItemStack.class,
                createSimpleTransformer(FriendlyByteBuf::writeItem, FriendlyByteBuf::readItem, ItemStack::serializeNBT,
                        castTag(ItemStack.EMPTY, CompoundTag.class, ItemStack::of)));

        registerClassTransformer(FluidStack.class,
                createSimpleTransformer((b, f) -> f.writeToPacket(b), FluidStack::readFromPacket,
                        (f) -> f.writeToNBT(new CompoundTag()),
                        castTag(FluidStack.EMPTY, CompoundTag.class, FluidStack::loadFluidStackFromNBT)));

        registerClassTransformer(UUID.class, createSimpleTransformer(FriendlyByteBuf::writeUUID,
                FriendlyByteBuf::readUUID, NbtUtils::createUUID, NbtUtils::loadUUID));

        registerClassTransformer(BlockPos.class,
                createSimpleTransformer(FriendlyByteBuf::writeBlockPos, FriendlyByteBuf::readBlockPos,
                        NbtUtils::writeBlockPos, castTag(BlockPos.ZERO, CompoundTag.class, NbtUtils::readBlockPos)));

        registerClassTransformer(CompoundTag.class, createSimpleTransformer(FriendlyByteBuf::writeNbt,
                FriendlyByteBuf::readNbt, (v) -> v, castTag(new CompoundTag(), CompoundTag.class, (v) -> v)));

        registerClassTransformer(Component.class,
                createSimpleTransformer(FriendlyByteBuf::writeComponent, FriendlyByteBuf::readComponent,
                        (c) -> StringTag.valueOf(Component.Serializer.toJson(c)),
                        castTag(Component.literal(""), StringTag.class,
                                (s) -> Component.Serializer.fromJson(s.getAsString()))));

        registerClassTransformer(GTRecipe.class, new GTRecipeTransformer());
        registerClassTransformer(GTRecipeType.class, new GTRecipeTypeTransformer());
        registerClassTransformer(MachineRenderStateTransformer.class, new MachineRenderStateTransformer());
        registerClassTransformer(Material.class, new MaterialTransformer());

        // Interfaces

        registerClassTransformer(INBTSerializable.class, new NBTSerialisableTransformer());
        registerClassTransformer(ISyncManaged.class, new SyncManagedTransformer<>());
    }
}
