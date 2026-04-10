package com.gregtechceu.gtceu.integration.recipeviewer.rei;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.GTOreByProductWidget;

import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.rei.IGui2Renderer;
import com.lowdragmc.lowdraglib.rei.ModularDisplay;
import com.lowdragmc.lowdraglib.rei.ModularUIDisplayCategory;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import lombok.Getter;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey.ORE;
import static com.gregtechceu.gtceu.common.data.GTMachines.*;

public class GTOreProcessingDisplayCategory extends
                                            ModularUIDisplayCategory<GTOreProcessingDisplayCategory.GTOreProcessingDisplay> {

    public static final CategoryIdentifier<GTOreProcessingDisplay> CATEGORY = CategoryIdentifier
            .of(GTCEu.id("ore_processing_diagram"));
    @Getter
    private final Renderer icon;

    @Getter
    private final Size size;

    public GTOreProcessingDisplayCategory() {
        this.icon = IGui2Renderer.toDrawable(new ItemStackTexture(Items.RAW_IRON));
        this.size = new Size(176, 166);
    }

    @Override
    public CategoryIdentifier<? extends GTOreProcessingDisplay> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public int getDisplayHeight() {
        return getSize().height;
    }

    @Override
    public int getDisplayWidth(GTOreProcessingDisplay display) {
        return getSize().width;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.jei.ore_processing_diagram");
    }

    public static void registerDisplays(DisplayRegistry registry) {
        for (Material mat : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (mat.hasProperty(ORE) && !mat.hasFlag(MaterialFlags.NO_ORE_PROCESSING_TAB)) {
                registry.add(new GTOreProcessingDisplay(mat));
            }
        }
    }

    public static void registerWorkstations(CategoryRegistry registry) {
        registry.addWorkstations(GTOreProcessingDisplayCategory.CATEGORY,
                EntryStacks.of(MACERATOR[GTValues.LV].asStack()));
        registry.addWorkstations(GTOreProcessingDisplayCategory.CATEGORY,
                EntryStacks.of(ORE_WASHER[GTValues.LV].asStack()));
        registry.addWorkstations(GTOreProcessingDisplayCategory.CATEGORY,
                EntryStacks.of(THERMAL_CENTRIFUGE[GTValues.LV].asStack()));
        registry.addWorkstations(GTOreProcessingDisplayCategory.CATEGORY,
                EntryStacks.of(CENTRIFUGE[GTValues.LV].asStack()));
        registry.addWorkstations(GTOreProcessingDisplayCategory.CATEGORY,
                EntryStacks.of(CHEMICAL_BATH[GTValues.LV].asStack()));
        registry.addWorkstations(GTOreProcessingDisplayCategory.CATEGORY,
                EntryStacks.of(ELECTROMAGNETIC_SEPARATOR[GTValues.LV].asStack()));
        registry.addWorkstations(GTOreProcessingDisplayCategory.CATEGORY,
                EntryStacks.of(SIFTER[GTValues.LV].asStack()));
    }

    public static class GTOreProcessingDisplay extends ModularDisplay<WidgetGroup> {

        private final Material material;

        public GTOreProcessingDisplay(Material material) {
            super(() -> new GTOreByProductWidget(material), CATEGORY);
            this.material = material;
        }

        @Override
        public Optional<ResourceLocation> getDisplayLocation() {
            return Optional.of(material.getResourceLocation());
        }
    }
}
