package com.gregtechceu.gtceu.api.recipe.lookup;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.lookup.ingredient.AbstractMapIngredient;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class RecipeIterator implements Iterator<GTRecipe> {

    private final @NotNull Branch rootBranch;
    private final @NotNull List<List<AbstractMapIngredient>> ingredients;
    private final @NotNull Predicate<GTRecipe> predicate;

    private final Deque<SearchFrame> stack = new ArrayDeque<>();

    public RecipeIterator(@NotNull GTRecipeType type,
                          @NotNull List<List<AbstractMapIngredient>> ingredients,
                          @NotNull Predicate<GTRecipe> predicate) {
        this(type.getLookup(), ingredients, predicate);
    }

    public RecipeIterator(@NotNull GTRecipeLookup lookup,
                          @NotNull List<List<AbstractMapIngredient>> ingredients,
                          @NotNull Predicate<GTRecipe> predicate) {
        this(lookup.getLookup(), ingredients, predicate);
    }

    public RecipeIterator(@NotNull Branch rootBranch,
                          @NotNull List<List<AbstractMapIngredient>> ingredients,
                          @NotNull Predicate<GTRecipe> predicate) {
        this.rootBranch = rootBranch;
        this.ingredients = ingredients;
        this.predicate = predicate;

        for (int i = ingredients.size() - 1; i >= 0; i--) {
            stack.push(new SearchFrame(i, rootBranch));
        }
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public GTRecipe next() {
        while (!stack.isEmpty()) {
            // We stay on one frame until all ingredients have been checked
            SearchFrame frame = stack.peek();

            if (frame.ingredientIndex >= ingredients.get(frame.index).size()) {
                stack.pop();
                continue;
            }

            List<AbstractMapIngredient> ingredientList = ingredients.get(frame.index);
            AbstractMapIngredient ingredient = ingredientList.get(frame.ingredientIndex);
            // Increment candidate pos for next iteration
            frame.ingredientIndex++;
            var nodes = GTRecipeLookup.determineRootNodes(ingredient, frame.branch);
            var result = nodes.get(ingredient);
            if (result == null) {
                continue;
            }

            // Option 1: It's a recipe
            GTRecipe recipe = result.map(
                    r -> predicate.test(r) ? r : null,
                    b -> null);
            if (recipe != null) {
                return recipe;
            }

            // Option 2: It's a branch, dive deeper
            result.ifRight(b -> {
                for (int j = ingredients.size() - 1; j >= 0; j--) {
                    stack.push(new SearchFrame(j, b));
                }
            });
        }

        return null; // no more recipes
    }

    public void reset() {
        stack.clear();
        for (int i = ingredients.size() - 1; i >= 0; i--) {
            stack.push(new SearchFrame(i, rootBranch));
        }
    }

    private static class SearchFrame {

        int index;           // ingredient slot we’re exploring
        int ingredientIndex; // position within ingredients[index]
        Branch branch;       // branch in the recipe DB

        public SearchFrame(int index, Branch branch) {
            this.index = index;
            this.ingredientIndex = 0;
            this.branch = branch;
        }
    }
}
