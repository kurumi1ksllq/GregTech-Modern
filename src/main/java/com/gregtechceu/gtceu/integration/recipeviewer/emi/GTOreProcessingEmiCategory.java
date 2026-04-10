package com.gregtechceu.gtceu.integration.recipeviewer.emi;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.gregtechceu.gtceu.integration.recipeviewer.widgets.GTOreByProductWidget;
import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey.ORE;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;
import static com.gregtechceu.gtceu.integration.recipeviewer.emi.recipe.GTRecipeEMICategory.sortDefinition;

public class GTOreProcessingEmiCategory extends EmiRecipeCategory {

    public static final GTOreProcessingEmiCategory CATEGORY = new GTOreProcessingEmiCategory();

    public GTOreProcessingEmiCategory() {
        super(GTCEu.id("ore_processing_diagram"), EmiStack.of(Items.RAW_IRON));
    }

    public static void registerDisplays(EmiRegistry registry) {
        for (Material mat : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (mat.hasProperty(ORE) && !mat.hasFlag(MaterialFlags.NO_ORE_PROCESSING_TAB)) {
                registry.addRecipe(new GTEmiOreProcessingWrapper(mat));
            }
        }
    }

    public static void registerWorkStations(EmiRegistry registry) {
        List<MachineDefinition> registeredMachines = new ArrayList<>();
        GTRecipeType[] validTypes = new GTRecipeType[] {
                MACERATOR_RECIPES, ORE_WASHER_RECIPES, THERMAL_CENTRIFUGE_RECIPES, CENTRIFUGE_RECIPES,
                CHEMICAL_BATH_RECIPES, ELECTROMAGNETIC_SEPARATOR_RECIPES, SIFTER_RECIPES
        };
        for (MachineDefinition machine : GTRegistries.MACHINES.values()
                .stream()
                .sorted(sortDefinition)
                .toList()) {
            for (GTRecipeType type : machine.getRecipeTypes()) {
                for (GTRecipeType validType : validTypes) {
                    if (type == validType && !registeredMachines.contains(machine)) {
                        registry.addWorkstation(CATEGORY, EmiStack.of(machine.asStack()));
                        registeredMachines.add(machine);
                    }
                }
            }
        }
    }

    @Override
    public Component getName() {
        return Component.translatable("gtceu.jei.ore_processing_diagram");
    }

    public static class GTEmiOreProcessingWrapper extends ModularEmiRecipe<GTOreByProductWidget> {

        final Material material;

        public GTEmiOreProcessingWrapper(Material material) {
            super(() -> new GTOreByProductWidget(material));
            this.material = material;
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return CATEGORY;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return material.getResourceLocation();
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }
    }
}
