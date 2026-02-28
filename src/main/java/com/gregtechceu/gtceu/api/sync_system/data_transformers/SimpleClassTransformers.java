package com.gregtechceu.gtceu.api.sync_system.data_transformers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

public class SimpleClassTransformers {
    public static class ItemStackTransformer implements ValueTransformer<ItemStack> {
        @Override
        public Tag serializeNBT(ItemStack value, TransformerContext<ItemStack> context) {
            return value.save(context.lookup());
        }

        @Override
        public @Nullable ItemStack deserializeNBT(Tag tag, TransformerContext<ItemStack> context) {
            return ItemStack.parse(context.lookup(), tag).orElse(ItemStack.EMPTY);
        }
    }

    public static class FluidStackTransformer implements ValueTransformer<FluidStack> {
        @Override
        public Tag serializeNBT(FluidStack value, TransformerContext<FluidStack> context) {
            return value.save(context.lookup());
        }

        @Override
        public @Nullable FluidStack deserializeNBT(Tag tag, TransformerContext<FluidStack> context) {
            return FluidStack.parse(context.lookup(), tag).orElse(FluidStack.EMPTY);
        }
    }

    public static class BlockPosTransformer implements ValueTransformer<BlockPos> {
        @Override
        public Tag serializeNBT(BlockPos value, TransformerContext<BlockPos> context) {
            return null;
        }

        @Override
        public @Nullable BlockPos deserializeNBT(Tag tag, TransformerContext<BlockPos> context) {
            return null;
        }
    }

    public static class ComponentTransformer implements ValueTransformer<Component> {
        @Override
        public Tag serializeNBT(Component value, TransformerContext<Component> context) {
            return null;
        }

        @Override
        public @Nullable Component deserializeNBT(Tag tag, TransformerContext<Component> context) {
            return null;
        }
    }
}
