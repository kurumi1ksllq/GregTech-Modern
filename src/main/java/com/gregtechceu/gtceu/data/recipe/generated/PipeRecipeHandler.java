package com.gregtechceu.gtceu.data.recipe.generated;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.pipelike.block.duct.DuctStructure;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.NO_SMASHING;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;

public final class PipeRecipeHandler {

    private PipeRecipeHandler() {}

    // spotless:off
    public static void run(@NotNull Consumer<FinishedRecipe> provider, @NotNull Material material) {
        processPipeTiny(provider, PropertyKey.PIPENET_PROPERTIES, pipeTiny, material);
        processPipeSmall(provider, PropertyKey.PIPENET_PROPERTIES, pipeSmall, material);
        processPipeNormal(provider, PropertyKey.PIPENET_PROPERTIES, pipeNormal, material);
        processPipeLarge(provider, PropertyKey.PIPENET_PROPERTIES, pipeLarge, material);
        processPipeHuge(provider, PropertyKey.PIPENET_PROPERTIES, pipeHuge, material);
        processPipeQuadruple(provider, PropertyKey.PIPENET_PROPERTIES, pipeQuadruple, material);
        processPipeNonuple(provider, PropertyKey.PIPENET_PROPERTIES, pipeNonuple, material);

        processRestrictivePipe(provider, PropertyKey.PIPENET_PROPERTIES, pipeSmallRestrictive, pipeSmall, material);
        processRestrictivePipe(provider, PropertyKey.PIPENET_PROPERTIES, pipeNormalRestrictive, pipeNormal, material);
        processRestrictivePipe(provider, PropertyKey.PIPENET_PROPERTIES, pipeLargeRestrictive, pipeLarge, material);
        processRestrictivePipe(provider, PropertyKey.PIPENET_PROPERTIES, pipeHugeRestrictive, pipeHuge, material);
        processRestrictivePipe(provider, PropertyKey.PIPENET_PROPERTIES, pipeQuadrupleRestrictive, pipeQuadruple, material);
        processRestrictivePipe(provider, PropertyKey.PIPENET_PROPERTIES, pipeNonupleRestrictive, pipeNonuple, material);

        addDuctRecipes(provider, Steel, 2);
        addDuctRecipes(provider, StainlessSteel, 4);
        addDuctRecipes(provider, TungstenSteel, 8);
    }
    // spotless:on

    private static void processRestrictivePipe(@NotNull Consumer<FinishedRecipe> provider,
                                               @NotNull PropertyKey<?> propertyKey,
                                               @NotNull TagPrefix prefix, @NotNull TagPrefix unrestrictive,
                                               @NotNull Material material) {
        if (!material.shouldGenerateRecipesFor(prefix) || !material.hasProperty(propertyKey)) {
            return;
        }

        ASSEMBLER_RECIPES.recipeBuilder("assemble_" + material.getName() + "_" + prefix.name)
                .inputItems(unrestrictive, material)
                .inputItems(ring, Iron, 2)
                .outputItems(prefix, material)
                .duration(20)
                .EUt(VA[ULV])
                .save(provider);

        VanillaRecipeHelper.addShapedRecipe(provider,
                FormattingUtil.toLowerCaseUnderscore(prefix + "_" + material.getName()),
                ChemicalHelper.get(prefix, material), "PR", "Rh",
                'P', new MaterialEntry(unrestrictive, material), 'R', ChemicalHelper.get(ring, Iron));
    }

    private static void processPipeTiny(@NotNull Consumer<FinishedRecipe> provider, @NotNull PropertyKey<?> propertyKey,
                                        @NotNull TagPrefix prefix, @NotNull Material material) {
        if (!material.shouldGenerateRecipesFor(prefix) || !material.hasProperty(propertyKey)) {
            return;
        }

        if (material.hasProperty(PropertyKey.WOOD)) return;
        ItemStack pipeStack = ChemicalHelper.get(prefix, material);
        EXTRUDER_RECIPES.recipeBuilder("extrude_" + material.getName() + "_tiny_pipe")
                .inputItems(ingot, material, 1)
                .notConsumable(GTItems.SHAPE_EXTRUDER_PIPE_TINY)
                .outputItems(pipeStack.copyWithCount(2))
                .duration((int) (material.getMass()))
                .EUt(6L * getVoltageMultiplier(material))
                .save(provider);

        if (material.hasFlag(NO_SMASHING)) {
            EXTRUDER_RECIPES.recipeBuilder("extrude_" + material.getName() + "_tiny_pipe_dust")
                    .inputItems(dust, material, 1)
                    .notConsumable(GTItems.SHAPE_EXTRUDER_PIPE_TINY)
                    .outputItems(pipeStack.copyWithCount(2))
                    .duration((int) (material.getMass()))
                    .EUt(6L * getVoltageMultiplier(material))
                    .save(provider);
        } else {
            VanillaRecipeHelper.addShapedRecipe(provider, String.format("tiny_%s_pipe", material.getName()),
                    pipeStack.copyWithCount(2), " s ", "hXw",
                    'X', new MaterialEntry(plate, material));
        }
    }

