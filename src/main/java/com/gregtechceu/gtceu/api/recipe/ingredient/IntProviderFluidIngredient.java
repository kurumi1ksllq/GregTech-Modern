package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.data.tag.GTIngredientTypes;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredientType;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class IntProviderFluidIngredient extends FluidIngredient {

    // spotless:
    public static final MapCodec<IntProviderFluidIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    FluidIngredient.CODEC.fieldOf("inner").forGetter(IntProviderFluidIngredient::getInner),
                    IntProvider.CODEC.fieldOf("count_provider").forGetter(IntProviderFluidIngredient::getCountProvider))
            .apply(instance, IntProviderFluidIngredient::new));
    // spotless:

    @Getter
    private final IntProvider countProvider;
    @Setter
    protected int sampledCount = -1;
    @Getter
    private final FluidIngredient inner;
    @Setter
    protected @NotNull FluidStack [] fluidStacks = null;

    public static final FluidStack[] EMPTY_STACK_ARRAY = new FluidStack[0];

    protected IntProviderFluidIngredient(FluidIngredient inner, IntProvider provider) {
        this.inner = inner;
        this.countProvider = provider;
    }

    public IntProviderFluidIngredient copy() {
        IntProviderFluidIngredient copied = new IntProviderFluidIngredient(this.inner, this.countProvider);
        copied.setSampledCount(this.sampledCount);
        copied.setFluidStacks(this.fluidStacks);
        return copied;
    }

    public FluidStack[] getFluids() {
        if (fluidStacks == null) {
            int cachedAmount = getSampledCount(GTValues.RNG);
            if (cachedAmount == 0) {
                return EMPTY_STACK_ARRAY;
            }
            var innerStacks = inner.getStacks();
            this.fluidStacks = new FluidStack[innerStacks.length];
            for (int i = 0; i < fluidStacks.length; i++) {
                fluidStacks[i] = innerStacks[i].copy();
                fluidStacks[i].setAmount(cachedAmount);
            }
        }
        return fluidStacks;
    }

    @Override
    public boolean test(@NotNull FluidStack stack) {
        return inner.test(stack);
    }

    @Override
    protected Stream<FluidStack> generateStacks() {
        return Stream.empty();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public FluidIngredientType<?> getType() {
        return GTIngredientTypes.INT_PROVIDER_FLUID_INGREDIENT.get();
    }

    public @NotNull FluidStack getMaxSizeStack() {
        FluidStack[] in = inner.getStacks();
        if (in.length == 0) return FluidStack.EMPTY;
        return in[0].copyWithAmount(countProvider.getMaxValue());
    }

    public int getSampledCount(@NotNull RandomSource random) {
        if (sampledCount == -1) {
            sampledCount = countProvider.sample(random);
        }
        return sampledCount;
    }

    @Override
    public int hashCode() {
        return this.inner.hashCode() * 31 * this.countProvider.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IntProviderFluidIngredient other)) {
            return false;
        }

        return this.inner.equals(other.inner) && intProviderEqual(this.countProvider, other.countProvider);
    }

    public static boolean intProviderEqual(IntProvider o1, IntProvider o2) {
        if (o1 == o2) return true;
        if (o1.getType() != o2.getType()) return false;
        return o1.getMinValue() == o2.getMinValue() && o1.getMaxValue() == o2.getMaxValue();
    }

    public static IntProviderFluidIngredient of(FluidIngredient inner, IntProvider provider) {
        return new IntProviderFluidIngredient(inner, provider);
    }

    public static IntProviderFluidIngredient of(FluidStack inner, int min, int max) {
        return IntProviderFluidIngredient.of(FluidIngredient.of(inner), UniformInt.of(min, max));
    }
}
