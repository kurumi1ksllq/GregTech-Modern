package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import org.jetbrains.annotations.Nullable;

public class GTRecipeTypeTransformer implements IValueTransformer<GTRecipeType> {

    @Override
    public void writeBufferPayload(FriendlyByteBuf buffer, GTRecipeType value) {
        buffer.writeResourceLocation(value.registryName);
    }

    @Override
    public GTRecipeType readBufferPayload(FriendlyByteBuf buffer, @Nullable GTRecipeType currentVal) {
        return GTRegistries.RECIPE_TYPES.get(buffer.readResourceLocation());
    }

    @Override
    public boolean canSaveAsNBT() {
        return false;
    }

    @Override
    public Tag serializeNBT(GTRecipeType value) {
        return null;
    }

    @Override
    public GTRecipeType deserializeNBT(Tag tag, @Nullable GTRecipeType currentVal) {
        return null;
    }
}
