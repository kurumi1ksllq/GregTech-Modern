package com.gregtechceu.gtceu.syncdata;

import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Represents a class with fields that have sync annotations.
 * <p>
 * A field of type {@code T} can be marked with sync annotations if:
 * <ul>
 * <li>{@code T} is primitive
 * <li>{@code T} has an {@link IValueTransformer} registered
 * <li>{@code T} implements {@link INBTSerializable<Tag>}
 * <li>{@code T} is a {@link ISyncManaged} class
 * </ul>
 *
 * @see SyncDataHolder
 */
public interface ISyncManaged {

    SyncDataHolder getSyncDataHolder();

    void scheduleRenderUpdate();

    void markAsChanged();

    default void onSaveDataLoaded() {}
}
