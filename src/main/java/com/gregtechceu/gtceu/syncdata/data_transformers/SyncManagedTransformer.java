package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.syncdata.ISyncManaged;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public class SyncManagedTransformer implements IValueTransformer<ISyncManaged> {

    @Override
    public boolean mustProvideObject() {
        return true;
    }

    @Override
    public void writeToBuffer(ISyncManaged value, FriendlyByteBuf buf) {
        value.getSyncDataHolder().writeToNetworkBuffer(buf);
    }

    @Override
    public ISyncManaged readFromBuffer(FriendlyByteBuf buf, ISyncManaged currentValue) {
        currentValue.getSyncDataHolder().readFromNetworkBuffer(buf);
        return currentValue;
    }

    @Override
    public Tag serializeClientChunkPayload(ISyncManaged value) {
        return value.getSyncDataHolder().serializeNBT(true);
    }

    @Override
    public ISyncManaged deserializeClientChunkPayload(Tag tag, @Nullable ISyncManaged currentVal) {
        if (!(tag instanceof CompoundTag compound) || currentVal == null) return currentVal;
        currentVal.getSyncDataHolder().deserializeNBT(compound, true);
        return currentVal;
    }

    @Override
    public Tag serializeNBT(ISyncManaged value) {
        return value.getSyncDataHolder().serializeNBT(false);
    }

    @Override
    public ISyncManaged deserializeNBT(Tag tag, @Nullable ISyncManaged currentVal) {
        if (!(tag instanceof CompoundTag compound) || currentVal == null) return currentVal;
        currentVal.getSyncDataHolder().deserializeNBT(compound, false);
        return currentVal;
    }
}
