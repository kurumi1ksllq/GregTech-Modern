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

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "block_reach",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_CHESTPLATES)),
                CustomTags.ROBOT_ARMS, GTArmorModifiers.BLOCK_REACH);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "sneak_speed",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_LEGGINGS)),
                CustomTags.CONVEYOR_MODULES, GTArmorModifiers.SNEAK_SPEED);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "speed_attribute",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_BOOTS)),
                CustomTags.ELECTRIC_MOTORS, GTArmorModifiers.MOVEMENT_SPEED_ATTR);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "respiration",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_HELMETS)),
                CustomTags.ELECTRIC_PUMPS, GTArmorModifiers.AIR_SUPPLIER);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "autoeat",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_HELMETS)),
                CustomTags.ROBOT_ARMS, GTArmorModifiers.AUTO_EAT);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "swim_speed",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_BOOTS)),
                CustomTags.ELECTRIC_PUMPS, GTArmorModifiers.SWIM_SPEED);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "step_height",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_BOOTS)),
                CustomTags.ELECTRIC_PISTONS, GTArmorModifiers.STEP_HEIGHT);

        /*
         * FIXME
         * VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "electric_jetpack",
         * IntersectionIngredient.of(
         * Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
         * Ingredient.of(Tags.Items.ARMORS_CHESTPLATES)),
         * GTItems.ELECTRIC_JETPACK,
         * GTArmorModifiers.JETPACK);
         * 
         * VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "advanced_electric_jetpack",
         * IntersectionIngredient.of(
         * Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
         * Ingredient.of(Tags.Items.ARMORS_CHESTPLATES)),
         * GTItems.ELECTRIC_JETPACK_ADVANCED,
         * GTArmorModifiers.JETPACK);
         * 
         * VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "liquid_fuel_jetpack",
         * IntersectionIngredient.of(
         * Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
         * Ingredient.of(Tags.Items.ARMORS_CHESTPLATES)),
         * GTItems.LIQUID_FUEL_JETPACK,
         * GTArmorModifiers.JETPACK);
         * 
         *
         */
        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "battery_modifier",
                Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                CustomTags.BATTERIES, GTArmorModifiers.BATTERY);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "night_vision",
                IntersectionIngredient.of(
                        Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT),
                        Ingredient.of(Tags.Items.ARMORS_HELMETS)),
                GTItems.NIGHTVISION_GOGGLES, GTArmorModifiers.NIGHT_VISION);
    }
}
