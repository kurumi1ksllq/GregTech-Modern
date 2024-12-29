package com.gregtechceu.gtceu.data.recipe.builder;

import com.gregtechceu.gtceu.api.item.armor.modifier.ArmorModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.NBTIngredient;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

import com.google.gson.JsonObject;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@Accessors(chain = true, fluent = true)
public class EquipmentFoundryRecipeBuilder {

    @Setter
    private ResourceLocation id;
    @Setter
    private Ingredient ingredient;
    @Setter
    private ArmorModifier modifier;

    public EquipmentFoundryRecipeBuilder(@Nullable ResourceLocation id) {
        this.id = id;
    }

    public EquipmentFoundryRecipeBuilder input(TagKey<Item> itemStack) {
        return input(Ingredient.of(itemStack));
    }

    public EquipmentFoundryRecipeBuilder input(ItemStack itemStack) {
        if (itemStack.hasTag()) {
            ingredient = NBTIngredient.createNBTIngredient(itemStack);
        } else {
            ingredient = Ingredient.of(itemStack);
        }
        return this;
    }

    public EquipmentFoundryRecipeBuilder input(ItemLike itemLike) {
        return input(Ingredient.of(itemLike));
    }

    public EquipmentFoundryRecipeBuilder input(Ingredient ingredient) {
        this.ingredient = ingredient;
        return this;
    }

    protected ResourceLocation defaultId() {
        return modifier.id;
    }

    public void toJson(JsonObject json) {
        if (!ingredient.isEmpty()) {
            json.add("ingredient", ingredient.toJson());
        }

        if (modifier != null) {
            json.addProperty("modifier", modifier.id.toString());
        }
    }

    public void save(Consumer<FinishedRecipe> consumer) {
        consumer.accept(new FinishedRecipe() {

            @Override
            public void serializeRecipeData(JsonObject pJson) {
                toJson(pJson);
            }

            @Override
            public ResourceLocation getId() {
                var ID = id == null ? defaultId() : id;
                return new ResourceLocation(ID.getNamespace(), "equipment_foundry" + "/" + ID.getPath());
            }

            @Override
            public RecipeSerializer<?> getType() {
                return RecipeSerializer.SMOKING_RECIPE;
            }

            @Nullable
            @Override
            public JsonObject serializeAdvancement() {
                return null;
            }

            @Nullable
            @Override
            public ResourceLocation getAdvancementId() {
                return null;
            }
        });
    }
}
