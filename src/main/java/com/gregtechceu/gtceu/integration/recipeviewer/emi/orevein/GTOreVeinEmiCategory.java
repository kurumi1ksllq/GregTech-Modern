package com.gregtechceu.gtceu.integration.recipeviewer.emi.orevein;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.client.ClientProxy;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.GTOreVeinWidget;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.Nullable;

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

    public static class GTEmiOreVein extends ModularEmiRecipe<WidgetGroup> {

        private final GTOreDefinition oreDefinition;

        public GTEmiOreVein(GTOreDefinition oreDefinition) {
            super(() -> new GTOreVeinWidget(oreDefinition));
            this.oreDefinition = oreDefinition;
        }

        @Override
        public EmiRecipeCategory getCategory() {
            return GTOreVeinEmiCategory.CATEGORY;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return ClientProxy.CLIENT_ORE_VEINS.inverse().get(oreDefinition).withPrefix("/ore_vein_diagram/");
        }

        @Override
        public List<EmiStack> getOutputs() {
            return GTOreVeinWidget.getContainedOresAndBlocks(oreDefinition)
                    .stream()
                    .map(EmiStack::of)
                    .toList();
        }
    }
}
