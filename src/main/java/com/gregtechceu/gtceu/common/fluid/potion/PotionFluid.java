package com.gregtechceu.gtceu.common.fluid.potion;

import com.gregtechceu.gtceu.GTCEu;

import com.gregtechceu.gtceu.data.fluid.GTFluids;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class PotionFluid extends BaseFlowingFluid {

    public PotionFluid(Properties properties) {
        super(properties
                .bucket(() -> Items.AIR)
                .block(() -> (LiquidBlock) Blocks.WATER));
        registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
    }

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
        super.createFluidStateDefinition(builder);
        builder.add(LEVEL);
    }

    public static FluidStack of(int amount, Holder<Potion> potion) {
        FluidStack fluidStack = new FluidStack(GTFluids.POTION.get()
                .getSource(), amount);
        addPotionToFluidStack(fluidStack, potion);
        return fluidStack;
    }

    public static FluidStack withEffects(int amount, Holder<Potion> potion, List<MobEffectInstance> customEffects) {
        FluidStack fluidStack = of(amount, potion);
        appendEffects(fluidStack, customEffects);
        return fluidStack;
    }

    public static FluidStack addPotionToFluidStack(FluidStack fluidStack, Holder<Potion> potion) {
        fluidStack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        return fluidStack;
    }

    public static FluidStack appendEffects(FluidStack fluidStack, Collection<MobEffectInstance> customEffects) {
        if (customEffects.isEmpty())
            return fluidStack;
        fluidStack.update(DataComponents.POTION_CONTENTS, PotionContents.EMPTY, pot -> {
            for (MobEffectInstance effect : customEffects) {
                pot.withEffectAdded(effect);
            }
            return pot;
        });
        return fluidStack;
    }

    @Override
    public boolean isSource(FluidState state) {
        return this == GTFluids.POTION.get().getSource();
    }

    @Override
    public int getAmount(FluidState state) {
        return state.getValue(LEVEL);
    }

    public static class PotionFluidType extends FluidType {

        private static final ResourceLocation texture = GTCEu.id("block/fluids/fluid.potion");

        /**
         * Default constructor.
         *
         * @param properties the general properties of the fluid type
         */
        public PotionFluidType(Properties properties, ResourceLocation still, ResourceLocation flow) {
            super(properties);
        }

        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {

                @Override
                public ResourceLocation getStillTexture() {
                    return texture;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return texture;
                }

                @Override
                public int getTintColor(FluidStack stack) {
                    return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getColor() | 0xff000000;
                }
            });
        }

        @Override
        public String getDescriptionId(FluidStack stack) {
            PotionContents tag = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            return Potion.getName(tag.potion(), Items.POTION.getDescriptionId() + ".effect.");
        }
    }
}
