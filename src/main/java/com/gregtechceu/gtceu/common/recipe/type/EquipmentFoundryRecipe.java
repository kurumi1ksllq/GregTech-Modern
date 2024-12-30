package com.gregtechceu.gtceu.common.recipe.type;

import com.gregtechceu.gtceu.api.item.armor.ArmorUtils;
import com.gregtechceu.gtceu.api.item.armor.modifier.ArmorModifier;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.ParametersAreNonnullByDefault;

@RequiredArgsConstructor
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EquipmentFoundryRecipe implements Recipe<RecipeWrapper> {

    @Getter
    private final ResourceLocation id;
    private final Ingredient equipment;
    private final Ingredient ingredient;
    private final ArmorModifier modifier;

    public boolean matches(RecipeWrapper container, Level level) {
        boolean foundItem = false, foundIngredient = false;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (equipment.test(stack)) {
                    if (foundItem) {
                        return false;
                    }
                    foundItem = true;
                } else if (ingredient.test(stack)) {
                    foundIngredient = true;
                }
            }
        }

        return foundItem && foundIngredient;
    }

    public ItemStack assemble(RecipeWrapper container, RegistryAccess registryAccess) {
        ItemStack result = ItemStack.EMPTY;
        boolean foundIngredient = false;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack stack = container.getItem(i);
            if (equipment.test(stack)) {
                if (!result.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                result = stack.copy();
            } else if (ingredient.test(stack)) {
                foundIngredient = true;
                break;
            }
        }

        if (!foundIngredient || result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ArmorUtils.addModifier(result, modifier);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return GTItems.QUANTUM_CHESTPLATE_ADVANCED.asStack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GTRecipeTypes.EQUIPMENT_FOUNDRY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return GTRecipeTypes.EQUIPMENT_FOUNDRY_RECIPES.get();
    }

    public static class Serializer implements RecipeSerializer<EquipmentFoundryRecipe> {

        public EquipmentFoundryRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            Ingredient equipment = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "equipment"), false);
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"), false);
            ArmorModifier modifier = ArmorModifier.MODIFIERS.get(
                    new ResourceLocation(GsonHelper.getAsString(json, "modifier")));

            return new EquipmentFoundryRecipe(recipeId, equipment, ingredient, modifier);
        }

        public EquipmentFoundryRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient equipment = Ingredient.fromNetwork(buffer);
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ArmorModifier modifier = ArmorModifier.MODIFIERS.get(buffer.readResourceLocation());

            return new EquipmentFoundryRecipe(recipeId, equipment, ingredient, modifier);
        }

        public void toNetwork(FriendlyByteBuf buffer, EquipmentFoundryRecipe recipe) {
            recipe.equipment.toNetwork(buffer);
            recipe.ingredient.toNetwork(buffer);
            buffer.writeResourceLocation(recipe.modifier.id());
        }
    }
}
