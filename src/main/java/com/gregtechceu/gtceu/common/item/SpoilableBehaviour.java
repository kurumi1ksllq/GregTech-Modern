package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.ISpoilableItemStack;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IDurabilityBar;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SpoilableBehaviour implements ISpoilableItem, IAddInformation, IDurabilityBar {

    private static final Map<Item, SpoilableBehaviour> ATTACHED_COMPONENTS = new HashMap<>();

    private final Function<ItemStack, Long> ticks;
    private final Function<ItemStack, ItemStack> spoilResult;
    private final Function<ItemStack, Component> spoilsInto;

    public SpoilableBehaviour(
                              Function<ItemStack, Long> tickProvider,
                              Function<ItemStack, ItemStack> spoilResultProvider,
                              Function<ItemStack, Component> spoilsIntoTooltip) {
        this.ticks = tickProvider;
        this.spoilResult = spoilResultProvider;
        this.spoilsInto = spoilsIntoTooltip;
    }

    public SpoilableBehaviour(Function<ItemStack, Long> tickProvider,
                              Function<ItemStack, ItemStack> spoilResultProvider) {
        this(tickProvider, spoilResultProvider, stack -> spoilResultProvider.apply(stack).getDisplayName());
    }

    public SpoilableBehaviour(long ticks, ItemStack spoilResult) {
        this(stack -> ticks, stack -> spoilResult.copyWithCount(stack.getCount()));
    }

    public SpoilableBehaviour(long ticks, ItemLike spoilResult) {
        this(stack -> ticks, stack -> spoilResult.asItem().getDefaultInstance().copyWithCount(stack.getCount()));
    }

    @Override
    public long getSpoilTicks(ItemStack stack) {
        return ticks.apply(stack);
    }

    @Override
    public ItemStack spoilResult(ItemStack stack) {
        return spoilResult.apply(stack);
    }

    @Override
    public boolean shouldSpoil(ItemStack stack) {
        return true;
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
                spoilsInto.apply(stack)));
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

    public static @Nullable ISpoilableItem getSpoilable(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ISpoilableItem spoilable) return spoilable;
        if (ATTACHED_COMPONENTS.containsKey(item)) return ATTACHED_COMPONENTS.get(item);
        SpoilableBehaviour behaviour = GTValues.DEFAULT_SPOIL_BEHAVIOR.apply(item);
        if (behaviour != null) ATTACHED_COMPONENTS.put(item, behaviour);
        return behaviour;
    }

    public static void unspoil(ItemLike item) {
        ATTACHED_COMPONENTS.remove(item.asItem());
    }

    public void attachTo(ItemLike item) {
        ATTACHED_COMPONENTS.put(item.asItem(), this);
    }
}
