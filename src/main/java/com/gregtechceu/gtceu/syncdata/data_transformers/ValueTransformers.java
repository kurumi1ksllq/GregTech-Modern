package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
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
import java.lang.reflect.Field;
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
    private static final Map<Class<?>, IValueTransformer<?>> REGISTERED_INTERFACES = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_BOXED = Map.of(
            boolean.class, Boolean.class,
            byte.class, Byte.class,
            char.class, Character.class,
            short.class, Short.class,
            int.class, Integer.class,
            long.class, Long.class,
            float.class, Float.class,
            double.class, Double.class,
            void.class, Void.class
    );

    public static Class<?> boxIfPrimitive(Class<?> cls) {
        return cls.isPrimitive() ? PRIMITIVE_TO_BOXED.get(cls) : cls;
    }

    // Logic for determining which IValueTransformer should be used to serialise a value
    private static final ClassValue<IValueTransformer<?>> TRANSFORMERS = new ClassValue<>() {

        @Override
        protected IValueTransformer<?> computeValue(@NotNull Class<?> type) {
            type = boxIfPrimitive(type);
            IValueTransformer<?> tx = REGISTERED.get(type);
            if (tx != null) return tx;
            IValueTransformer<?> ifaceTx = REGISTERED_INTERFACES.get(type);
            if (ifaceTx != null) return ifaceTx;

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

            for (var ifaceEntry : REGISTERED_INTERFACES.entrySet()) {
                if (ifaceEntry.getKey().isAssignableFrom(type)) return ifaceEntry.getValue();
            }

            throw new IllegalStateException("No transformer registered for type: " + type);
        }
    };


    public static IValueTransformer<?> getCollectionTransformer(Field type) {
        var collectionType = type.getType();

        if (type.getGenericType() instanceof ParameterizedType ptype) {
            var actualTypes = ptype.getActualTypeArguments();
            Type keyType = actualTypes[0];
            Type valueType = actualTypes.length > 1 ? actualTypes[1] : null;

            if (List.class.isAssignableFrom(collectionType)) {
                if (keyType instanceof Class<?> keyClass) {
                    return new ListTransformer<>(ValueTransformers.get(keyClass));
                }
            } else if (Set.class.isAssignableFrom(collectionType)) {
                if (keyType instanceof Class<?> keyClass) {
                    return new SetTransformer<>(ValueTransformers.get(keyClass));
                }
            } else if (Map.class.isAssignableFrom(collectionType)) {
                if (keyType instanceof Class<?> keyClass && valueType instanceof Class<?> valueClass) {
                    return new MapTransformer<>(ValueTransformers.get(keyClass), ValueTransformers.get(valueClass));
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> IValueTransformer<T> get(Class<T> type) {
        return (IValueTransformer<T>) TRANSFORMERS.get(boxIfPrimitive(type));
    }

    public static void registerClassTransformer(Class<?> type, IValueTransformer<?> transformer) {
        REGISTERED.putIfAbsent(type, transformer);
    }

    public static void registerInterfaceTransformer(Class<?> type, IValueTransformer<?> transformer) {
        REGISTERED_INTERFACES.put(type, transformer);
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

    static {

        registerClassTransformer(Integer.class, createSimpleTransformer(FriendlyByteBuf::writeInt,
                FriendlyByteBuf::readInt, IntTag::valueOf, castTag(0, NumericTag.class, NumericTag::getAsInt)));

        registerClassTransformer(Long.class, createSimpleTransformer(FriendlyByteBuf::writeLong,
                FriendlyByteBuf::readLong, LongTag::valueOf, castTag(0L, NumericTag.class, NumericTag::getAsLong)));

        registerClassTransformer(Float.class, createSimpleTransformer(FriendlyByteBuf::writeFloat,
                FriendlyByteBuf::readFloat, FloatTag::valueOf, castTag(0f, NumericTag.class, NumericTag::getAsFloat)));

        registerClassTransformer(Double.class, createSimpleTransformer(FriendlyByteBuf::writeDouble,
                FriendlyByteBuf::readDouble, DoubleTag::valueOf,
                castTag(0.0, NumericTag.class, NumericTag::getAsDouble)));

        registerClassTransformer(Short.class, createSimpleTransformer((buf, s) -> buf.writeShort(s),
                FriendlyByteBuf::readShort, ShortTag::valueOf,
                castTag((short) 0, NumericTag.class, NumericTag::getAsShort)));

        registerClassTransformer(Byte.class, createSimpleTransformer((buf, b) -> buf.writeByte(b),
                FriendlyByteBuf::readByte, ByteTag::valueOf,
                castTag((byte) 0, NumericTag.class, NumericTag::getAsByte)));

        registerClassTransformer(Character.class, createSimpleTransformer((buf, c) -> buf.writeChar(c),
                FriendlyByteBuf::readChar,
                (c) -> IntTag.valueOf((int) c), castTag((char) 0, NumericTag.class, (tag) -> (char) tag.getAsInt())));
        registerClassTransformer(Boolean.class, createSimpleTransformer(FriendlyByteBuf::writeBoolean,
                FriendlyByteBuf::readBoolean, ByteTag::valueOf,
                (t) -> t instanceof NumericTag num && num.getAsByte() != 0));

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

        registerClassTransformer(GTRecipe.class, new GTRecipeTransformer());
        registerClassTransformer(GTRecipeType.class, new GTRecipeTypeTransformer());
        registerClassTransformer(MachineRenderState.class, new MachineRenderStateTransformer());
        registerClassTransformer(Material.class, new MaterialTransformer());

        // Interfaces

        registerInterfaceTransformer(ISyncManaged.class, new SyncManagedTransformer<>());
        registerInterfaceTransformer(INBTSerializable.class, new NBTSerialisableTransformer());

        registerInterfaceTransformer(Component.class,
                createSimpleTransformer(FriendlyByteBuf::writeComponent, FriendlyByteBuf::readComponent,
                        (c) -> StringTag.valueOf(Component.Serializer.toJson(c)),
                        castTag(Component.literal(""), StringTag.class,
                                (s) -> Component.Serializer.fromJson(s.getAsString()))));

    }
}
