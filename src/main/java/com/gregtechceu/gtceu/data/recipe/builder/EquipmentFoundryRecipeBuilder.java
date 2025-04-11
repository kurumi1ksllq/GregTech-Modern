package com.gregtechceu.gtceu.data.recipe.builder;

import com.gregtechceu.gtceu.api.item.armor.modifier.ArmorModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.NBTIngredient;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

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
import lombok.experimental.Tolerate;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@Accessors(chain = true, fluent = true)
public class EquipmentFoundryRecipeBuilder {

    @Setter
    private ResourceLocation id;
    @Setter
    private Ingredient equipment = Ingredient.of(CustomTags.MODIFIABLE_EQUIPMENT);
    @Setter
    private Ingredient ingredient;
    @Setter
    private ArmorModifier modifier;

    public EquipmentFoundryRecipeBuilder(@Nullable ResourceLocation id) {
        this.id = id;
    }

    @Tolerate
    public EquipmentFoundryRecipeBuilder ingredient(TagKey<Item> itemStack) {
        return ingredient(Ingredient.of(itemStack));
    }

    @Tolerate
    public EquipmentFoundryRecipeBuilder ingredient(ItemStack itemStack) {
        if (itemStack.hasTag()) {
            ingredient = NBTIngredient.createNBTIngredient(itemStack);
        } else {
            ingredient = Ingredient.of(itemStack);
        }
        return this;
    }

    @Tolerate
    public EquipmentFoundryRecipeBuilder ingredient(ItemLike itemLike) {
        return ingredient(Ingredient.of(itemLike));
    }

    protected ResourceLocation defaultId() {
        return modifier.id();
    }

    public void toJson(JsonObject json) {
        json.add("equipment", equipment.toJson());
        json.add("ingredient", ingredient.toJson());

        json.addProperty("modifier", modifier.id().toString());
    }

    public void save(Consumer<FinishedRecipe> consumer) {
        consumer.accept(new FinishedRecipe() {

            @Override
            public void serializeRecipeData(JsonObject pJson) {
                toJson(pJson);
            }

            @Override
            public ResourceLocation getId() {
                var _id = id == null ? defaultId() : id;
                return _id.withPrefix("equipment_foundry/");
            }

            @Override
            public RecipeSerializer<?> getType() {
                return GTRecipeTypes.EQUIPMENT_FOUNDRY_SERIALIZER.get();
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
