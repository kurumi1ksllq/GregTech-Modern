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
        return currentValue.getSyncDataHolder().readFromNetworkBuffer(buf);
    }

    @Override
    public Tag serializeNBT(ISyncManaged value) {
        return value.getSyncDataHolder().serializeNBT();
    }

    @Override
    public ISyncManaged deserializeNBT(Tag tag, @Nullable ISyncManaged currentVal) {
        if (!(tag instanceof CompoundTag compound) || currentVal == null) return currentVal;
        currentVal.getSyncDataHolder().deserializeNBT(compound);
        return currentVal;
    }
}
