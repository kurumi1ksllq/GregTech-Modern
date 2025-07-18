package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.syncdata.data_transformers.ValueTransformers;

import net.minecraft.nbt.CompoundTag;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SyncDataHolder {

    public final ClassSyncData syncData;
    private final ISyncManaged holder;

    private final List<ClassSyncData.FieldSyncData> dirtySyncFields = new ArrayList<>();

    public SyncDataHolder(@NotNull ISyncManaged o) {
        holder = o;
        syncData = ClassSyncData.CACHE.get(o.getClass());
    }

    public CompoundTag syncNBT(boolean fullClientSync) {
        return (CompoundTag) ValueTransformers.get(ISyncManaged.class).getClientSyncNBT(holder, fullClientSync);
    }

    public void loadSyncNBT(CompoundTag tag) {
        ValueTransformers.get(ISyncManaged.class).loadClientSyncNBT(tag, holder);
    }

    public CompoundTag saveNBT() {
        return (CompoundTag) ValueTransformers.get(ISyncManaged.class).serializeNBT(holder);
    }

    public void loadFromNBT(CompoundTag tag) {
        ValueTransformers.get(ISyncManaged.class).deserializeNBT(tag, holder);
    }

    public ClassSyncData.FieldSyncData[] getDirtySyncFields() {
        var result = dirtySyncFields.toArray(ClassSyncData.FieldSyncData[]::new);
        dirtySyncFields.clear();
        return result;
    }

    public void markSyncFieldDirty(String fieldName) {
        var fieldData = Arrays.stream(syncData.clientSyncFields).filter(f -> Objects.equals(f.fieldName, fieldName)).findFirst();
        fieldData.ifPresent(dirtySyncFields::add);
    }
}
