package com.gregtechceu.gtceu.data.recipe.generated;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.MarkerMaterials;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.util.entry.ItemEntry;
import it.unimi.dsi.fastutil.ints.Int2ReferenceArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;

public final class ToolRecipeHandler {

    public static final Int2ReferenceMap<ItemEntry<? extends Item>> powerUnitItems = new Int2ReferenceArrayMap<>(
            GTValues.tiersBetween(GTValues.LV, GTValues.IV),
            new ItemEntry[] { GTItems.POWER_UNIT_LV, GTItems.POWER_UNIT_MV, GTItems.POWER_UNIT_HV,
                    GTItems.POWER_UNIT_EV, GTItems.POWER_UNIT_IV });

    public static final Material[] softMaterials = new Material[] {
            GTMaterials.Wood, GTMaterials.Rubber, GTMaterials.Polyethylene,
            GTMaterials.Polytetrafluoroethylene, GTMaterials.Polybenzimidazole,
            GTMaterials.SiliconeRubber, GTMaterials.StyreneButadieneRubber
    };

    private ToolRecipeHandler() {}

    public static void run(@NotNull Consumer<FinishedRecipe> provider, @NotNull Material material) {
        if (material.getProperty(PropertyKey.TOOL) == null) {
            return;
        }

        processTool(provider, material);
    }

    private static void processTool(@NotNull Consumer<FinishedRecipe> provider, @NotNull Material material) {
        ItemStack stick = new ItemStack(Items.STICK);
        MaterialEntry ingot = new MaterialEntry(
                material.hasProperty(PropertyKey.GEM) ? TagPrefix.gem : TagPrefix.ingot, material);
        addToolRecipe(provider, material, GTToolType.MORTAR, false,
                " I ", "SIS", "SSS",
                'I', ingot,
                'S', new ItemStack(Blocks.STONE));

        if (!material.shouldGenerateRecipesFor(plate)) {
            return;
        }

        MaterialEntry plate = new MaterialEntry(TagPrefix.plate, material);

        if (material.hasFlag(GENERATE_PLATE)) {
            addToolRecipe(provider, material, GTToolType.SPADE, false,
                    "fPh", "PSP", " S ",
                    'P', plate,
                    'S', stick);

            addToolRecipe(provider, material, GTToolType.AXE, false,
                    "PIh", "PS ", "fS ",
                    'P', plate,
                    'I', ingot,
                    'S', stick);

            addToolRecipe(provider, material, GTToolType.PICKAXE, false,
                    "PII", "fSh", " S ",
                    'P', plate,
                    'I', ingot,
                    'S', stick);

            addToolRecipe(provider, material, GTToolType.SHOVEL, false,
                    "fPh", " S ", " S ",
                    'P', plate,
                    'S', stick);

            addToolRecipe(provider, material, GTToolType.HARD_HAMMER, true,
                    "II ", "IIS", "II ",
                    'I', ingot,
                    'S', stick);

            addToolRecipe(provider, material, GTToolType.FILE, true,
                    " P ", " P ", " S ",
                    'P', plate,
                    'S', stick);

            addToolRecipe(provider, material, GTToolType.KNIFE, false,
                    "fPh", " S ",
                    'P', plate,
                    'S', stick);

            addToolRecipe(provider, material, GTToolType.WRENCH, false,
                    "PhP", " P ", " P ",
                    'P', plate);

            addArmorRecipe(provider, material, ArmorItem.Type.HELMET,
                    "PPP", "PhP",
                    'P', plate);
            addArmorRecipe(provider, material, ArmorItem.Type.CHESTPLATE,
                    "PhP", "PPP", "PPP",
                    'P', plate);
            addArmorRecipe(provider, material, ArmorItem.Type.LEGGINGS,
                    "PPP", "PhP", "P P",
                    'P', plate);
            addArmorRecipe(provider, material, ArmorItem.Type.BOOTS,
                    "P P", "PhP",
                    'P', plate);
        } else {
            GTCEu.LOGGER.info(
                    "Did not find plate for {}, skipping mining hammer, spade, saw, axe, hoe, pickaxe, scythe, shovel, sword, hammer, file, knife, wrench recipes",
                    material.getName());
        }

        if (material.hasFlag(GENERATE_ROD)) {
            MaterialEntry rod = new MaterialEntry(TagPrefix.rod, material);

            if (material.hasFlag(GENERATE_PLATE)) {
                if (material.hasFlag(GENERATE_BOLT_SCREW)) {
                    addToolRecipe(provider, material, GTToolType.WIRE_CUTTER, false,
                            "PfP", "hPd", "STS",
                            'P', plate,
                            'T', new MaterialEntry(TagPrefix.screw, material),
                            'S', rod);
                } else if (!ArrayUtils.contains(softMaterials, material)) {
                    GTCEu.LOGGER
                            .info("Did not find bolt for {}, skipping wirecutter recipe", material.getName());
                }
            } else {
                GTCEu.LOGGER.info("Did not find plate for {}, skipping wirecutter, butchery knife recipes",
                        material.getName());
            }

            addToolRecipe(provider, material, GTToolType.SCREWDRIVER, true,
                    " fS", " Sh", "W  ",
                    'S', rod,
                    'W', stick);

            addDyeableToolRecipe(provider, material, GTToolType.CROWBAR, true,
                    "hDS", "DSD", "SDf",
                    'S', rod);
        } else if (!ArrayUtils.contains(softMaterials, material)) {
            GTCEu.LOGGER.warn("Did not find rod for " + material.getName() +
                    ", skipping wirecutter, butchery knife, screwdriver, crowbar recipes");
        }

        GTToolType.getTypes().forEach((s, gtToolType) -> addNetheriteToolRecipe(provider, gtToolType));
    }

