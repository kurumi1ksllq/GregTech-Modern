package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public class GTRecipeTypeTransformer implements IValueTransformer<GTRecipeType> {

    @Override
    public void writeToBuffer(GTRecipeType value, FriendlyByteBuf buf) {
        buf.writeBoolean(value == null);
        if (value == null) return;
        buf.writeResourceLocation(value.registryName);
    }

    @Override
    public GTRecipeType readFromBuffer(FriendlyByteBuf buf, GTRecipeType currentValue) {
        if (buf.readBoolean()) return null;
        var id = buf.readResourceLocation();
        return GTRegistries.RECIPE_TYPES.getOrDefault(id, null);
    }

    @Override
    public Tag serializeNBT(GTRecipeType value) {
        var tag = new CompoundTag();
        if (value == null) return tag;
        tag.putString("namespace", value.registryName.getNamespace());
        tag.putString("path", value.registryName.getPath());
        return tag;
    }

    @Override
    public GTRecipeType deserializeNBT(Tag tag, @Nullable GTRecipeType currentVal) {
        if (!(tag instanceof CompoundTag compound) || compound.isEmpty()) return null;
        String namespace = compound.getString("namespace");
        String path = compound.getString("path");
        return GTRegistries.RECIPE_TYPES.get(new ResourceLocation(namespace, path));
    }
}
