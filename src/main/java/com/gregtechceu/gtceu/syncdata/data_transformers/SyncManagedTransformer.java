package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.syncdata.ClassSyncData;
import com.gregtechceu.gtceu.syncdata.ISyncManaged;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;

public class SyncManagedTransformer<T extends ISyncManaged> implements IValueTransformer<T> {

    @Override
    public boolean mustProvideObject() {
        return true;
    }

    @Override
    public void writeBufferPayload(FriendlyByteBuf buffer, T value) {}

    @Override
    public T readBufferPayload(FriendlyByteBuf buffer, T currentVal) {
        return null;
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public Tag serializeNBT(T value) {
        CompoundTag tag = new CompoundTag();
        for (ClassSyncData.FieldSyncData field : value.getSyncDataHolder().syncData.serverSaveFields) {
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
            if (result == null) {
                tag.put(field.nbtSaveKey, new CompoundTag());
                continue;
            }
            var nbtValue = transformer.serializeNBT(result);
            for (MethodHandle modifier : field.nbtSaveModifiers) {
                nbtValue = (Tag) modifier.invoke(value, nbtValue);
            }
            tag.put(field.nbtSaveKey, nbtValue);
        }
        return tag;
    }

    @SneakyThrows
    @Override
    @SuppressWarnings("unchecked")
    public T deserializeNBT(Tag tag, T currentVal) {
        if (!(tag instanceof CompoundTag compound) || currentVal == null) return currentVal;
        for (ClassSyncData.FieldSyncData field : currentVal.getSyncDataHolder().syncData.serverSaveFields) {
            if (field.isCustomData) {
                field.nbtLoadModifiers[0].invoke(currentVal, compound);
                continue;
            }

            if (field.transformer == null) {
                GTCEu.LOGGER.error("no value transformer for field {} {}", field.fieldName, field.fieldType);
                return currentVal;
            }
            Tag savedValue = compound.get(field.nbtSaveKey);
            if (savedValue == null) continue;
            IValueTransformer<Object> transformer = (IValueTransformer<Object>) field.transformer;

            if (transformer.mustProvideObject()) {
                transformer.deserializeNBT(savedValue, field.handle.get(currentVal));
            } else {
                field.handle.set(currentVal, transformer.deserializeNBT(savedValue, null));
            }
        }
        return null;
    }
}
