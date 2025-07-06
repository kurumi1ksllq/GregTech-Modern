package com.gregtechceu.gtceu.sync_system;

import net.minecraft.nbt.CompoundTag;

public interface ICompoundTagSerializable {

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag tag);
}
