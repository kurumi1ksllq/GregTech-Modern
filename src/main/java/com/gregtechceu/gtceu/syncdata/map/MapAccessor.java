package com.gregtechceu.gtceu.syncdata.map;

import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.SyncUtils;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.managed.*;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.PrimitiveTypedPayload;

import net.minecraft.nbt.Tag;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapAccessor implements IAccessor {

    @Getter
    @Setter
    private byte defaultType = -1;

    @Getter
    private final IAccessor keyAccessor;
    @Getter
    private final IAccessor valueAccessor;

    protected final Class<?> keyType;
    protected final Class<?> valueType;

    protected MapAccessor(IAccessor keyAccessor, Class<?> keyType, IAccessor valueAccessor, Class<?> valueType) {
        this.keyAccessor = keyAccessor;
        this.valueAccessor = valueAccessor;

        this.keyType = keyType;
        this.valueType = valueType;

        if (this.keyAccessor == null) {
            throw new RuntimeException("Cannot find accessor for key type");
        }
        if (this.valueAccessor == null) {
            throw new RuntimeException("Cannot find accessor for value type");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Object copyForManaged(Object value) {
        if (value instanceof Tag tag) {
            return tag.copy();
        }
        return SyncUtils.copyWhenNecessary(value);
    }

    @Override
    public byte getDefaultType() {
        return TypedPayloadRegistries.getId(MapPayload.class);
    }

    @Override
    public ITypedPayload<?> readField(AccessorOp op, IRef field) {
        if (field instanceof ManagedRef managedRef) {
            var managedField = managedRef.getField();
            if (!managedField.isPrimitive() && managedField.value() == null) {
                return PrimitiveTypedPayload.ofNull();
            }
            return readManagedField(op, managedField);
        }

        var obj = field.readRaw();
        if (obj == null) {
            throw new IllegalArgumentException("readonly field %s has a null reference".formatted(field));
        }

        return readFromReadonlyField(op, obj);
    }

    @Override
    public void writeField(AccessorOp op, IRef field, ITypedPayload<?> payload) {}

    @Override
    public boolean hasPredicate() {
        return true;
    }

    @Override
    public boolean test(Class<?> type) {
        return Map.class.isAssignableFrom(type);
    }

    @Override
    public boolean isManaged() {
        return keyAccessor.isManaged() || valueAccessor.isManaged();
    }

    @Override
    public ITypedPayload<?> readManagedField(AccessorOp op, IManagedVar<?> field) {
        var obj = field.value();
        if (obj == null) {
            return PrimitiveTypedPayload.ofNull();
        }

        if (!(obj instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Field %s is not a Map".formatted(obj));
        }

        int size = map.size();
        var iter = map.entrySet().iterator();
        @SuppressWarnings("unchecked")
        Map.Entry<ITypedPayload<?>, ITypedPayload<?>>[] result = new Map.Entry[size];

        for (int i = 0; i < size; i++) {
            var entry = iter.next();

            ITypedPayload<?> keyPayload;
            if (keyAccessor.isManaged()) {
                ManagedHolder<?> keyHolder = ManagedHolder.of(entry.getKey());
                keyPayload = keyAccessor.readManagedField(op, keyHolder);
            } else {
                keyPayload = keyAccessor.readFromReadonlyField(op, entry.getKey());
            }

            ITypedPayload<?> valuePayload;
            if (valueAccessor.isManaged()) {
                ManagedHolder<?> valueHolder = ManagedHolder.of(entry.getValue());
                valuePayload = valueAccessor.readManagedField(op, valueHolder);
            } else {
                valuePayload = valueAccessor.readFromReadonlyField(op, entry.getValue());
            }

            result[i] = Map.entry(keyPayload, valuePayload);
        }

        return MapPayload.of(result);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void writeManagedField(AccessorOp op, IManagedVar<?> field, ITypedPayload<?> payload) {
        if (payload instanceof PrimitiveTypedPayload<?> primitive && primitive.isNull()) {
            field.set(null);
            return;
        }
        if (!(payload instanceof MapPayload mapPayload)) {
            throw new IllegalArgumentException("Payload %s is not a MapPayload".formatted(payload));
        }

        var obj = field.value();
        if (obj == null) {
            try {
                Constructor<?> ctor = field.getType().getConstructor();
                ctor.setAccessible(true);
                obj = ctor.newInstance();
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchMethodException ignored) {
                obj = new HashMap<>();
            }
        }
        if (!(obj instanceof Map map)) {
            throw new IllegalArgumentException("Field %s is not a Map".formatted(obj));
        }

        var payloads = mapPayload.getPayload();
        for (var entry : payloads) {
            ITypedPayload<?> keyPayload = entry.getKey();
            Object key = keyPayload.getPayload();
            if (keyAccessor.isManaged()) {
                ManagedHolder<?> holder = ManagedHolder.ofType(keyType);
                keyAccessor.writeManagedField(op, holder, keyPayload);
                key = holder.value();
            } else {
                keyAccessor.writeToReadonlyField(op, key, keyPayload);
            }

            ITypedPayload<?> valuePayload = entry.getValue();
            Object value = valuePayload.getPayload();
            if (valueAccessor.isManaged()) {
                ManagedHolder<?> holder = ManagedHolder.ofType(valueType);
                valueAccessor.writeManagedField(op, holder, valuePayload);
                value = holder.value();
            } else {
                valueAccessor.writeToReadonlyField(op, value, valuePayload);
            }
            map.put(key, value);
        }
    }

    @Override
    public ITypedPayload<?> readFromReadonlyField(AccessorOp op, Object obj) {
        var holder = ManagedHolder.of(obj);
        return readManagedField(op, holder);
    }

    @Override
    public void writeToReadonlyField(AccessorOp op, Object obj, ITypedPayload<?> payload) {
        var holder = ManagedHolder.of(obj);
        writeManagedField(op, holder, payload);
    }

    public static IAccessor makeAccessor(IAccessor keyAccessor, Class<?> keyType,
                                         IAccessor valueAccessor, Class<?> valueType) {
        return ACCESSOR_CACHE.computeIfAbsent(new CacheKey(keyAccessor, keyType, valueAccessor, valueType),
                CacheKey::makeAccessor);
    }

    private static final Map<CacheKey, IAccessor> ACCESSOR_CACHE = new ConcurrentHashMap<>();

    private record CacheKey(IAccessor keyAccessor, Class<?> keyType, IAccessor valAccessor, Class<?> valType) {

        private IAccessor makeAccessor() {
            return new MapAccessor(this.keyAccessor, this.keyType, this.valAccessor, this.valType);
        }
    }
}
