package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public class CoverBehaviorTransformer implements IValueTransformer<CoverBehavior> {
    @Override
    public boolean mustProvideObject() {
        return true;
    }

    @Override
    public void writeToBuffer(CoverBehavior value, FriendlyByteBuf buf) {
    }

    @Override
    public CoverBehavior readFromBuffer(FriendlyByteBuf buf, CoverBehavior currentValue) {
        return null;
    }

    @Override
    public Tag serializeNBT(CoverBehavior value) {
        return null;
    }

    @Override
    public CoverBehavior deserializeNBT(Tag tag, @Nullable CoverBehavior currentVal) {
        return null;
    }
}
