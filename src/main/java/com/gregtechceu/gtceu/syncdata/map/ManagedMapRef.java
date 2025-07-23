package com.gregtechceu.gtceu.syncdata.map;

import com.lowdragmc.lowdraglib.syncdata.SyncUtils;
import com.lowdragmc.lowdraglib.syncdata.managed.IManagedVar;
import com.lowdragmc.lowdraglib.syncdata.managed.ManagedRef;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ManagedMapRef extends ManagedRef {

    protected Object oldValue;

    public ManagedMapRef(IManagedVar<?> field, boolean lazy) {
        super(field);
        this.lazy = lazy;
        if (!Map.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("Field %s is not a Map".formatted(field));
        }
        Object value = getField().value();
        if (value != null) {
            this.oldValue = copyMap(value);
        }
    }

    @Override
    public void update() {
        Object newValue = getField().value();
        if ((oldValue == null && newValue != null) || (oldValue != null && newValue == null) ||
                (oldValue != null && checkMapChanges(oldValue, newValue))) {
            if (newValue != null) {
                this.oldValue = copyMap(newValue);
            } else {
                this.oldValue = null;
            }
            this.markAsDirty();
        }
    }

    protected boolean checkMapChanges(@NotNull Object oldObj, @NotNull Object newObj) {
        if (!(oldObj instanceof Map<?, ?> oldMap) || !(newObj instanceof Map<?, ?> newMap)) {
            throw new IllegalArgumentException("Old or new value %s is not a Map".formatted(newObj));
        }
        if (oldMap.size() != newMap.size()) {
            return true;
        }

        for (var newEntry : newMap.entrySet()) {
            Object key = newEntry.getKey();
            Object newValue = newEntry.getValue();

            if (!oldMap.containsKey(key) || !oldMap.containsValue(newValue)) {
                return true;
            }
            Object oldValue = oldMap.get(key);
            if (oldValue != null && SyncUtils.isChanged(oldValue, newValue)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public static Object copyMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            Map<Object, Object> result = new HashMap<>();

            for (var entry : map.entrySet()) {
                Object key = SyncUtils.copyWhenNecessary(entry.getKey());
                Object value = SyncUtils.copyWhenNecessary(entry.getValue());
                result.put(key, value);
            }
            return result;
        }

        throw new IllegalArgumentException("Value %s is not a Map".formatted(obj));
    }
}
