package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.SpatialIngredient;
import com.gregtechceu.gtceu.common.data.GTRecipeCapabilities;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SpatialHatchPartMachine extends TieredIOPartMachine implements IRecipeHandler<SpatialIngredient> {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SpatialHatchPartMachine.class, TieredIOPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    @DescSynced
    @Getter
    private Vec3i offset = new BlockPos(Vec3i.ZERO);

    @Persisted
    @DescSynced
    @Getter
    private Vec3i size;

    public SpatialHatchPartMachine(IMachineBlockEntity holder, int tier, IO io) {
        super(holder, tier, io);
    }

    public int getMaxOffset() {
        return (int) Math.pow(2, tier >> 1);
    }

    public int getMaxSize() {
        return tier;
    }

    public void setOffset(Vec3i newOffset) {
        int max = getMaxOffset();
        offset = clamp(newOffset, -max, max);
    }

    public void setSize(Vec3i newSize) {
        int max = getMaxSize();
        size = clamp(newSize, 1, max);
    }

    private static Vec3i clamp(Vec3i vec, int min, int max) {
        return new Vec3i(
                Mth.clamp(vec.getX(), min, max),
                Mth.clamp(vec.getY(), min, max),
                Mth.clamp(vec.getZ(), min, max));
    }

    @Override
    public List<SpatialIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<SpatialIngredient> left,
                                                     boolean simulate) {
        return List.of();
    }

    @Override
    public @NotNull List<Object> getContents() {
        return List.of();
    }

    @Override
    public double getTotalContentAmount() {
        return 1;
    }

    @Override
    public RecipeCapability<SpatialIngredient> getCapability() {
        return GTRecipeCapabilities.SPATIAL;
    }
}
