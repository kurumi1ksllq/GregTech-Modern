package com.gregtechceu.gtceu.api.cover.filter;

import com.gregtechceu.gtceu.common.cover.filter.MatchResult;
import com.gregtechceu.gtceu.utils.TagExprFilter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import lombok.Getter;

import java.util.Objects;
import java.util.function.Consumer;

public class TagItemFilter extends TagFilter<ItemStack, ItemFilter> implements ItemFilter {

    private final Object2BooleanMap<Item> cache = new Object2BooleanOpenHashMap<>();

    @Getter
    protected int maxStackSize = 1;

    protected TagItemFilter() {}

    public static TagItemFilter loadFilter(ItemStack itemStack) {
        return loadFilter(Objects.requireNonNullElseGet(itemStack.getTag(), CompoundTag::new),
                filter -> itemStack.setTag(filter.saveFilter()));
    }

    private static TagItemFilter loadFilter(CompoundTag tag, Consumer<ItemFilter> itemWriter) {
        var handler = new TagItemFilter();
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
        return maxStackSize;
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
    public boolean test(ItemStack itemStack) {
        if (oreDictFilterExpression.isEmpty()) return false;
        if (cache.containsKey(itemStack.getItem())) return cache.getOrDefault(itemStack.getItem(), false);
        if (TagExprFilter.tagsMatch(matchExpr, itemStack)) {
            cache.put(itemStack.getItem(), true);
            return true;
        }
        cache.put(itemStack.getItem(), false);
        return false;
    }

    @Override
    public int testItemCount(ItemStack itemStack) {
        return test(itemStack) ? Integer.MAX_VALUE : 0;
    }

    @Override
    public boolean supportsAmounts() {
        return false;
    }

    @Override
    public MatchResult apply(ItemStack itemStack) {
        var match = TagExprFilter.tagsMatch(matchExpr, itemStack);
        return MatchResult.create(match != isBlackList(), match ? itemStack.copy() : ItemStack.EMPTY, -1);
    }
}
