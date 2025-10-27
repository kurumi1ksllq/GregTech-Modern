package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.item.component.forge.IComponentCapability;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class SpoilableBehaviour implements IItemComponent, IComponentCapability {

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
    public @NotNull <T> LazyOptional<T> getCapability(ItemStack itemStack, @NotNull Capability<T> cap) {
        return GTCapability.CAPABILITY_SPOILABLE_ITEM.orEmpty(cap,
                LazyOptional.of(() -> new SpoilableItemStack(itemStack) {

                    @Override
                    public long getSpoilTicks() {
                        return ticks.apply(getStack());
                    }

                    @Override
                    public ItemStack spoilResult() {
                        return spoilResult.apply(getStack());
                    }

                    @Override
                    protected Component getSpoilResultTooltip() {
                        return spoilsInto.apply(getStack());
                    }
                }));
    }

    public ICapabilityProvider toCapProvider(ItemStack stack) {
        return new ICapabilityProvider() {

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability,
                                                              @Nullable Direction direction) {
                return SpoilableBehaviour.this.getCapability(stack, capability);
            }
        };
    }
}
