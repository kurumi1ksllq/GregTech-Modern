package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.syncdata.annotations.*;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Modifier;
import java.util.*;

/// Static data for synced classes

public final class ClassSyncData {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    public static final ClassValue<ClassSyncData> CACHE = new ClassValue<>() {

        @Override
        protected ClassSyncData computeValue(@NotNull Class<?> type) {
            return new ClassSyncData(type);
        }
    };

    public final FieldSyncData[] clientSyncFields;
    public final FieldSyncData[] serverSaveFields;
    public final FieldListener[] listeners;

    private ClassSyncData(@NotNull Class<?> clazz) {
        MethodHandles.Lookup privateLookup;
        try {
            privateLookup = MethodHandles.privateLookupIn(clazz, LOOKUP);
        } catch (IllegalAccessException ignored) {
            clientSyncFields = new FieldSyncData[0];
            serverSaveFields = new FieldSyncData[0];
            listeners = new FieldListener[0];
            return;
        }

        ArrayList<FieldSyncData> foundSyncFields = new ArrayList<>();
        ArrayList<FieldSyncData> foundSaveFields = new ArrayList<>();
        ArrayList<FieldListener> foundListeners = new ArrayList<>();

        for (var field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            /// Currently using ldlib annotations
            SaveField saveF = field.getAnnotation(SaveField.class);
            boolean hasClientSync = field.isAnnotationPresent(SyncToClient.class);
            boolean hasTriggerClientRerender = field.isAnnotationPresent(RerenderOnChanged.class);
            boolean hasSaveToStack = field.isAnnotationPresent(SaveToItemStack.class);
            if (saveF != null || hasClientSync || hasTriggerClientRerender || hasSaveToStack) {
                VarHandle handle;
                try {
                    handle = privateLookup.unreflectVarHandle(field);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
                if (hasClientSync) foundSyncFields.add(new FieldSyncData(field.getName(), handle,
                        (saveF != null && !saveF.nbtKey().isBlank()) ? saveF.nbtKey() : "", hasTriggerClientRerender,
                        hasSaveToStack));
                if (saveF != null) foundSaveFields.add(new FieldSyncData(field.getName(), handle,
                        (!saveF.nbtKey().isBlank()) ? saveF.nbtKey() : "", hasTriggerClientRerender, hasSaveToStack));
            }
        }
        for (var method : clazz.getDeclaredMethods()) {
            ClientFieldChangeListener ann = method.getAnnotation(ClientFieldChangeListener.class);
            if (ann == null) continue;
            if (Modifier.isStatic(method.getModifiers()))
                throw new IllegalStateException(
                        "@ClientFieldChangeListener may not be applied to static method " + method.getName());
            try {
                MethodHandle mh = privateLookup.unreflect(method);
                foundListeners.add(new FieldListener(mh, ann.fieldName()));
            } catch (IllegalAccessException ignored) {

            }
        }

        Class<?> parent = clazz.getSuperclass();
        if (parent != null) {
            var parentHandles = CACHE.get(parent);
            foundSyncFields.addAll(List.of(parentHandles.clientSyncFields));
            foundSaveFields.addAll(List.of(parentHandles.serverSaveFields));
            foundListeners.addAll(List.of(parentHandles.listeners));
        }

        serverSaveFields = foundSaveFields.toArray(FieldSyncData[]::new);
        clientSyncFields = foundSyncFields.toArray(FieldSyncData[]::new);
        listeners = foundListeners.toArray(FieldListener[]::new);
    }

    public record FieldSyncData(String fieldName, VarHandle handle, String nbtSaveKey, boolean triggerClientRerender,
                                boolean saveToStack) {}

    public static final class FieldListener {

        private final MethodHandle handle;
        private final String fieldName;

        FieldListener(MethodHandle handle, String fieldName) {
            this.handle = handle;
            this.fieldName = fieldName;
        }

        public MethodHandle handle() {
            return handle;
        }

        public String field() {
            return fieldName;
        }
    }
}