    private static void processPipeSmall(@NotNull Consumer<FinishedRecipe> provider,
                                         @NotNull PropertyKey<?> propertyKey,
                                         @NotNull TagPrefix prefix, @NotNull Material material) {
        if (!material.shouldGenerateRecipesFor(prefix) || !material.hasProperty(propertyKey)) {
            return;
        }

        if (material.hasProperty(PropertyKey.WOOD)) return;
        ItemStack pipeStack = ChemicalHelper.get(prefix, material);
        EXTRUDER_RECIPES.recipeBuilder("extrude_" + material.getName() + "_small_pipe")
                .inputItems(ingot, material, 1)
                .notConsumable(GTItems.SHAPE_EXTRUDER_PIPE_SMALL)
                .outputItems(pipeStack)
                .duration((int) (material.getMass()))
                .EUt(6L * getVoltageMultiplier(material))
                .save(provider);

        if (material.hasFlag(NO_SMASHING)) {
            EXTRUDER_RECIPES.recipeBuilder("extrude_" + material.getName() + "_small_pipe_dust")
                    .inputItems(dust, material, 1)
                    .notConsumable(GTItems.SHAPE_EXTRUDER_PIPE_SMALL)
                    .outputItems(pipeStack)
                    .duration((int) (material.getMass()))
                    .EUt(6L * getVoltageMultiplier(material))
                    .save(provider);
        } else {
            VanillaRecipeHelper.addShapedRecipe(provider, String.format("small_%s_pipe", material.getName()),
                    pipeStack, "wXh",
                    'X', new MaterialEntry(plate, material));
        }
    }

    private static void processPipeNormal(@NotNull Consumer<FinishedRecipe> provider,
                                          @NotNull PropertyKey<?> propertyKey,
                                          @NotNull TagPrefix prefix, @NotNull Material material) {
        if (!material.shouldGenerateRecipesFor(prefix) || !material.hasProperty(propertyKey)) {
            return;
        }

        if (material.hasProperty(PropertyKey.WOOD)) return;
        ItemStack pipeStack = ChemicalHelper.get(prefix, material);
        EXTRUDER_RECIPES.recipeBuilder("extrude_" + material.getName() + "_pipe")
                .inputItems(ingot, material, 3)
                .notConsumable(GTItems.SHAPE_EXTRUDER_PIPE_NORMAL)
                .outputItems(pipeStack)
                .duration((int) material.getMass() * 3)
                .EUt(6L * getVoltageMultiplier(material))
                .save(provider);

        if (material.hasFlag(NO_SMASHING)) {
            EXTRUDER_RECIPES.recipeBuilder("extrude_" + material.getName() + "_pipe_dust")
                    .inputItems(dust, material, 3)
                    .notConsumable(GTItems.SHAPE_EXTRUDER_PIPE_NORMAL)
                    .outputItems(pipeStack)
                    .duration((int) material.getMass() * 3)
                    .EUt(6L * getVoltageMultiplier(material))
                    .save(provider);
        } else {
            VanillaRecipeHelper.addShapedRecipe(provider, String.format("medium_%s_pipe", material.getName()),
                    pipeStack, "XXX", "w h",
                    'X', new MaterialEntry(plate, material));
        }
    }

    private static void processPipeLarge(@NotNull Consumer<FinishedRecipe> provider,
                                         @NotNull PropertyKey<?> propertyKey,
                                         @NotNull TagPrefix prefix, @NotNull Material material) {
        if (!material.shouldGenerateRecipesFor(prefix) || !material.hasProperty(propertyKey)) {
            return;
        }

        if (material.hasProperty(PropertyKey.WOOD)) return;
        ItemStack pipeStack = ChemicalHelper.get(prefix, material);
        EXTRUDER_RECIPES.recipeBuilder("extrude_" + material.getName() + "_large_pipe")
                .inputItems(ingot, material, 6)
                .notConsumable(GTItems.SHAPE_EXTRUDER_PIPE_LARGE)
                .outputItems(pipeStack)
                .duration((int) material.getMass() * 6)
                .EUt(6L * getVoltageMultiplier(material))
                .save(provider);

        if (material.hasFlag(NO_SMASHING)) {
            EXTRUDER_RECIPES.recipeBuilder("extrude_" + material.getName() + "_large_pipe_dust")
                    .inputItems(dust, material, 6)
                    .notConsumable(GTItems.SHAPE_EXTRUDER_PIPE_LARGE)
                    .outputItems(pipeStack)
                    .duration((int) material.getMass() * 6)
                    .EUt(6L * getVoltageMultiplier(material))
                    .save(provider);
        } else {
            VanillaRecipeHelper.addShapedRecipe(provider, String.format("large_%s_pipe", material.getName()),
                    pipeStack, "XXX", "w h", "XXX",
                    'X', new MaterialEntry(plate, material));
        }
    }

