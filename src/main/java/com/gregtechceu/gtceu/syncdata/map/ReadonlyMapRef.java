package com.gregtechceu.gtceu.syncdata.map;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import com.lowdragmc.lowdraglib.syncdata.managed.ReadonlyRef;

import java.util.Map;

public class ReadonlyMapRef extends ReadonlyRef {

    public ReadonlyMapRef(boolean isLazy, Object value) {
        super(isLazy, value);
    }

    @Override
    protected void init() {
        var obj = readRaw();
        if (obj instanceof IContentChangeAware || obj instanceof IManaged) {
            super.init();
            return;
        }
        if (isLazy()) {
            return;
        }

        if (!(obj instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Field must be a Map");
        }
        for (var entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            boolean oneIsManaged = false;

            if (key instanceof IContentChangeAware handler) {
                replaceHandler(handler);
                oneIsManaged = true;
            } else if (key instanceof IManaged) {
                oneIsManaged = true;
            }

            if (value instanceof IContentChangeAware handler) {
                replaceHandler(handler);
                oneIsManaged = true;
            } else if (value instanceof IManaged) {
                oneIsManaged = true;
            }

            if (!oneIsManaged) {
                throw new IllegalArgumentException("complex sync field must be an IContentChangeAware if not lazy!");
            }
        }
    }

    @Override
    public void update() {
        super.update();
        var obj = readRaw();
        if (!(obj instanceof Map<?, ?> map)) {
            return;
        }

        for (var entry : map.entrySet()) {
            if (entry.getKey() instanceof IManaged managed) {
                for (IRef field : managed.getSyncStorage().getNonLazyFields()) {
                    field.update();
                }
                if (managed.getSyncStorage().hasDirtySyncFields() ||
                        managed.getSyncStorage().hasDirtyPersistedFields()) {
                    markAsDirty();
                }
            }
            if (entry.getValue() instanceof IManaged managed) {
                for (IRef field : managed.getSyncStorage().getNonLazyFields()) {
                    field.update();
                }
                if (managed.getSyncStorage().hasDirtySyncFields() ||
                        managed.getSyncStorage().hasDirtyPersistedFields()) {
                    markAsDirty();
                }
            }
        }
    }
}
