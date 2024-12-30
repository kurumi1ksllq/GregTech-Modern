package com.gregtechceu.gtceu.data.recipe.misc;

import com.gregtechceu.gtceu.common.data.GTArmorModifiers;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.IntersectionIngredient;

import java.util.function.Consumer;

public class EquipmentFoundryRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "speed",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_LEGGINGS)),
                GTItems.ELECTRIC_PISTON_EV, GTArmorModifiers.SPEED);
    }
}
