package com.gregtechceu.gtceu.api.cover.filter;

import com.gregtechceu.gtceu.common.cover.filter.MatchResult;
import com.gregtechceu.gtceu.utils.TagExprFilter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import lombok.Getter;

import java.util.Objects;
import java.util.function.Consumer;

public class TagFluidFilter extends TagFilter<FluidStack, FluidFilter> implements FluidFilter {

    private final Object2BooleanMap<Fluid> cache = new Object2BooleanOpenHashMap<>();

    @Getter
    protected long maxStackSize = 1L;

    protected TagFluidFilter() {}

    public static TagFluidFilter loadFilter(ItemStack itemStack) {
        return loadFilter(Objects.requireNonNullElseGet(itemStack.getTag(), CompoundTag::new),
                filter -> itemStack.setTag(filter.saveFilter()));
    }

    private static TagFluidFilter loadFilter(CompoundTag tag, Consumer<FluidFilter> itemWriter) {
        var handler = new TagFluidFilter();
        handler.itemWriter = itemWriter;
        handler.oreDictFilterExpression = tag.getString("oreDict");
        handler.matchExpr = null;
        handler.cache.clear();
        handler.matchExpr = TagExprFilter.parseExpression(handler.oreDictFilterExpression);
        return handler;
    }

    @Override
    public CompoundTag saveFilter() {
        CompoundTag tag = super.saveFilter();
        tag.putString("type", FilterType.FLUID_TAG.getSerializedName());
        return tag;
    }

    @Override
    public int getMaxTransferSize() {
        return (int) maxStackSize;
    }

    @Override
    public void setMaxTransferSize(int transferRate) {
        transferRate = Mth.clamp(transferRate, 1, Integer.MAX_VALUE);
        if (this.maxStackSize != transferRate) {
            this.maxStackSize = transferRate;
            onUpdated.accept(this);
        }
    }

    public void setOreDict(String oreDict) {
        cache.clear();
        super.setOreDict(oreDict);
    }

    @Override
    public boolean test(FluidStack fluidStack) {
        if (oreDictFilterExpression.isEmpty()) return false;
        if (cache.containsKey(fluidStack.getFluid())) return cache.getOrDefault(fluidStack.getFluid(), false);
        if (TagExprFilter.tagsMatch(matchExpr, fluidStack)) {
            cache.put(fluidStack.getFluid(), true);
            return true;
        }
        cache.put(fluidStack.getFluid(), false);
        return false;
    }

    @Override
    public int testFluidAmount(FluidStack fluidStack) {
        return test(fluidStack) ? Integer.MAX_VALUE : 0;
    }

    @Override
    public boolean supportsAmounts() {
        return false;
    }

    @Override
    public MatchResult apply(FluidStack fluidStack) {
        var match = TagExprFilter.tagsMatch(matchExpr, fluidStack);
        return MatchResult.create(match != isBlackList(), match ? fluidStack.copy() : FluidStack.EMPTY, -1);
    }
}