    private static void processPipeHuge(@NotNull Consumer<FinishedRecipe> provider, @NotNull PropertyKey<?> propertyKey,
                                        @NotNull TagPrefix prefix, @NotNull Material material) {
        if (!material.shouldGenerateRecipesFor(prefix) || !material.hasProperty(propertyKey)) {
            return;
        }

        if (material.hasProperty(PropertyKey.WOOD)) return;
        ItemStack pipeStack = ChemicalHelper.get(prefix, material);
        EXTRUDER_RECIPES.recipeBuilder("extrude_" + material.getName() + "_huge_pipe")
                .inputItems(ingot, material, 12)
                .notConsumable(GTItems.SHAPE_EXTRUDER_PIPE_HUGE)
                .outputItems(pipeStack)
                .duration((int) material.getMass() * 24)
                .EUt(6L * getVoltageMultiplier(material))
                .save(provider);

        if (material.hasFlag(NO_SMASHING)) {
            EXTRUDER_RECIPES.recipeBuilder("extrude_" + material.getName() + "_huge_pipe_dust")
                    .inputItems(dust, material, 12)
                    .notConsumable(GTItems.SHAPE_EXTRUDER_PIPE_HUGE)
                    .outputItems(pipeStack)
                    .duration((int) material.getMass() * 24)
                    .EUt(6L * getVoltageMultiplier(material))
                    .save(provider);
        } else if (plateDouble.doGenerateItem(material)) {
            VanillaRecipeHelper.addShapedRecipe(provider, String.format("huge_%s_pipe", material.getName()),
                    pipeStack, "XXX", "w h", "XXX",
                    'X', new MaterialEntry(plateDouble, material));
        }
    }

    private static void processPipeQuadruple(@NotNull Consumer<FinishedRecipe> provider,
                                             @NotNull PropertyKey<?> propertyKey,
                                             @NotNull TagPrefix prefix, @NotNull Material material) {
        if (!material.shouldGenerateRecipesFor(prefix) || !material.hasProperty(propertyKey)) {
            return;
        }

        if (material.hasProperty(PropertyKey.WOOD)) return;
        ItemStack smallPipe = ChemicalHelper.get(pipeSmall, material);
        ItemStack quadPipe = ChemicalHelper.get(prefix, material);
        VanillaRecipeHelper.addShapedRecipe(provider, String.format("quadruple_%s_pipe", material.getName()),
                quadPipe, "XX", "XX",
                'X', smallPipe);

        PACKER_RECIPES.recipeBuilder("package_" + material.getName() + "_quadruple_pipe")
                .inputItems(smallPipe.copyWithCount(4))
                .circuitMeta(4)
                .outputItems(quadPipe)
                .duration(30)
                .EUt(VA[ULV])
                .save(provider);
    }

    private static void processPipeNonuple(@NotNull Consumer<FinishedRecipe> provider,
                                           @NotNull PropertyKey<?> propertyKey,
                                           @NotNull TagPrefix prefix, @NotNull Material material) {
        if (!material.shouldGenerateRecipesFor(prefix) || !material.hasProperty(propertyKey)) {
            return;
        }

        if (material.hasProperty(PropertyKey.WOOD)) return;
        ItemStack smallPipe = ChemicalHelper.get(pipeSmall, material);
        ItemStack nonuplePipe = ChemicalHelper.get(prefix, material);
        VanillaRecipeHelper.addShapedRecipe(provider, String.format("nonuple_%s_pipe", material.getName()),
                nonuplePipe, "XXX", "XXX", "XXX",
                'X', smallPipe);

        PACKER_RECIPES.recipeBuilder("package_" + material.getName() + "_nonuple_pipe")
                .inputItems(smallPipe.copyWithCount(9))
                .circuitMeta(9)
                .outputItems(nonuplePipe)
                .duration(40)
                .EUt(VA[ULV])
                .save(provider);
    }

    private static void addDuctRecipes(Consumer<FinishedRecipe> provider, Material material, int outputAmount) {
        VanillaRecipeHelper.addShapedRecipe(provider, "small_duct_%s".formatted(material.getName()),
                GTBlocks.DUCT_PIPE_BLOCKS.get(DuctStructure.SMALL).asStack(outputAmount * 2),
                "w", "X", "h",
                'X', new MaterialEntry(plate, material));
        VanillaRecipeHelper.addShapedRecipe(provider, "medium_duct_%s".formatted(material.getName()),
                GTBlocks.DUCT_PIPE_BLOCKS.get(DuctStructure.NORMAL).asStack(outputAmount),
                " X ", "wXh", " X ",
                'X', new MaterialEntry(plate, material));
        VanillaRecipeHelper.addShapedRecipe(provider, "large_duct_%s".formatted(material.getName()),
                GTBlocks.DUCT_PIPE_BLOCKS.get(DuctStructure.LARGE).asStack(outputAmount),
                "XwX", "X X", "XhX",
                'X', new MaterialEntry(plate, material));
        VanillaRecipeHelper.addShapedRecipe(provider, "huge_duct_%s".formatted(material.getName()),
                GTBlocks.DUCT_PIPE_BLOCKS.get(DuctStructure.HUGE).asStack(outputAmount),
                "XwX", "X X", "XhX",
                'X', new MaterialEntry(plateDouble, material));
    }

    private static int getVoltageMultiplier(Material material) {
        return material.getBlastTemperature() >= 2800 ? VA[LV] : VA[ULV];
    }
}
