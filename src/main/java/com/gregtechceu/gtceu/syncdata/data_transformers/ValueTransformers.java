package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.GTCEu;
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
            void.class, Void.class);

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
                if (componentTx != null) return new ObjectArrayTransformer<>(componentTx);
            }

            for (var ifaceEntry : REGISTERED_INTERFACES.entrySet()) {
                if (ifaceEntry.getKey().isAssignableFrom(type)) return ifaceEntry.getValue();
            }

            throw new IllegalStateException("No transformer registered for type: " + type);
        }
    };

    public static IValueTransformer<?> getCollectionTransformer(Field type) {
        Class<?> collectionType = type.getType();
        if (!Collection.class.isAssignableFrom(collectionType)) return null;
        if (type.getGenericType() instanceof ParameterizedType ptype) {
            Type[] actualTypes = ptype.getActualTypeArguments();
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

    static {

        registerClassTransformer(Integer.class, new PrimitiveTransformers.IntTransformer());
        registerClassTransformer(Long.class, new PrimitiveTransformers.LongTransformer());
        registerClassTransformer(Float.class, new PrimitiveTransformers.FloatTransformer());
        registerClassTransformer(Double.class, new PrimitiveTransformers.DoubleTransformer());
        registerClassTransformer(Short.class, new PrimitiveTransformers.ShortTransformer());
        registerClassTransformer(Byte.class, new PrimitiveTransformers.ByteTransformer());
        registerClassTransformer(Character.class, new PrimitiveTransformers.CharacterTransformer());
        registerClassTransformer(Boolean.class, new PrimitiveTransformers.BooleanTransformer());

        // Primtive arrays
        registerClassTransformer(int[].class, new PrimitiveArrayTransformers.IntArrayTransformer());
        registerClassTransformer(long[].class, new PrimitiveArrayTransformers.LongArrayTransformer());
        registerClassTransformer(byte[].class, new PrimitiveArrayTransformers.ByteArrayTransformer());

        // Classes

        registerClassTransformer(String.class, new CommonClassTransformers.StringTransformer());
        registerClassTransformer(ItemStack.class, new CommonClassTransformers.ItemStackTransformer());
        registerClassTransformer(FluidStack.class, new CommonClassTransformers.FluidStackTransformer());
        registerClassTransformer(UUID.class, new CommonClassTransformers.UUIDTransformer());
        registerClassTransformer(BlockPos.class, new CommonClassTransformers.BlockPosTransformer());
        registerClassTransformer(CompoundTag.class, new CommonClassTransformers.CompoundTagTransformer());

        registerClassTransformer(GTRecipe.class, new GTRecipeTransformer());
        registerClassTransformer(GTRecipeType.class, new GTRecipeTypeTransformer());
        registerClassTransformer(MachineRenderState.class, new MachineRenderStateTransformer());
        registerClassTransformer(Material.class, new MaterialTransformer());

        // Interfaces

        registerInterfaceTransformer(INBTSerializable.class, new NBTSerialisableTransformer());
        registerInterfaceTransformer(Component.class, new CommonClassTransformers.ComponentTransformer());

    }
}
