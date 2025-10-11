package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ParallelHatchPartMachine;
import com.gregtechceu.gtceu.gametest.util.TestUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import lombok.Getter;

import java.util.Arrays;

/**
 * Test cases:
 * do many passes of most tests as a safeguard against bad rolls
 * 4 link logics
 * linked inputs
 * linked outputs
 * outputs linked to inputs
 * inputs linked to outputs (failure test)
 * outputs linked to both i/o
 * Forced rolls of 0 breaking recipes
 */
@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class IntProviderLinkedIngredientTest {

    private static GTRecipeType CR_RECIPE_TYPE;
    private static GTRecipeType LCR_RECIPE_TYPE;
    private static GTRecipeType CENTRIFUGE_RECIPE_TYPE;

    // items used in recipes. Up top here for quick replacements.
    private static final ItemStack CR_IN = new ItemStack(Items.ORANGE_STAINED_GLASS);
    private static final ItemStack CR_OUT = new ItemStack(Items.DEEPSLATE_BRICK_SLAB);
    private static final ItemStack LCR_IN = new ItemStack(Items.BLUE_STAINED_GLASS);
    private static final ItemStack LCR_OUT = new ItemStack(Items.DEEPSLATE_BRICK_STAIRS);
    private static final ItemStack LCENT_IN = new ItemStack(Items.RED_STAINED_GLASS);
    private static final ItemStack LCENT_OUT = new ItemStack(Items.DEEPSLATE_BRICK_WALL);
    private static final FluidStack CR_FIN = GTMaterials.Hydrogen.getFluid(1);
    private static final FluidStack CR_FOUT = GTMaterials.Iron.getFluid(1);
    private static final FluidStack LCR_FIN = GTMaterials.Oxygen.getFluid(1);
    private static final FluidStack LCR_FOUT = GTMaterials.Copper.getFluid(1);
    private static final FluidStack LCENT_FIN = GTMaterials.Nitrogen.getFluid(1);
    private static final FluidStack LCENT_FOUT = GTMaterials.Gold.getFluid(1);
    private static final FluidStack RUBBER = GTMaterials.Rubber.getFluid(1);
    private static final FluidStack REDSTONE = GTMaterials.Redstone.getFluid(1);
    private static final ItemStack COBBLE = new ItemStack(Items.COBBLESTONE);
    private static final ItemStack STONE = new ItemStack(Items.STONE);

    /**
     * How many times to repeat the Batch and Parallel random roll tests to avoid false positives
     * Currently set to 7, with singleblock recipes processing up to 9 items, allowing for stacks of up to 63 items.
     */
    @Getter
    private static final int REPLICAS = 7;

    @BeforeBatch(batch = "LinkedIngredients")
    public static void prepare(ServerLevel level) {
        CR_RECIPE_TYPE = TestUtils.createRecipeType("linked_ingredient_cr_tests", GTRecipeTypes.CHEMICAL_RECIPES);
        LCR_RECIPE_TYPE = TestUtils.createRecipeType("linked_ingredient_lcr_tests",
                GTRecipeTypes.LARGE_CHEMICAL_RECIPES);
        CENTRIFUGE_RECIPE_TYPE = TestUtils.createRecipeType("linked_ingredient_centrifuge_tests",
                GTRecipeTypes.CENTRIFUGE_RECIPES);

        // orange determines the count of CR_OUT
        CR_RECIPE_TYPE.getLookup().addRecipe(CR_RECIPE_TYPE
                .recipeBuilder(GTCEu.id("test_direct_linked_item-item_input_cr"))
                .inputItemsRangedMarked(CR_IN, UniformInt.of(0, 9), "orange")
                .inputItemsLinked(CR_OUT, UniformInt.of(0, 9), "direct", "orange")
                .inputFluids(REDSTONE)
                .outputItems(STONE)
                .EUt(GTValues.V[GTValues.HV])
                .duration(2)
                .buildRawRecipe());

        // blue determines the count of orange; orange determines the count of LCENT_IN
        CR_RECIPE_TYPE.getLookup().addRecipe(CR_RECIPE_TYPE
                .recipeBuilder(GTCEu.id("test_chain_linked_fluid-item-item_input_cr"))
                .inputFluidsRangedMarked(RUBBER, UniformInt.of(0, 9), "blue")
                .inputItemsLinkedMarked(CR_IN, UniformInt.of(0, 9), "orange", "direct", "blue")
                .inputItemsLinked(LCENT_IN, UniformInt.of(0, 9), "direct", "orange")
                .inputFluids(REDSTONE)
                .outputItems(STONE)
                .EUt(GTValues.V[GTValues.HV])
                .duration(2)
                .buildRawRecipe());

        // blue determines the amount of orange; orange determines the count of CR_IN
        CR_RECIPE_TYPE.getLookup().addRecipe(CR_RECIPE_TYPE
                .recipeBuilder(GTCEu.id("test_chain_linked_item-fluid-item_input_cr"))
                .inputItemsRangedMarked(LCENT_OUT, UniformInt.of(0, 9), "blue")
                .inputFluidsLinkedMarked(RUBBER, UniformInt.of(0, 9), "orange", "direct", "blue")
                .inputItemsLinked(CR_IN, UniformInt.of(0, 9), "direct", "orange")
                .inputFluids(REDSTONE)
                .outputItems(STONE)
                .EUt(GTValues.V[GTValues.HV])
                .duration(2)
                .buildRawRecipe());
    }

    private static MetaMachine getMetaMachine(BlockEntity entity) {
        return ((MetaMachineBlockEntity) entity).getMetaMachine();
    }

    private record BusHolder(ItemBusPartMachine inputBus1, FluidHatchPartMachine inputHatch1,
                             ItemBusPartMachine outputBus1,
                             FluidHatchPartMachine outputHatch1, WorkableMultiblockMachine controller) {}

    private record BusHolderBatchParallel(ItemBusPartMachine inputBus1, FluidHatchPartMachine inputHatch1,
                                          ItemBusPartMachine outputBus1,
                                          FluidHatchPartMachine outputHatch1,
                                          WorkableElectricMultiblockMachine controller,
                                          ParallelHatchPartMachine parallelHatch) {}

    /**
     * Retrieves the busses for this LCR template and force a multiblock structure check
     *
     * @param helper the GameTestHelper
     * @return the busses, in the BusHolder record.
     */
    private static BusHolder getBussesAndFormLCR(GameTestHelper helper) {
        WorkableMultiblockMachine controller = (WorkableMultiblockMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(1, 2, 0)));
        TestUtils.formMultiblock(controller);
        controller.setRecipeType(LCR_RECIPE_TYPE);
        ItemBusPartMachine inputBus1 = (ItemBusPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(2, 1, 0)));
        FluidHatchPartMachine inputHatch1 = (FluidHatchPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(2, 2, 0)));
        ItemBusPartMachine outputBus1 = (ItemBusPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));
        FluidHatchPartMachine outputHatch1 = (FluidHatchPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 2, 0)));
        return new BusHolder(inputBus1, inputHatch1, outputBus1, outputHatch1, controller);
    }

    /**
     * Retrieves the busses for this Large Centrifuge template and force a multiblock structure check
     *
     * @param helper the GameTestHelper
     * @return the busses, in the BusHolder record.
     */
    private static BusHolderBatchParallel getBussesAndFormLCENT(GameTestHelper helper) {
        WorkableElectricMultiblockMachine controller = (WorkableElectricMultiblockMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(2, 2, 0)));
        TestUtils.formMultiblock(controller);
        controller.setRecipeType(CENTRIFUGE_RECIPE_TYPE);
        ItemBusPartMachine inputBus1 = (ItemBusPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(1, 2, 0)));
        FluidHatchPartMachine inputHatch1 = (FluidHatchPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 2, 0)));
        ItemBusPartMachine outputBus1 = (ItemBusPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(2, 1, 0)));
        FluidHatchPartMachine outputHatch1 = (FluidHatchPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(1, 1, 0)));
        ParallelHatchPartMachine parallelHatch = (ParallelHatchPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(3, 3, 0)));
        return new BusHolderBatchParallel(inputBus1, inputHatch1, outputBus1, outputHatch1, controller, parallelHatch);
    }

    // Test for singleblock machine with item-item direct linked input
    @GameTest(template = "singleblock_charged_cr", batch = "LinkedIngredients")
    public static void singleblockDirectLinkedItemItemInput(GameTestHelper helper) {
        SimpleTieredMachine machine = (SimpleTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        machine.setRecipeType(CR_RECIPE_TYPE);
        NotifiableItemStackHandler itemIn = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).get(0);
        NotifiableFluidTank fluidIn = (NotifiableFluidTank) machine
                .getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP).get(0);
        NotifiableItemStackHandler itemOut = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP).get(0);

        int runs = 7;
        // CR_OUT determines CR_IN
        itemIn.setStackInSlot(0, CR_IN.copyWithCount(64));
        itemIn.setStackInSlot(1, CR_OUT.copyWithCount(64));
        fluidIn.setFluidInTank(0, new FluidStack(REDSTONE, runs));

        // 1t to turn on, 2t per recipe run
        // get the result of each roll independently
        int[] addedRolls = new int[runs];
        int[] matchedRolls = new int[runs];
        for (int i = 0; i < runs; i++) {
            final int finalI = i; // lambda preserve you
            helper.runAfterDelay(2 * i + 1, () -> {
                addedRolls[finalI] = itemIn.getStackInSlot(0).getCount();
                matchedRolls[finalI] = itemIn.getStackInSlot(1).getCount();
            });
        }
        // check the results of all rolls together
        helper.runAfterDelay(runs * 2 + 1, () -> {
            ItemStack[] results = { itemIn.getStackInSlot(0), itemIn.getStackInSlot(1) };

            int upperLimit = 64 - (runs * 0);
            int lowerLimit = 64 - (runs * 9);
            helper.assertTrue(TestUtils.isItemStackEqual(itemOut.getStackInSlot(0), STONE.copyWithCount(runs)),
                    "I-I Direct Linked Singleblock CR didn't complete correct number of recipes, completed [" +
                            itemOut.getStackInSlot(0).getCount() + "] not [" + runs + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results[1], lowerLimit, upperLimit),
                    "I-I Direct Linked Singleblock CR didn't consume correct number of marked items, consumed [" +
                            (64 - results[1].getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results[1], lowerLimit, upperLimit),
                    "I-I Direct Linked Singleblock CR didn't consume correct number of linked items, consumed [" +
                            (64 - results[1].getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertFalse((results[1].getCount() == lowerLimit),
                    "I-I Direct Linked Singleblock CR rolled max value on every roll");
            helper.assertFalse((results[1].getCount() == upperLimit),
                    "I-I Direct Linked Singleblock CR rolled min value on every roll");

            // check if the consumed amounts matched
            for (int i = 0; i < addedRolls.length; i++) {
                helper.assertTrue(addedRolls[i] == matchedRolls[i], "Linked Singleblock CR " +
                        "should have consumed equal ingredient counts! Consumed " + Arrays.toString(addedRolls) +
                        " - vs - " + Arrays.toString(matchedRolls));
            }

            // check if all the rolls were equal
            int[] rolls = new int[runs];
            rolls[0] = 64 - matchedRolls[0];
            boolean allEqual = false;
            for (int i = 1; i < runs; i++) {
                rolls[i] = matchedRolls[i - 1] - matchedRolls[i];
                if (rolls[i] == rolls[i - 1]) {
                    allEqual = true;
                } else {
                    allEqual = false;
                    break;
                }
            }
            helper.assertFalse(allEqual,
                    "Direct Linked Singleblock CR rolled the same value on every input roll (rolled " + rolls[0] + ")");
            helper.succeed();
        });
    }

    // Test for singleblock machine with fluid-item-item direct linked input
    @GameTest(template = "singleblock_charged_cr", batch = "LinkedIngredients")
    public static void singleblockDirectLinkedFluidItemItemInput(GameTestHelper helper) {
        SimpleTieredMachine machine = (SimpleTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        machine.setRecipeType(CR_RECIPE_TYPE);
        NotifiableItemStackHandler itemIn = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).get(0);
        NotifiableFluidTank fluidIn = (NotifiableFluidTank) machine
                .getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP).get(0);
        NotifiableItemStackHandler itemOut = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP).get(0);

        int runs = 7;
        // RUBBER determines CR_IN, CR_IN determines LCENT_IN
        itemIn.setStackInSlot(0, CR_IN.copyWithCount(64));
        itemIn.setStackInSlot(1, LCENT_IN.copyWithCount(64));
        fluidIn.setFluidInTank(0, new FluidStack(REDSTONE, runs));
        fluidIn.setFluidInTank(1, new FluidStack(RUBBER, 64));

        // 1t to turn on, 2t per recipe run
        // get the result of each roll independently
        int[] middleRolls = new int[runs];
        int[] matchedRolls = new int[runs];
        int[] fluidRolls = new int[runs];
        for (int i = 0; i < runs; i++) {
            final int finalI = i; // lambda preserve you
            helper.runAfterDelay(2 * i + 1, () -> {
                middleRolls[finalI] = itemIn.getStackInSlot(0).getCount();
                matchedRolls[finalI] = itemIn.getStackInSlot(1).getCount();
                fluidRolls[finalI] = fluidIn.getFluidInTank(1).getAmount();
            });
        }
        // check the results of all rolls together
        helper.runAfterDelay(runs * 2 + 1, () -> {
            ItemStack[] iresults = { itemIn.getStackInSlot(0), itemIn.getStackInSlot(1) };

            int upperLimit = 64 - (runs * 0);
            int lowerLimit = 64 - (runs * 9);
            helper.assertTrue(TestUtils.isItemStackEqual(itemOut.getStackInSlot(0), STONE.copyWithCount(runs)),
                    "F-I-I Chain Direct Linked Singleblock CR didn't complete correct number of recipes, completed [" +
                            itemOut.getStackInSlot(0).getCount() + "] not [" + runs + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(iresults[1], lowerLimit, upperLimit),
                    "F-I-I Chain Direct Linked Singleblock CR didn't consume correct number of marked items, consumed [" +
                            (64 - iresults[1].getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(iresults[1], lowerLimit, upperLimit),
                    "F-I-I Chain Direct Linked Singleblock CR didn't consume correct number of linked items, consumed [" +
                            (64 - iresults[1].getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertFalse((iresults[1].getCount() == lowerLimit),
                    "F-I-I Chain Direct Linked Singleblock CR rolled max value on every roll");
            helper.assertFalse((iresults[1].getCount() == upperLimit),
                    "F-I-I Chain Direct Linked Singleblock CR rolled min value on every roll");

            // check if the consumed amounts matched
            for (int i = 0; i < middleRolls.length; i++) {
                helper.assertTrue(middleRolls[i] == matchedRolls[i] && matchedRolls[i] == fluidRolls[i],
                        "F-I-I Chain Direct Linked Singleblock CR " +
                                "should have consumed equal ingredient counts! Consumed " +
                                Arrays.toString(fluidRolls) +
                                " - vs - " + Arrays.toString(middleRolls) + " - vs - " + Arrays.toString(matchedRolls));
            }

            // check if all the rolls were equal
            int[] rolls = new int[runs];
            rolls[0] = 64 - matchedRolls[0];
            boolean allEqual = false;
            for (int i = 1; i < runs; i++) {
                rolls[i] = matchedRolls[i - 1] - matchedRolls[i];
                if (rolls[i] == rolls[i - 1]) {
                    allEqual = true;
                } else {
                    allEqual = false;
                    break;
                }
            }
            helper.assertFalse(allEqual,
                    "F-I-I Chain Direct Linked Singleblock CR rolled the same value on every input roll (rolled " +
                            rolls[0] +
                            ")");
            helper.succeed();
        });
    }

    // Test for singleblock machine with item-fluid-item direct linked input
    @GameTest(template = "singleblock_charged_cr", batch = "LinkedIngredients")
    public static void singleblockDirectLinkedItemFluidItemInput(GameTestHelper helper) {
        SimpleTieredMachine machine = (SimpleTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        machine.setRecipeType(CR_RECIPE_TYPE);
        NotifiableItemStackHandler itemIn = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).get(0);
        NotifiableFluidTank fluidIn = (NotifiableFluidTank) machine
                .getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP).get(0);
        NotifiableItemStackHandler itemOut = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP).get(0);

        int runs = 7;
        // LCENT_OUT determines RUBBER, RUBBER determines CR_IN
        itemIn.setStackInSlot(0, LCENT_OUT.copyWithCount(64));
        itemIn.setStackInSlot(1, CR_IN.copyWithCount(64));
        fluidIn.setFluidInTank(0, new FluidStack(REDSTONE, runs));
        fluidIn.setFluidInTank(1, new FluidStack(RUBBER, 64));

        // 1t to turn on, 2t per recipe run
        // get the result of each roll independently
        int[] baseRolls = new int[runs];
        int[] matchedRolls = new int[runs];
        int[] fluidRolls = new int[runs];
        for (int i = 0; i < runs; i++) {
            final int finalI = i; // lambda preserve you
            helper.runAfterDelay(2 * i + 1, () -> {
                baseRolls[finalI] = itemIn.getStackInSlot(0).getCount();
                matchedRolls[finalI] = itemIn.getStackInSlot(1).getCount();
                fluidRolls[finalI] = fluidIn.getFluidInTank(1).getAmount();
            });
        }
        // check the results of all rolls together
        helper.runAfterDelay(runs * 2 + 1, () -> {
            ItemStack[] iresults = { itemIn.getStackInSlot(0), itemIn.getStackInSlot(1) };
            FluidStack fresults = fluidIn.getFluidInTank(1);

            int upperLimit = 64 - (runs * 0);
            int lowerLimit = 64 - (runs * 9);
            helper.assertTrue(TestUtils.isItemStackEqual(itemOut.getStackInSlot(0), STONE.copyWithCount(runs)),
                    "I-F-I Chain Direct Linked Singleblock CR didn't complete correct number of recipes, completed [" +
                            itemOut.getStackInSlot(0).getCount() + "] not [" + runs + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(iresults[1], lowerLimit, upperLimit),
                    "I-F-I Chain Direct Linked Singleblock CR didn't consume correct number of marked items, consumed [" +
                            (64 - iresults[1].getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(iresults[1], lowerLimit, upperLimit),
                    "I-F-I Chain Direct Linked Singleblock CR didn't consume correct number of linked items, consumed [" +
                            (64 - iresults[1].getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertTrue(TestUtils.isFluidWithinRange(fresults, lowerLimit, upperLimit),
                    "I-F-I Chain Direct Linked Singleblock CR didn't consume correct amount of linked fluid, consumed [" +
                            (64 - fresults.getAmount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertFalse((iresults[1].getCount() == lowerLimit),
                    "I-F-I Chain Direct Linked Singleblock CR rolled max value on every roll");
            helper.assertFalse((iresults[1].getCount() == upperLimit),
                    "I-F-I Chain Direct Linked Singleblock CR rolled min value on every roll");

            // check if the consumed amounts matched
            for (int i = 0; i < baseRolls.length; i++) {
                helper.assertTrue(baseRolls[i] == matchedRolls[i] && matchedRolls[i] == fluidRolls[i],
                        "I-F-I Chain Direct Linked Singleblock CR " +
                                "should have consumed equal ingredient counts! Consumed " +
                                Arrays.toString(baseRolls) +
                                " - vs - " + Arrays.toString(fluidRolls) + " - vs - " + Arrays.toString(matchedRolls));
            }

            // check if all the rolls were equal
            int[] rolls = new int[runs];
            rolls[0] = 64 - matchedRolls[0];
            boolean allEqual = false;
            for (int i = 1; i < runs; i++) {
                rolls[i] = matchedRolls[i - 1] - matchedRolls[i];
                if (rolls[i] == rolls[i - 1]) {
                    allEqual = true;
                } else {
                    allEqual = false;
                    break;
                }
            }
            helper.assertFalse(allEqual,
                    "I-F-I Chain Direct Linked Singleblock CR rolled the same value on every input roll (rolled " +
                            rolls[0] +
                            ")");
            helper.succeed();
        });
    }
}
