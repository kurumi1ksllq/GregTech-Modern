package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.item.ISpoilableItemStack;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IDurabilityBar;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpoilableBehaviour implements ISpoilableItem, IAddInformation, IDurabilityBar {

    private final long ticks;
    private final ItemLike spoilResult;

    public SpoilableBehaviour(long ticks, ItemLike spoilResult) {
        this.ticks = ticks;
        this.spoilResult = spoilResult;
    }

    @Override
    public long getSpoilTicks(ItemStack stack) {
        return ticks;
    }

    @Override
    public ItemStack spoilResult(ItemStack stack) {
        return new ItemStack(spoilResult, stack.getCount());
    }

    @Override
    public boolean shouldSpoil(ItemStack stack) {
        return true;
    }

    public long getTicksUntilSpoiled(ItemStack stack) {
        return ((ISpoilableItemStack) (Object) stack).gtceu$getRemainingTicks(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        ISpoilableItemStack spoilable = (ISpoilableItemStack) (Object) stack;
        if (spoilable == null) return;
        tooltipComponents.add(Component.translatable(
                "gtceu.tooltip.spoil_time_remaining",
                Component.literal(FormattingUtil.formatTime(getTicksUntilSpoiled(stack)))
                        .withStyle(ChatFormatting.DARK_AQUA)));
        tooltipComponents.add(Component.translatable(
                "gtceu.tooltip.spoils_into",
                spoilResult(stack).getDisplayName()));
        if (isAdvanced.isAdvanced()) {
            tooltipComponents.add(Component.translatable(
                    "gtceu.tooltip.spoil_time_total",
                    Component.literal(FormattingUtil.formatTime(getSpoilTicks(stack)))
                            .withStyle(ChatFormatting.GREEN)));
            tooltipComponents.add(Component.translatable(
                    "gtceu.tooltip.creation_tick",
                    spoilable.gtceu$getCreationTick(null)));
        }
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return FastColor.ARGB32.color(255, 255, 255, 255);
    }

    @Override
    public boolean doDamagedStateColors(ItemStack itemStack) {
        return false;
    }

    @Override
    public @Nullable IntIntPair getDurabilityColorsForDisplay(ItemStack itemStack) {
        return IntIntPair.of(getBarColor(itemStack), getBarColor(itemStack));
    }

    @Override
    public float getDurabilityForDisplay(ItemStack stack) {
        return (float) getTicksUntilSpoiled(stack) / getSpoilTicks(stack);
    }
}
