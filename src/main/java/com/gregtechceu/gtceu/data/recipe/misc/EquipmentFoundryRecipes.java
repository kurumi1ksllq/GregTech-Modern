package com.gregtechceu.gtceu.data.recipe.misc;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.common.data.GTArmorModifiers;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;

public class EquipmentFoundryRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "speed",
                Ingredient.of(Tags.Items.ARMORS_LEGGINGS),
                CustomTags.ELECTRIC_MOTORS, GTArmorModifiers.SPEED);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "energy_shield",
                Ingredient.of(Tags.Items.ARMORS_CHESTPLATES),
                CustomTags.FIELD_GENERATORS, GTArmorModifiers.DAMAGE_BLOCK);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "attack_speed",
                Ingredient.of(Tags.Items.ARMORS_CHESTPLATES),
                CustomTags.ELECTRIC_MOTORS, GTArmorModifiers.ATTACK_SPEED);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "attack_damage",
                Ingredient.of(Tags.Items.ARMORS_CHESTPLATES),
                CustomTags.ELECTRIC_PISTONS, GTArmorModifiers.ATTACK_DAMAGE);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "block_reach",
                Ingredient.of(Tags.Items.ARMORS_CHESTPLATES),
                CustomTags.ROBOT_ARMS, GTArmorModifiers.BLOCK_REACH);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "sneak_speed",
                Ingredient.of(Tags.Items.ARMORS_LEGGINGS),
                CustomTags.CONVEYOR_MODULES, GTArmorModifiers.SNEAK_SPEED);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "speed_attribute",
                Ingredient.of(Tags.Items.ARMORS_BOOTS),
                CustomTags.ELECTRIC_MOTORS, GTArmorModifiers.MOVEMENT_SPEED_ATTR);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "respiration",
                Ingredient.of(Tags.Items.ARMORS_HELMETS),
                CustomTags.ELECTRIC_PUMPS, GTArmorModifiers.AIR_SUPPLIER);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "autoeat",
                Ingredient.of(Tags.Items.ARMORS_HELMETS),
                CustomTags.ROBOT_ARMS, GTArmorModifiers.AUTO_EAT);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "swim_speed",
                Ingredient.of(Tags.Items.ARMORS_BOOTS),
                CustomTags.ELECTRIC_PUMPS, GTArmorModifiers.SWIM_SPEED);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "step_height",
                Ingredient.of(Tags.Items.ARMORS_BOOTS),
                CustomTags.ELECTRIC_PISTONS, GTArmorModifiers.STEP_HEIGHT);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "jump_boost",
                Ingredient.of(Tags.Items.ARMORS_LEGGINGS),
                CustomTags.ELECTRIC_PISTONS, GTArmorModifiers.JUMP_BOOST);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "battery_modifier",
                Ingredient.EMPTY,
                CustomTags.BATTERIES, GTArmorModifiers.BATTERY);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "night_vision",
                Ingredient.of(Tags.Items.ARMORS_HELMETS),
                GTItems.NIGHTVISION_GOGGLES, GTArmorModifiers.NIGHT_VISION);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "ppe_helmet",
                Ingredient.of(Tags.Items.ARMORS_HELMETS),
                GTItems.MASK_FILTER,
                GTArmorModifiers.PPE);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "ppe_chestplate",
                Ingredient.of(Tags.Items.ARMORS_CHESTPLATES),
                ChemicalHelper.get(plate, PolyvinylChloride),
                GTArmorModifiers.PPE);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "ppe_leggings",
                Ingredient.of(Tags.Items.ARMORS_LEGGINGS),
                ChemicalHelper.get(plate, PolyvinylChloride),
                GTArmorModifiers.PPE);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "ppe_boots",
                Ingredient.of(Tags.Items.ARMORS_BOOTS),
                ChemicalHelper.get(plate, PolyvinylChloride),
                GTArmorModifiers.PPE);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "sensor",
                Ingredient.of(Tags.Items.ARMORS_HELMETS),
                CustomTags.SENSORS, GTArmorModifiers.SENSOR);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "wireless_charger",
                Ingredient.of(Tags.Items.ARMORS_CHESTPLATES),
                CustomTags.SENSORS, GTArmorModifiers.WIRELESS_CHARGER);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "jetpack",
                Ingredient.of(Tags.Items.ARMORS_CHESTPLATES),
                GTItems.ELECTRIC_JETPACK, GTArmorModifiers.JETPACK);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "advanced_jetpack",
                Ingredient.of(Tags.Items.ARMORS_CHESTPLATES),
                GTItems.ELECTRIC_JETPACK_ADVANCED, GTArmorModifiers.ADVANCED_JETPACK);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "liquid_fuel_jetpack",
                Ingredient.of(Tags.Items.ARMORS_CHESTPLATES),
                GTItems.LIQUID_FUEL_JETPACK, GTArmorModifiers.LIQUID_FUEL_JETPACK);

        VanillaRecipeHelper.addEquipmentFoundryRecipe(provider, "creative_flight",
                Ingredient.of(Tags.Items.ARMORS_CHESTPLATES),
                GTItems.CREATIVE_FLIGHT_MODULE, GTArmorModifiers.CREATIVE_FLIGHT);
    }
}
