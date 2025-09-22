package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.GTCEu;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.*;

/**
 * Class that holds all sync info for an {@link ISyncManaged} object.
 */
public class SyncDataHolder {

    private final ClassSyncData syncData;
    private final ISyncManaged holder;

    private final ObjectSet<String> dirtySyncFields = new ObjectOpenHashSet<>();
    private boolean resyncAll = false;

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

    public void resyncAllFields() {
        resyncAll = true;
        holder.markAsChanged();
    }

    @SuppressWarnings("unchecked")
    public void writeToNetworkBuffer(FriendlyByteBuf buf) {
        for (var fieldEntry : syncData.clientSyncFields.entrySet()) {
            if (!resyncAll && !(dirtySyncFields.contains(fieldEntry.getKey()) || fieldEntry.getValue().isComplex)) continue;
            var field = fieldEntry.getValue();
            if (field.isCustomData) {

                try {
                    field.bufWriteModifier[0].invoke(holder, buf);
                } catch (Throwable e) {
                    GTCEu.LOGGER.error("Error while invoking bufWriteModifier for field {}", field.fieldName);
                    GTCEu.LOGGER.error(e.getMessage());
                    return;
                }
            }

            if (field.isComplex) {
                ISyncManaged currentValue = (ISyncManaged) field.handle.get(holder);
                if (currentValue != null) {
                    buf.writeUtf(fieldEntry.getKey());
                    currentValue.getSyncDataHolder().writeToNetworkBuffer(buf);
                }
            } else {
                if (field.transformer == null) {
                    GTCEu.LOGGER.error("no value transformer registered for field: {}", field.fieldName);
                    return;
                }
                IValueTransformer<Object> transformer = (IValueTransformer<Object>) field.transformer;
                Object result = field.handle.get(holder);
                buf.writeUtf(fieldEntry.getKey());
                buf.writeBoolean(result == null);
                if (result == null) continue;
                transformer.writeToBuffer(result, buf);
            }
        }
        resyncAll = false;
        dirtySyncFields.clear();
    }

