package com.gregtechceu.gtceu.data.recipe.misc;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.common.data.GTArmorModifiers;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.IntersectionIngredient;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.plateDense;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;

public class EquipmentFoundryRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "speed",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_LEGGINGS)),
                CustomTags.ELECTRIC_MOTORS, GTArmorModifiers.SPEED);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "energy_shield",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_CHESTPLATES)),
                CustomTags.FIELD_GENERATORS, GTArmorModifiers.DAMAGE_BLOCK);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "attack_speed",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_CHESTPLATES)),
                CustomTags.ELECTRIC_MOTORS, GTArmorModifiers.ATTACK_SPEED);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "attack_damage",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_CHESTPLATES)),
                CustomTags.ELECTRIC_PISTONS, GTArmorModifiers.ATTACK_DAMAGE);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "defense_5",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_CHESTPLATES)),
                ChemicalHelper.get(plateDense, TungstenSteel), GTArmorModifiers.ARMOR_PLATE_TUNGSTENSTEEL);
    }
}
