package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.tag.GTIngredientTypes;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
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

/**
 * Allows a {@link FluidIngredient} to be created with a ranged {@code amount}, which will be randomly rolled upon
 * recipe start (input) / completion (output).
 * Instantiated using {@link IntProviderFluidIngredient#of()}, with a {@link FluidIngredient}
 * and either an {@link IntProvider} or {@code int, int} range bounds (inclusive).
 * Functions similarly to {@link IntProviderIngredient}.
 */
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
    /**
     * The last result of {@link IntProviderFluidIngredient#getSampledCount()}. -1 if not rolled.
     */
    @Setter
    protected int sampledCount = -1;
    /**
     * The {@link FluidIngredient} to have a ranged amount.
     */
    @Getter
    private final FluidIngredient inner;
    @Setter
    protected @Nullable FluidStack @NotNull[] fluidStacks = null;

    public static final FluidStack[] EMPTY_STACK_ARRAY = new FluidStack[0];

    protected IntProviderFluidIngredient(FluidIngredient inner, IntProvider provider) {
        this.inner = inner;
        this.countProvider = provider;
    }

    protected IntProviderFluidIngredient(FluidIngredient inner, IntProvider provider, int sampledCount) {
        this.inner = inner;
        this.countProvider = provider;
        this.sampledCount = sampledCount;
    }

    public IntProviderFluidIngredient copy() {
        IntProviderFluidIngredient copied = new IntProviderFluidIngredient(this.inner, this.countProvider);
        copied.setSampledCount(this.sampledCount);
        copied.setFluidStacks(this.fluidStacks);
        return copied;
    }

    /**
     * An {@link IntProviderFluidIngredient} does not have an amount.
     * You probably want either {@link IntProviderFluidIngredient#getStacks()} or
     * {@link IntProviderFluidIngredient#getMaxSizeStack()}.
     */
    @Deprecated
    public int getAmount() {
        if (ConfigHolder.INSTANCE.dev.debug) {
            throw new IllegalCallerException("An IPFI should never have getAmount() called on it!");
        }
        return -1;
    }

    /**
     * Gets a usable {@link FluidStack FluidStack[]} from this {@link IntProviderFluidIngredient}.
     * If this ingredient has not yet had its {@link IntProviderFluidIngredient#sampledCount} rolled, rolls it.
     *
     * @return a {@link FluidStack FluidStack[]} with amount {@link IntProviderFluidIngredient#sampledCount}
     */
    @Override
    public Stream<FluidStack> generateStacks() {
        if (fluidStacks == null) {
            int cachedAmount = getSampledCount(GTValues.RNG);
            if (cachedAmount == 0) {
                return Stream.of(EMPTY_STACK_ARRAY);
            }
            var innerStacks = inner.getStacks();
            this.fluidStacks = new FluidStack[innerStacks.length];
            for (int i = 0; i < fluidStacks.length; i++) {
                fluidStacks[i] = innerStacks[i].copy();
                fluidStacks[i].setAmount(cachedAmount);
            }
        }
        return Stream.of(fluidStacks);
    }

    @Override
    public boolean test(@NotNull FluidStack stack) {
        return inner.test(stack);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public FluidIngredientType<?> getType() {
        return GTIngredientTypes.INT_PROVIDER_FLUID_INGREDIENT.get();
    }

    /**
     * Gets a {@link FluidStack} containing the maximum possible output from this {@link IntProviderFluidIngredient}.
     * Mainly used for things like Recipe provider simulations to see if there is enough tank space to handle
     * the recipe output.
     *
     * @return a {@link FluidStack} with amount {@link IntProvider#getMaxValue()}
     */
    public @NotNull FluidStack getMaxSizeStack() {
        FluidStack[] in = inner.getStacks();
        if (in.length == 0) return FluidStack.EMPTY;
        return in[0].copyWithAmount(countProvider.getMaxValue());
    }

    /**
     * If this ingredient has not yet had its {@link IntProviderFluidIngredient#sampledCount} rolled, rolls it and
     * returns the roll.
     * If it has, returns the existing roll.
     * Passthrough method, invokes {@link IntProviderFluidIngredient#getSampledCount(RandomSource)} using the threadsafe
     * {@link GTValues#RNG}.
     *
     * @return the amount rolled
     */
    public int getSampledCount() {
        return getSampledCount(GTValues.RNG);
    }

    /**
     * If this ingredient has not yet had its {@link IntProviderFluidIngredient#sampledCount} rolled, rolls it and
     * returns the roll.
     * If it has, returns the existing roll.
     *
     * @param random {@link RandomSource}, must be threadsafe, usually called using {@link GTValues#RNG}.
     * @return the amount rolled
     */
    public int getSampledCount(@NotNull RandomSource random) {
        if (sampledCount == -1) {
            sampledCount = countProvider.sample(random);
        }
        return sampledCount;
    }

    /**
     * @return the average roll of this ranged amount
     */
    public double getMidRoll() {
        return ((countProvider.getMaxValue() + countProvider.getMinValue()) / 2.0);
    }

    @Override
    public int hashCode() {
        return this.inner.hashCode();// * 31 * this.countProvider.hashCode();
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

    /**
     * Resets the random roll on this ingredient
     */
    public void reroll() {
        sampledCount = -1;
        fluidStacks = null;
    }

    /**
     * @param inner    {@link FluidIngredient}
     * @param provider usually as {@link UniformInt#of(int, int)}
     */
    public static IntProviderFluidIngredient of(FluidIngredient inner, IntProvider provider) {
        return new IntProviderFluidIngredient(inner, provider);
    }

    public static IntProviderFluidIngredient of(FluidStack inner, int min, int max) {
        return IntProviderFluidIngredient.of(FluidIngredient.of(inner), UniformInt.of(min, max));
    }
}
