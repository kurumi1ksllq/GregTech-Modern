package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

public class MaterialTransformer implements IValueTransformer<Material> {

    @Override
    public void writeBufferPayload(FriendlyByteBuf buf, Material value) {
        buf.writeUtf(value.getResourceLocation().toString());
    }

    @Override
    public Material readBufferPayload(FriendlyByteBuf buf, Material currentVal) {
        return GTCEuAPI.materialManager.getMaterial(buf.readUtf());
    }

    @Override
    public Tag serializeNBT(Material currentValue) {
        return StringTag.valueOf(currentValue.getResourceLocation().toString());
    }

    @Override
    public Material deserializeNBT(Tag tag, Material currentValue) {
        return GTCEuAPI.materialManager.getMaterial(tag.getAsString());
    }
}
