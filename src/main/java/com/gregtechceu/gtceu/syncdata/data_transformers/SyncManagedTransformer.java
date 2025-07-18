package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.syncdata.ClassSyncData;
import com.gregtechceu.gtceu.syncdata.ISyncManaged;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;

public class SyncManagedTransformer<T extends ISyncManaged> implements IValueTransformer<T> {

    @Override
    public boolean mustProvideObject() {
        return true;
    }

    @Override
    public Tag getClientSyncNBT(T value, boolean fullSync) {
        return serialize(value, true, fullSync);
    }

    @Override
    public T loadClientSyncNBT(Tag tag, @Nullable T currentVal) {
        if (currentVal == null || !(tag instanceof CompoundTag compound)) return null;
        return deserialize(compound, currentVal, true);
    }

    @Override
    public Tag serializeNBT(T value) {
        return serialize(value, false, false);
    }

    @Override
    public T deserializeNBT(Tag tag, T currentVal) {
        if (currentVal == null || !(tag instanceof CompoundTag compound)) return null;
        return deserialize(compound, currentVal, false);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private CompoundTag serialize(T value, boolean isSync, boolean fullSync) {
        CompoundTag tag = new CompoundTag();

        ClassSyncData.FieldSyncData[] fieldsToSerialize;

        if (isSync && fullSync) {
            fieldsToSerialize = value.getSyncDataHolder().syncData.clientSyncFields;
        } else if (isSync) {
            fieldsToSerialize = value.getSyncDataHolder().getDirtySyncFields();
        } else {
            fieldsToSerialize = value.getSyncDataHolder().syncData.serverSaveFields;
        }

        for (ClassSyncData.FieldSyncData field : fieldsToSerialize) {
            if (field.isCustomData) {
                var result = field.nbtSaveModifiers[0].invoke(value, new CompoundTag());
                tag.put(field.nbtSaveKey, (Tag) result);
                continue;
            }

            if (field.transformer == null) {
                GTCEu.LOGGER.error("missing value transformer for field {} {}", field.fieldName, field.fieldType);
                return new CompoundTag();
            }
            IValueTransformer<Object> transformer = (IValueTransformer<Object>) field.transformer;
            Object result = field.handle.get(value);
            if (result == null) continue;
            var nbtValue = (isSync) ? transformer.getClientSyncNBT(result, fullSync) : transformer.serializeNBT(result);
            for (MethodHandle modifier : field.nbtSaveModifiers) {
                nbtValue = (Tag) modifier.invoke(value, nbtValue);
            }
            tag.put(field.nbtSaveKey, nbtValue);
        }
        return tag;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private T deserialize(CompoundTag tag, T currentVal, boolean isSync) {

        ClassSyncData.FieldSyncData[] fieldsToCheck = (isSync) ? currentVal.getSyncDataHolder().syncData.clientSyncFields : currentVal.getSyncDataHolder().syncData.serverSaveFields;
        for (ClassSyncData.FieldSyncData field : fieldsToCheck) {
            if (field.isCustomData) {
                field.nbtLoadModifiers[0].invoke(currentVal, tag);
                continue;
            }

            if (field.transformer == null) {
                GTCEu.LOGGER.error("no value transformer for field {} {}", field.fieldName, field.fieldType);
                return currentVal;
            }

            Tag savedValue = tag.get(field.nbtSaveKey);
            if (savedValue == null) continue;
            IValueTransformer<Object> transformer = (IValueTransformer<Object>) field.transformer;

            if (transformer.mustProvideObject()) {
                if (isSync) transformer.loadClientSyncNBT(savedValue, field.handle.get(currentVal));
                else transformer.deserializeNBT(savedValue, field.handle.get(currentVal));
            } else {
                field.handle.set(currentVal, isSync ? transformer.loadClientSyncNBT(savedValue, null) : transformer.deserializeNBT(savedValue, null));
            }
        }
        return null;
    }
}
