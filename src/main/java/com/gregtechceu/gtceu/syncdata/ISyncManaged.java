package com.gregtechceu.gtceu.syncdata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

/**
* Represents a class with fields that have sync annotations. <p>
 * A field of type {@code T} can be marked with sync annotations if:
 * <ul>
 *     <li>{@code T} is primitive
 *     <li>{@code T} has an {@link IValueTransformer} registered
 *     <li>{@code T} implements {@link INBTSerializable<Tag>}
 *     <li>{@code T} is a {@link ISyncManaged} class
 * </ul>
 *
 * @see ManagedSyncBlockEntity
* */
public interface ISyncManaged {

    SyncDataHolder getSyncDataHolder();

    default void serializeCustomNBTData(CompoundTag tag, boolean isItemDrop) {}

    default void deserializeCustomNBTData(CompoundTag tag) {}

    void onChanged();
}
