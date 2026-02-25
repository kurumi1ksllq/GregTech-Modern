package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.capability.GTCapability;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import com.gregtechceu.gtceu.api.item.component.SpoilContext;
import com.gregtechceu.gtceu.api.item.component.SpoilUtils;
import com.gregtechceu.gtceu.api.item.component.forge.IComponentCapability;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SpoilableBehaviour implements IItemComponent, IComponentCapability {

    private final Function<ItemStack, Long> ticks;
    private final SpoilResultProvider spoilResult;
    private final Function<ItemStack, Component> spoilsIntoTooltip;

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(ItemStack itemStack, @NotNull Capability<T> cap) {
        MutableObject<LazyOptional<ISpoilableItem>> lazyOptional = new MutableObject<>();
        lazyOptional.setValue(LazyOptional.of(() -> new SpoilableBehaviourStack(itemStack)));
        return GTCapability.CAPABILITY_SPOILABLE_ITEM.orEmpty(cap, lazyOptional.getValue());
    }

    public ICapabilityProvider toCapProvider(ItemStack stack) {
        return new SpoilableBehaviourStack(stack);
    }

    public class SpoilableBehaviourStack extends SpoilableItemStack {

        private SpoilableBehaviourStack(ItemStack stack) {
            super(stack);
        }

        @Override
        public long getSpoilTicks() {
            return ticks.apply(getStack());
        }

        @Override
        public ItemStack spoilResult(SpoilContext spoilContext, boolean simulate) {
            return spoilResult.getSpoilResult(getStack(), spoilContext, simulate);
        }

        @Override
        protected Component getSpoilResultTooltip() {
            return spoilsIntoTooltip.apply(getStack());
        }

        @Override
        protected void onItemChanged() {}
    }

    @FunctionalInterface
    public interface SpoilResultProvider {

        ItemStack getSpoilResult(@NotNull ItemStack stack, @NotNull SpoilContext spoilContext, boolean simulate);
    }

    public static class Builder {

        private Function<ItemStack, Long> ticks;
        private SpoilResultProvider result;
        private Function<ItemStack, Component> tooltip;

        private Builder() {
            ticks(100);
            result(ItemStack.EMPTY);
        }

        public SpoilableBehaviour build() {
            return new SpoilableBehaviour(ticks, result, tooltip);
        }

        public Builder ticks(Function<ItemStack, Long> ticks) {
            this.ticks = ticks;
            return this;
        }

        public Builder result(SpoilResultProvider result) {
            this.result = result;
            return this;
        }

        public Builder tooltip(Function<ItemStack, Component> tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder ticks(long ticks) {
            return ticks(stack -> ticks);
        }

        public Builder result(ItemLike itemLike) {
            return result(stack -> itemLike.asItem().getDefaultInstance().copyWithCount(stack.getCount()));
        }

        public Builder result(ItemStack stack) {
            return result(stack1 -> stack.copyWithCount(stack1.getCount()));
        }

        public Builder result(Function<ItemStack, ItemStack> result) {
            return result((stack, spoilContext, simulate) -> result.apply(stack))
                    .tooltip(stack -> {
                        ItemStack resultStack = result.apply(stack);
                        if (resultStack.isEmpty()) return Component.empty();
                        return resultStack.getHoverName();
                    });
        }

        public Builder result(EntityType<? extends Mob> entityType) {
            return result(() -> entityType);
        }

        public Builder result(Supplier<? extends EntityType<? extends Mob>> entityType) {
            SpoilResultProvider previousResult = result;
            Function<ItemStack, Component> previousTooltip = tooltip;
            return result((stack, spoilContext, simulate) -> {
                if (!simulate) {
                    EntityType<? extends Mob> type = entityType.get();
                    SpoilUtils.spawnEntity(spoilContext, type, stack.getCount());
                }
                return previousResult.getSpoilResult(stack, spoilContext, simulate);
            }).tooltip(stack -> {
                EntityType<? extends Mob> type = entityType.get();
                MutableComponent component = type.getDescription().copy();
                Component previous = previousTooltip.apply(stack);
                if (!previous.getString().isEmpty()) component.append(", ").append(previous);
                return component;
            });
        }

        public Builder multiplyResult(int mult) {
            SpoilResultProvider prevResult = result;
            Function<ItemStack, Component> previousTooltip = tooltip;
            return result((stack, spoilContext, simulate) -> {
                ItemStack total = prevResult.getSpoilResult(stack, spoilContext, simulate);
                for (int i = 1; i < mult; i++) {
                    ItemStack temp = prevResult.getSpoilResult(stack, spoilContext, simulate);
                    if (ItemStack.isSameItemSameTags(total, temp)) total.grow(temp.getCount());
                }
                return total;
            }).tooltip(stack -> {
                MutableComponent component = Component.literal("(");
                component.append(previousTooltip.apply(stack));
                component.append(") x").append(Integer.toString(mult));
                return component;
            });
        }

        public Builder tooltip(Component tooltip) {
            return tooltip(stack -> tooltip);
        }
    }
}
