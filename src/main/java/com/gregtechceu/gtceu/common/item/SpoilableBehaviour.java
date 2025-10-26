package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IDurabilityBar;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import com.gregtechceu.gtceu.api.item.component.forge.IComponentCapability;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class SpoilableBehaviour implements IAddInformation, IDurabilityBar, IItemComponent, IComponentCapability {

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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(stack);
        if (spoilable == null) return;
        tooltipComponents.add(Component.translatable(
                "gtceu.tooltip.spoil_time_remaining",
                Component.literal(FormattingUtil.formatTime(spoilable.getTicksUntilSpoiled()))
                        .withStyle(ChatFormatting.DARK_AQUA)));
        tooltipComponents.add(Component.translatable(
                "gtceu.tooltip.spoils_into",
                spoilsInto.apply(stack)));
        if (isAdvanced.isAdvanced()) {
            tooltipComponents.add(Component.translatable(
                    "gtceu.tooltip.spoil_time_total",
                    Component.literal(FormattingUtil.formatTime(spoilable.getSpoilTicks()))
                            .withStyle(ChatFormatting.GREEN)));
            tooltipComponents.add(Component.translatable(
                    "gtceu.tooltip.creation_tick",
                    spoilable.getCreationTick()));
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
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(stack);
        if (spoilable == null) return 0;
        return (float) spoilable.getTicksUntilSpoiled() / spoilable.getSpoilTicks();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(ItemStack itemStack, @NotNull Capability<T> cap) {
        return GTCapability.CAPABILITY_SPOILABLE_ITEM.orEmpty(cap, LazyOptional.of(() -> new SpoilableItemStack(itemStack) {
            @Override
            public long getSpoilTicks() {
                return ticks.apply(getStack());
            }

            @Override
            public ItemStack spoilResult() {
                return spoilResult.apply(getStack());
            }
        }));
    }

    public ICapabilityProvider toCapProvider(ItemStack stack) {
        return new ICapabilityProvider() {
            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
                return SpoilableBehaviour.this.getCapability(stack, capability);
            }
        };
    }
}
