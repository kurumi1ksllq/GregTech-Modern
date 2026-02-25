package com.gregtechceu.gtceu.api.item;

import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

public interface IMergeableNBTSerializable extends INBTSerializable<Tag> {

    void prepareForComparisonWith(INBTSerializable<Tag> other);
}
