package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.*;

/**
 * Class that holds all sync info for an {@link ISyncManaged} object.
 */
public class SyncDataHolder {

    private final ClassSyncData syncData;
    private final ISyncManaged holder;

    private final Set<String> dirtySyncFields = new HashSet<>();

    public SyncDataHolder(@NotNull ISyncManaged o) {
        holder = o;
        syncData = ClassSyncData.CACHE.get(o.getClass());
    }

    /**
     * Instructs the sync system that this field has been updated and must be synced with clients.
     * 
     * @param fieldName The field that has changed.
     */
    public void markClientSyncFieldDirty(String fieldName) {
        dirtySyncFields.add(fieldName);
        holder.markAsChanged();
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public void writeToNetworkBuffer(FriendlyByteBuf buf) {
        for (String fieldName : dirtySyncFields) {
            var field = syncData.clientSyncFields.get(fieldName);
            if (field == null) continue;
            if (field.isCustomData) {
                field.bufWriteModifier[0].invoke(holder, buf);
            }

            if (field.isComplex) {
                ISyncManaged currentValue = (ISyncManaged) field.handle.get(holder);
                if (currentValue != null) {
                    buf.writeUtf(fieldName);
                    currentValue.getSyncDataHolder().writeToNetworkBuffer(buf);
                };
            } else {
                if (field.transformer == null) throw new IllegalStateException(
                        "Missing value transformer for field %s".formatted(field.fieldName));
                IValueTransformer<Object> transformer = (IValueTransformer<Object>) field.transformer;
                Object result = field.handle.get(holder);
                buf.writeUtf(fieldName);
                transformer.writeToBuffer(result, buf);
            }
        }
        dirtySyncFields.clear();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public void readFromNetworkBuffer(FriendlyByteBuf buf) {
        while (buf.isReadable()) {
            var updatedField = buf.readUtf();
            var field = syncData.clientSyncFields.get(updatedField);
            if (field == null) throw new IllegalStateException("Recieved update info for unknown field: " + updatedField);

            if (field.isCustomData) {
                field.bufReadModifier[0].invoke(holder, buf);
            }
            if (field.isComplex) {
                ISyncManaged currentValue = (ISyncManaged) field.handle.get(holder);
                currentValue.getSyncDataHolder().readFromNetworkBuffer(buf);
            } else {
                if (field.transformer == null) throw new IllegalStateException("Missing value transformer for field %s".formatted(field.fieldName));
                IValueTransformer<Object> transformer = (IValueTransformer<Object>) field.transformer;
                if (transformer.mustProvideObject()) {
                    transformer.readFromBuffer(buf, field.handle.get(holder));
                } else {
                    field.handle.set(transformer.readFromBuffer(buf, null));
                }
            }
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public CompoundTag serializeNBT(boolean writeClientFields) {
        CompoundTag tag = new CompoundTag();

        Map<String, ClassSyncData.FieldSyncData> fieldsToSerialize = (writeClientFields ? syncData.clientSyncFields :
                syncData.serverSaveFields);

        for (var fieldEntry : fieldsToSerialize.entrySet()) {
            var field = fieldEntry.getValue();
            if (field.isCustomData) {
                var result = field.nbtSaveModifiers[0].invoke(holder, new CompoundTag(), writeClientFields);
                tag.put(field.nbtSaveKey, (Tag) result);
                continue;
            }

            Tag nbtValue;
            if (field.isComplex) {
                ISyncManaged currentValue = (ISyncManaged) field.handle.get(holder);
                if (currentValue != null) nbtValue = currentValue.getSyncDataHolder().serializeNBT(writeClientFields);
                else nbtValue = new CompoundTag();
            } else {
                if (field.transformer == null) {
                    throw new IllegalStateException("no value transformer for field: " + field.fieldName);
                }
                IValueTransformer<Object> transformer = (IValueTransformer<Object>) field.transformer;
                Object result = field.handle.get(holder);
                if (result == null) continue;
                nbtValue = transformer.serializeNBT(result);
            }

            if (!writeClientFields) {
                for (MethodHandle modifier : field.nbtSaveModifiers) {
                    nbtValue = (Tag) modifier.invoke(holder, nbtValue);
                }
            }
            tag.put(field.nbtSaveKey, nbtValue);
        }
        return tag;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public void deserializeNBT(CompoundTag tag, boolean readingClientFields) {
        Map<String, ClassSyncData.FieldSyncData> fieldsToCheck = readingClientFields ? syncData.clientSyncFields :
                syncData.serverSaveFields;
        for (var fieldEntry : fieldsToCheck.entrySet()) {
            var field = fieldEntry.getValue();
            if (field.isCustomData) {
                field.nbtLoadModifiers[0].invoke(holder, tag, readingClientFields);
                continue;
            }

            Tag savedValue = tag.get(field.nbtSaveKey);
            if (savedValue == null || savedValue instanceof CompoundTag compound && compound.isEmpty()) continue;

            if (field.isComplex && savedValue instanceof CompoundTag compound) {
                ISyncManaged currentVal = (ISyncManaged) field.handle.get(holder);
                if (currentVal == null) {
                    throw new IllegalArgumentException("Field %s is null and cannot be instantiated".formatted(field.fieldName));
                }
                currentVal.getSyncDataHolder().deserializeNBT(compound, readingClientFields);
            } else {
                if (field.transformer == null) {
                    throw new IllegalStateException("no value transformer for field: " + field.fieldName);
                }
                IValueTransformer<Object> transformer = (IValueTransformer<Object>) field.transformer;

                if (transformer.mustProvideObject()) {
                    transformer.deserializeNBT(savedValue, field.handle.get(holder));
                } else {
                    field.handle.set(holder, transformer.deserializeNBT(savedValue, null));
                }
            }
            if (!readingClientFields) {
                for (MethodHandle modifier : field.nbtLoadModifiers) {
                    modifier.invoke(holder, savedValue);
                }
            }
        }
        holder.onSaveDataLoaded();
    }
}