    @SuppressWarnings("unchecked")
    public void readFromNetworkBuffer(FriendlyByteBuf buf) {
        while (buf.isReadable()) {
            var updatedField = buf.readUtf();
            boolean fieldNull = buf.readBoolean();
            var field = syncData.clientSyncFields.get(updatedField);
            if (field == null) {
                GTCEu.LOGGER.error("Recieved update info for unknown field: {}", updatedField);
                return;
            };

            if (field.isCustomData) {
                try {
                    field.bufReadModifier[0].invoke(holder, buf);
                } catch (Throwable e) {
                    GTCEu.LOGGER.error("Error while invoking bufReadModifier for field {}", field.fieldName);
                    GTCEu.LOGGER.error(e.getMessage());
                    return;
                }
            }
            if (field.isComplex) {
                ISyncManaged currentValue = (ISyncManaged) field.handle.get(holder);
                currentValue.getSyncDataHolder().readFromNetworkBuffer(buf);
            } else {
                if (fieldNull) {
                    field.handle.set(holder, null);
                    continue;
                }
                if (field.transformer == null) {
                    GTCEu.LOGGER.error("no value transformer registered for field: {}", field.fieldName);
                    return;
                }
                IValueTransformer<Object> transformer = (IValueTransformer<Object>) field.transformer;
                if (transformer.mustProvideObject()) {
                    transformer.readFromBuffer(buf, field.handle.get(holder));
                } else {
                    try {
                        field.handle.set(holder, transformer.readFromBuffer(buf, null));
                    } catch (UnsupportedOperationException e) {
                        GTCEu.LOGGER.error("Sync error: failed to perform VarHandle set: unsupported op {} {}", field.fieldName, field.handle.toString());
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public CompoundTag serializeNBT(boolean writeClientFields) {
        CompoundTag tag = new CompoundTag();

        Map<String, ClassSyncData.FieldSyncData> fieldsToSerialize = (writeClientFields ? syncData.clientSyncFields :
                syncData.serverSaveFields);

        for (var fieldEntry : fieldsToSerialize.entrySet()) {
            var field = fieldEntry.getValue();
            if (field.isCustomData) {
                try {
                    Object result = field.nbtSaveModifiers[0].invoke(holder, new CompoundTag(), writeClientFields);
                    tag.put(field.nbtSaveKey, (Tag) result);
                } catch (Throwable e) {
                    GTCEu.LOGGER.error("Error while invoking nbtSaveModifier for field {}", field.fieldName);
                    GTCEu.LOGGER.error(e.getMessage());
                    return new CompoundTag();
                }
                continue;
            }

            Tag nbtValue;
            if (field.isComplex) {
                ISyncManaged currentValue = (ISyncManaged) field.handle.get(holder);
                if (currentValue != null) nbtValue = currentValue.getSyncDataHolder().serializeNBT(writeClientFields);
                else nbtValue = new CompoundTag();
            } else {
                if (field.transformer == null) {
                    GTCEu.LOGGER.error("no value transformer for field: {}", field.fieldName);
                    return new CompoundTag();
                }
                IValueTransformer<Object> transformer = (IValueTransformer<Object>) field.transformer;
                Object result = field.handle.get(holder);
                if (result == null) continue;
                nbtValue = transformer.serializeNBT(result);
            }

            for (MethodHandle modifier : field.nbtSaveModifiers) {
                try {
                    nbtValue = (Tag) modifier.invoke(holder, nbtValue, writeClientFields);
                } catch (Throwable e) {
                    GTCEu.LOGGER.error("Error while invoking nbtSaveModifier for field {}", field.fieldName);
                    GTCEu.LOGGER.error(e.getMessage());
                    return new CompoundTag();
                }
            }
            tag.put(field.nbtSaveKey, nbtValue);
        }
        return tag;
    }

    @SuppressWarnings("unchecked")
    public void deserializeNBT(CompoundTag tag, boolean readingClientFields) {
        Map<String, ClassSyncData.FieldSyncData> fieldsToCheck = readingClientFields ? syncData.clientSyncFields :
                syncData.serverSaveFields;
        if (tag.getAllKeys().size() != fieldsToCheck.size()) {
            GTCEu.LOGGER.warn("Sync: Mismatch between field count: expected {}, got {} - {}", fieldsToCheck.size(), tag.getAllKeys().size(), holder);
            var actualFields = tag.getAllKeys();
            for (var fieldEntry : fieldsToCheck.entrySet()) {
                actualFields.remove(fieldEntry.getValue().nbtSaveKey);
                if (!(tag.contains(fieldEntry.getValue().nbtSaveKey))) GTCEu.LOGGER.warn("No value for field: {}", fieldEntry.getValue().fieldName);
            }
            actualFields.forEach((v) -> GTCEu.LOGGER.warn("Unknown NBT value was defined: {}", v));
        }
        for (var fieldEntry : fieldsToCheck.entrySet()) {
            var field = fieldEntry.getValue();
            if (field.isCustomData) {
                try {
                    field.nbtLoadModifiers[0].invoke(holder, tag, readingClientFields);
                } catch (Throwable e) {
                    GTCEu.LOGGER.error("Error while invoking nbtLoadModifier for field {}", field.fieldName);
                    GTCEu.LOGGER.error(e.getMessage());
                    return;
                }
                continue;
            }

            Tag savedValue = tag.get(field.nbtSaveKey);
            if (savedValue == null || savedValue instanceof CompoundTag compound && compound.isEmpty()) continue;

            if (field.isComplex && savedValue instanceof CompoundTag compound) {
                ISyncManaged currentVal = (ISyncManaged) field.handle.get(holder);
                if (currentVal == null) {
                    GTCEu.LOGGER.error("Sync error: ISyncManaged field was null, cannot instantiate {}", field.fieldName);
                    return;
                }
                currentVal.getSyncDataHolder().deserializeNBT(compound, readingClientFields);
            } else {
                if (field.transformer == null) {
                    GTCEu.LOGGER.error("missing value transformer for field: {}", field.fieldName);
                    return;
                }
                IValueTransformer<Object> transformer = (IValueTransformer<Object>) field.transformer;

                if (transformer.mustProvideObject()) {
                    transformer.deserializeNBT(savedValue, field.handle.get(holder));
                } else {
                    try {
                        field.handle.set(holder, transformer.deserializeNBT(savedValue, null));
                    } catch (UnsupportedOperationException e) {
                        GTCEu.LOGGER.error("Sync error: failed to perform VarHandle set: unsupported op {} {}", field.fieldName, field.handle.toString());
                    }
                }
            }
            for (MethodHandle modifier : field.nbtLoadModifiers) {
                try {
                    modifier.invoke(holder, savedValue, readingClientFields);
                } catch (Throwable e) {
                    GTCEu.LOGGER.error("Error while invoking nbtLoadModifier for field {}", field.fieldName);
                    GTCEu.LOGGER.error(e.getMessage());
                    return;
                }
            }
        }
        holder.onSaveDataLoaded();
    }
}
