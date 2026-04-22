package com.gregtechceu.gtceu.integration.recipeviewer.emi.orevein;

import brachy.modularui.integration.emi.recipe.ModularUIEmiRecipe;
import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.client.ClientProxy;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.gregtechceu.gtceu.integration.recipeviewer.widgets.GTOreVeinWidget;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GTOreVeinEmiCategory extends EmiRecipeCategory {

    public static final GTOreVeinEmiCategory CATEGORY = new GTOreVeinEmiCategory();

    public GTOreVeinEmiCategory() {
        super(GTCEu.id("ore_vein_diagram"), EmiStack.of(Items.RAW_IRON));
    }

    public static void registerDisplays(EmiRegistry registry) {
        for (GTOreDefinition oreDefinition : ClientProxy.CLIENT_ORE_VEINS.values()) {
            registry.addRecipe(new GTEmiOreVein(oreDefinition));
        }
    }

    public static void registerWorkStations(EmiRegistry registry) {
        registry.addWorkstation(CATEGORY, EmiStack.of(GTItems.PROSPECTOR_LV.asStack()));
        registry.addWorkstation(CATEGORY, EmiStack.of(GTItems.PROSPECTOR_HV.asStack()));
        registry.addWorkstation(CATEGORY, EmiStack.of(GTItems.PROSPECTOR_LuV.asStack()));
    }

    @Override
    public Component getName() {
        return Component.translatable("gtceu.jei.ore_vein_diagram");
    }

    public static class GTEmiOreVein extends ModularUIEmiRecipe {

        private final GTOreDefinition oreDefinition;

        public GTEmiOreVein(GTOreDefinition oreDefinition) {
            super(ClientProxy.CLIENT_ORE_VEINS.inverse().get(oreDefinition).withPrefix("/ore_vein_diagram/"), () -> new GTOreVeinWidget(oreDefinition));
            this.oreDefinition = oreDefinition;
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return GTOreVeinEmiCategory.CATEGORY;
        }

        @Override
        public @NotNull List<EmiStack> getOutputs() {
            return GTOreVeinWidget.getContainedOresAndBlocks(oreDefinition)
                    .stream()
                    .map(EmiStack::of)
                    .toList();
        }
    }
}
