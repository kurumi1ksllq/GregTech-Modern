package com.gregtechceu.gtceu.common.recipe.type;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.armor.ArmorUtils;
import com.gregtechceu.gtceu.api.item.module.AppliedItemModule;
import com.gregtechceu.gtceu.api.item.module.ItemModule;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;

import com.google.gson.JsonArray;
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
    private final ItemModule[] modifier;

    private ItemModule getModule(ItemStack ingredient) {
        int tier = GTUtil.getTier(ingredient.getItem());
        return modifier[Mth.clamp(tier - GTValues.LV, 0, modifier.length)];
    }

    public boolean matches(RecipeWrapper container, Level level) {
        ItemStack foundItem = null, foundIngredient = null;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (equipment.test(stack)) {
                    if (foundItem != null) {
                        return false;
                    }
                    foundItem = stack;
                } else if (ingredient.test(stack)) {
                    foundIngredient = stack;
                }
            }
        }
        if (foundIngredient == null || foundItem == null) return false;

        if (GTUtil.getTier(foundIngredient.getItem()) != -1) {
            if (GTUtil.getTier(foundIngredient.getItem()) > ArmorUtils.getMaxModuleTier(foundItem)) return false;
        }
        ItemModule module = getModule(foundIngredient);

        return AppliedItemModule.getModule(foundItem, module) == null && module.canApplyTo(foundItem);
    }

    public ItemStack assemble(RecipeWrapper container, RegistryAccess registryAccess) {
        ItemStack result = ItemStack.EMPTY;
        ItemStack foundIngredient = null;

        for (int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack stack = container.getItem(i);
            if (equipment.test(stack)) {
                if (!result.isEmpty()) {
                    return ItemStack.EMPTY;
                }

                result = stack.copy();
            } else if (ingredient.test(stack)) {
                foundIngredient = stack;
                break;
            }
        }

        if (foundIngredient == null || result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemModule module = getModule(foundIngredient);
        if (AppliedItemModule.getModule(result, module) != null) return ItemStack.EMPTY;
        if (!module.canApplyTo(result)) return ItemStack.EMPTY;
        AppliedItemModule.attach(result, module);
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
            JsonArray arr = json.getAsJsonArray("modifier");
            ItemModule[] modifier = new ItemModule[arr.size()];
            for (int i = 0; i < arr.size(); i++)
                modifier[i] = ItemModule.getModuleById(new ResourceLocation(arr.get(i).getAsString()));

            return new EquipmentFoundryRecipe(recipeId, equipment, ingredient, modifier);
        }

        public EquipmentFoundryRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            Ingredient equipment = Ingredient.fromNetwork(buffer);
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int length = buffer.readInt();
            ItemModule[] modifier = new ItemModule[length];
            for (int i = 0; i < length; i++) modifier[i] = ItemModule.getModuleById(buffer.readResourceLocation());
            return new EquipmentFoundryRecipe(recipeId, equipment, ingredient, modifier);
        }

        public void toNetwork(FriendlyByteBuf buffer, EquipmentFoundryRecipe recipe) {
            recipe.equipment.toNetwork(buffer);
            recipe.ingredient.toNetwork(buffer);
            buffer.writeInt(recipe.modifier.length);
            for (ItemModule module : recipe.modifier) buffer.writeResourceLocation(module.getId());
        }
    }
}