    public static void addToolRecipe(@NotNull Consumer<FinishedRecipe> provider, @NotNull Material material,
                                     @NotNull GTToolType tool, boolean mirrored, Object... recipe) {
        ItemStack toolStack = ToolHelper.get(tool, material);
        if (toolStack.isEmpty()) return;
        if (mirrored) { // todo mirrored
            VanillaRecipeHelper.addShapedRecipe(provider, String.format("%s_%s", tool.name, material.getName()),
                    toolStack, recipe);
        } else {
            VanillaRecipeHelper.addShapedRecipe(provider, String.format("%s_%s", tool.name, material.getName()),
                    toolStack, recipe);
        }
    }

    public static void addNetheriteToolRecipe(@NotNull Consumer<FinishedRecipe> provider, @NotNull GTToolType tool) {
        VanillaRecipeHelper.addToolUpgradingRecipe(provider, tool, GTMaterials.Netherite, GTMaterials.Diamond,
                Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ChemicalHelper.get(ingot, GTMaterials.Netherite).getItem());
    }

    public static void addArmorRecipe(Consumer<FinishedRecipe> provider, @NotNull Material material,
                                      @NotNull ArmorItem.Type armor, Object... recipe) {
        ItemStack armorStack = ToolHelper.getArmor(armor, material);
        if (armorStack.isEmpty()) return;
        VanillaRecipeHelper.addShapedRecipe(provider, String.format("%s_%s", armor.getName(), material.getName()),
                armorStack, recipe);
    }

    /**
     * {@code D} is inferred as the dye key
     */
    public static void addDyeableToolRecipe(@NotNull Consumer<FinishedRecipe> provider, @NotNull Material material,
                                            @NotNull GTToolType tool, boolean mirrored, Object... recipe) {
        ItemStack toolStack = ToolHelper.get(tool, material);
        if (toolStack.isEmpty()) return;
        for (var color : MarkerMaterials.Color.COLORS.entrySet()) {
            ToolHelper.getToolTag(toolStack).putInt(ToolHelper.TINT_COLOR_KEY, color.getKey().getTextColor());
            Object[] recipeWithDye = ArrayUtils.addAll(recipe, 'D',
                    new MaterialEntry(TagPrefix.dye, color.getValue()));

            if (mirrored) { // todo mirrored
                VanillaRecipeHelper.addShapedRecipe(provider,
                        String.format("%s_%s_%s", tool.name, material.getName(), color.getKey().getSerializedName()),
                        toolStack, recipeWithDye);
            } else {
                VanillaRecipeHelper.addShapedRecipe(provider,
                        String.format("%s_%s_%s", tool.name, material.getName(), color.getKey().getSerializedName()),
                        toolStack, recipeWithDye);
            }
        }
    }
}
