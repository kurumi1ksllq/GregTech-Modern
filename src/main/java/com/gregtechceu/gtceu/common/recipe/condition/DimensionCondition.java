package com.gregtechceu.gtceu.common.recipe.condition;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.DimensionMarker;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.api.ui.component.SlotComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.Insets;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;
import com.gregtechceu.gtceu.common.data.GTRecipeConditions;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
public class DimensionCondition extends RecipeCondition {

    public static final Codec<DimensionCondition> CODEC = RecordCodecBuilder
            .create(instance -> RecipeCondition.isReverse(instance)
                    .and(ResourceLocation.CODEC.fieldOf("dimension").forGetter(val -> val.dimension))
                    .apply(instance, DimensionCondition::new));

    public final static DimensionCondition INSTANCE = new DimensionCondition();
    private ResourceLocation dimension = new ResourceLocation("dummy");

    public DimensionCondition(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    public DimensionCondition(boolean isReverse, ResourceLocation dimension) {
        super(isReverse);
        this.dimension = dimension;
    }

    @Override
    public RecipeConditionType<?> getType() {
        return GTRecipeConditions.DIMENSION;
    }

    @Override
    public boolean isOr() {
        return true;
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.dimension.tooltip", dimension);
    }

    public StackLayout setupDimensionMarkers() {
        StackLayout layout = UIContainers.stack(Sizing.fixed(18), Sizing.fixed(18));
        layout.positioning(Positioning.relative(100, 100));
        layout.margins(Insets.of(0, 32, 0, 44));

        DimensionMarker dimMarker = GTRegistries.DIMENSION_MARKERS.getOrDefault(this.dimension,
                new DimensionMarker(DimensionMarker.MAX_TIER, () -> Blocks.BARRIER, this.dimension.toString()));
        ItemStack icon = dimMarker.getIcon();
        CustomItemStackHandler handler = new CustomItemStackHandler(1);
        // TODO change to item component (?)
        SlotComponent dimSlot = UIComponents.slot(handler, 0)
                .canInsert(false)
                .canExtract(false)
                .ingredientIO(IO.IN);
        dimSlot.positioning(Positioning.relative(100, 100));
        handler.setStackInSlot(0, icon);

        layout.child(dimSlot);
        if (ConfigHolder.INSTANCE.compat.showDimensionTier) {
            layout.child(UIComponents.texture(UITextures.text(
                    Component.literal("T" + (dimMarker.tier >= DimensionMarker.MAX_TIER ? "?" : dimMarker.tier)))
                    .scale(0.75f).transform(-3.0f, 5.0f)));
        }
        return layout;
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    @Override
    public boolean test(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        Level level = recipeLogic.machine.self().getLevel();
        return level != null && dimension.equals(level.dimension().location());
    }

    @Override
    public RecipeCondition createTemplate() {
        return new DimensionCondition();
    }

    @NotNull
    @Override
    public JsonObject serialize() {
        JsonObject config = super.serialize();
        config.addProperty("dimension", dimension.toString());
        return config;
    }

    @Override
    public RecipeCondition deserialize(@NotNull JsonObject config) {
        super.deserialize(config);
        dimension = new ResourceLocation(
                GsonHelper.getAsString(config, "dimension", "dummy"));
        return this;
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        dimension = new ResourceLocation(buf.readUtf());
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        buf.writeUtf(dimension.toString());
    }
}
