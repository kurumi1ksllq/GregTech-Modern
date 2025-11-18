package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.data.tag.GTIngredientTypes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import com.google.common.base.Preconditions;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderFluidIngredient.intProviderEqual;

public class IntProviderIngredient implements ICustomIngredient {

    public static final MapCodec<IntProviderIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("inner").forGetter(IntProviderIngredient::getInner),
            IntProvider.CODEC.fieldOf("count_provider").forGetter(IntProviderIngredient::getCountProvider))
            .apply(instance, IntProviderIngredient::new));
    public static final ResourceLocation TYPE = GTCEu.id("int_provider");
    public static final ItemStack[] EMPTY_STACK_ARRAY = new ItemStack[0];

    @Getter
    protected final IntProvider countProvider;
    @Setter
    protected int sampledCount = -1;
    @Getter
    protected final Ingredient inner;
    @Setter
    protected @NotNull ItemStack [] itemStacks = null;

    protected IntProviderIngredient(Ingredient inner, IntProvider countProvider) {
        this.inner = inner;
        this.countProvider = countProvider;
    }

    public static Ingredient of(Ingredient inner, IntProvider countProvider) {
        Preconditions.checkArgument(countProvider.getMinValue() >= 0,
                "IntProviderIngredient must have a min value of at least 0.");
        return new IntProviderIngredient(inner, countProvider).toVanilla();
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return inner.test(stack);
    }

    public ItemStack[] getItemStacks() {
        if (itemStacks == null) {
            int cachedCount = getSampledCount(GTValues.RNG);
            if (cachedCount == 0) {
                return EMPTY_STACK_ARRAY;
            }
            var innerStacks = inner.getItems();
            this.itemStacks = new ItemStack[innerStacks.length];
            for (int i = 0; i < itemStacks.length; i++) {
                itemStacks[i] = innerStacks[i].copyWithCount(cachedCount);
            }
        }
        return itemStacks;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Arrays.stream(getItemStacks());
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public int hashCode() {
        return this.inner.hashCode() * 31 * this.countProvider.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IntProviderIngredient other)) {
            return false;
        }

        return this.inner.equals(other.inner) && intProviderEqual(this.countProvider, other.countProvider);
    }

    @Override
    public IngredientType<?> getType() {
        return GTIngredientTypes.INT_PROVIDER_INGREDIENT.get();
    }

    public @NotNull ItemStack getMaxSizeStack() {
        if (inner.getItems().length == 0) return ItemStack.EMPTY;
        else return inner.getItems()[0].copyWithCount(countProvider.getMaxValue());
    }

    public int getSampledCount(@NotNull RandomSource random) {
        if (sampledCount == -1) {
            sampledCount = countProvider.sample(random);
        }
        return sampledCount;
    }
}
