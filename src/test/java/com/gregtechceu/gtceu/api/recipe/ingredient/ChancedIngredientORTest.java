package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Test cases:
 * We have 3 chance logics (OR, AND, XOR). FIRST isn't real.
 * Chance logics are applied to all ingredients within a Handler and IO side. AND and XOR roll in those groupings.
 * Singleblock rolls are % chances. Batch/Parallel rolls multiply for an expected "guaranteed" amount.
 * Need to special test Assembly Line and Distillation Tower because they use nonstandard recipe logic
 */
@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class ChancedIngredientORTest {

    private static GTRecipeType CR_RECIPE_TYPE;
    private static GTRecipeType ASSM_RECIPE_TYPE;
    private static GTRecipeType LCR_RECIPE_TYPE;
    private static GTRecipeType CENTRIFUGE_RECIPE_TYPE;

    // fluids used in recipes. Up top here for quick replacements.
    private static final ItemStack IN_SINGLE = new ItemStack(Items.STRIPPED_SPRUCE_WOOD);
    private static final ItemStack OUT_SINGLE = new ItemStack(Items.SPRUCE_SLAB);
    private static final ItemStack IN_OR_1 = new ItemStack(Items.STRIPPED_BIRCH_WOOD);
    private static final ItemStack IN_OR_2 = new ItemStack(Items.BIRCH_SLAB);
    private static final ItemStack IN_OR_3 = new ItemStack(Items.STRIPPED_JUNGLE_WOOD);
    private static final ItemStack IN_OR_4 = new ItemStack(Items.JUNGLE_SLAB);
    private static final ItemStack COBBLE = new ItemStack(Items.COBBLESTONE);
    private static final ItemStack STONE = new ItemStack(Items.STONE);

    @BeforeBatch(batch = "ChancedIngredientsOR")
    public static void prepare(ServerLevel level) {
        CR_RECIPE_TYPE = TestUtils.createRecipeType("chanced_ingredient_or_cr_tests", GTRecipeTypes.CHEMICAL_RECIPES);
        ASSM_RECIPE_TYPE = TestUtils.createRecipeType("chanced_ingredient_or_assm_tests",
                GTRecipeTypes.ASSEMBLER_RECIPES);
        LCR_RECIPE_TYPE = TestUtils.createRecipeType("chanced_ingredient_or_lcr_tests",
                GTRecipeTypes.LARGE_CHEMICAL_RECIPES);
        CENTRIFUGE_RECIPE_TYPE = TestUtils.createRecipeType("chanced_ingredient_or_centrifuge_tests",
                GTRecipeTypes.CENTRIFUGE_RECIPES);

        CR_RECIPE_TYPE.getLookup().addRecipe(CR_RECIPE_TYPE
                .recipeBuilder(GTCEu.id("test_single_chanced_input_cr"))
                .chancedInput(IN_SINGLE.copyWithCount(3), 5000, 0)
                .inputItems(COBBLE)
                .outputItems(STONE)
                .EUt(GTValues.V[GTValues.HV])
                .duration(2)
                .buildRawRecipe());

        ASSM_RECIPE_TYPE.getLookup().addRecipe(ASSM_RECIPE_TYPE
                .recipeBuilder(GTCEu.id("test_double_or_chanced_input_assm"))
                .chance(4000)
                .inputItems(IN_OR_1.copyWithCount(3))
                .chance(6000)
                .inputItems(IN_OR_2.copyWithCount(3))
                .chance(10000)
                .chancedItemInputLogic(ChanceLogic.OR)
                .inputItems(COBBLE)
                .outputItems(STONE)
                .EUt(GTValues.V[GTValues.HV])
                .duration(2)
                .buildRawRecipe());

        ASSM_RECIPE_TYPE.getLookup().addRecipe(ASSM_RECIPE_TYPE
                .recipeBuilder(GTCEu.id("test_quad_or_chanced_input_assm"))
                .chance(4000)
                .inputItems(STONE.copyWithCount(3))
                .chance(5000)
                .inputItems(IN_OR_2.copyWithCount(3))
                .chance(6000)
                .inputItems(IN_OR_3.copyWithCount(3))
                .chance(7000)
                .inputItems(IN_OR_4.copyWithCount(3))
                .chance(10000)
                .chancedItemInputLogic(ChanceLogic.OR)
                .inputItems(COBBLE)
                .outputItems(STONE)
                .EUt(GTValues.V[GTValues.HV])
                .duration(2)
                .buildRawRecipe());

        CR_RECIPE_TYPE.getLookup().addRecipe(CR_RECIPE_TYPE
                .recipeBuilder(GTCEu.id("test_single_chanced_output_cr"))
                .inputItems(IN_OR_1)
                .chancedOutput(STONE, 5000, 0)
                .EUt(GTValues.V[GTValues.HV])
                .duration(2)
                .buildRawRecipe());

        CR_RECIPE_TYPE.getLookup().addRecipe(CR_RECIPE_TYPE
                .recipeBuilder(GTCEu.id("test_double_or_chanced_output_assm"))
                .inputItems(OUT_SINGLE)
                .chance(4000)
                .outputItems(IN_OR_1)
                .chance(6000)
                .outputItems(IN_OR_2)
                .chance(10000)
                .chancedItemOutputLogic(ChanceLogic.OR)
                .EUt(GTValues.V[GTValues.HV])
                .duration(2)
                .buildRawRecipe());

//        CENTRIFUGE_RECIPE_TYPE.getLookup().addRecipe(CENTRIFUGE_RECIPE_TYPE
//                .recipeBuilder(GTCEu.id("test_double_or_chanced_output_cent"))
//                .inputItems(COBBLE)
//                .chance(4000)
//                .outputItems(IN_OR_1.copyWithCount(1))
//                .chance(6000)
//                .outputItems(IN_OR_2.copyWithCount(1))
//                .chance(10000)
//                .chancedItemOutputLogic(ChanceLogic.OR)
//                .EUt(GTValues.V[GTValues.IV])
//                .duration(2)
//                .buildRawRecipe());
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
        // controller.setRecipeType(LCR_RECIPE_TYPE);
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
        // controller.setRecipeType(CENTRIFUGE_RECIPE_TYPE);
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

    // Failure Test for singleblock machine with chanced item input
    // Provides too few input items, should not run recipes.
    @GameTest(template = "singleblock_charged_cr", batch = "ChancedIngredientsOR")
    public static void singleblockSingleChancedItemInputFailure(GameTestHelper helper) {
        SimpleTieredMachine machine = (SimpleTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        machine.setRecipeType(CR_RECIPE_TYPE);
        NotifiableItemStackHandler itemIn = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).get(0);
        NotifiableItemStackHandler itemOut = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP).get(0);

        int runs = 10;
        itemIn.setStackInSlot(0, IN_SINGLE.copyWithCount(2));
        itemIn.setStackInSlot(1, COBBLE.copyWithCount(runs));
        // 1t to turn on, 2t per non- recipe run
        helper.runAfterDelay(runs * 2 + 1, () -> {
            ItemStack results = itemIn.getStackInSlot(0);

            helper.assertTrue(itemOut.isEmpty(),
                    "Singleblock CR should not have run, ran [" +
                            itemOut.getStackInSlot(0).getCount() + "] times");
            helper.assertTrue(TestUtils.isItemStackEqual(results, IN_SINGLE.copyWithCount(2)),
                    "Singleblock CR should not have consumed items, consumed [" +
                            (2 - results.getCount()) + "]");

            helper.succeed();
        });
    }

    // Test for singleblock machine with single chanced item input
    @GameTest(template = "singleblock_charged_cr", batch = "ChancedIngredientsOR")
    public static void singleblockSingleChancedItemInput(GameTestHelper helper) {
        SimpleTieredMachine machine = (SimpleTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        machine.setRecipeType(CR_RECIPE_TYPE);
        NotifiableItemStackHandler itemIn = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).get(0);
        NotifiableItemStackHandler itemOut = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP).get(0);

        int runs = 16;
        itemIn.setStackInSlot(0, IN_SINGLE.copyWithCount(runs * 3));
        itemIn.setStackInSlot(1, COBBLE.copyWithCount(runs));
        // 1t to turn on, 2t per recipe run
        // check the results of all rolls together
        helper.runAfterDelay(runs * 2 + 1, () -> {
            ItemStack results = itemIn.getStackInSlot(0);
            int upperLimit = 64 - (runs * 0);
            int lowerLimit = 64 - (runs * 3);
            helper.assertTrue(TestUtils.isItemStackEqual(itemOut.getStackInSlot(0), STONE.copyWithCount(runs)),
                    "Singleblock CR didn't complete correct number of recipes, completed [" +
                            itemOut.getStackInSlot(0).getCount() + "] not [" + runs + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results, lowerLimit, upperLimit),
                    "Singleblock CR didn't consume correct number of items, consumed [" +
                            (64 - results.getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertFalse((results.getCount() == lowerLimit),
                    "Singleblock CR succeeded every chance roll!");
            helper.assertFalse((results.getCount() == upperLimit),
                    "Singleblock CR failed every chance roll!");

            helper.succeed();
        });
    }

    // Failure Test for singleblock machine with two OR chanced item inputs
    // Provides too few input items, should not run recipes.
    @GameTest(template = "singleblock_charged_assembler", batch = "ChancedIngredientsOR")
    public static void singleblockDoubleORChancedItemInputFailure(GameTestHelper helper) {
        SimpleTieredMachine machine = (SimpleTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        machine.setRecipeType(ASSM_RECIPE_TYPE);
        NotifiableItemStackHandler itemIn = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).get(0);
        NotifiableItemStackHandler itemOut = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP).get(0);

        int runs = 10;
        itemIn.setStackInSlot(0, IN_OR_1.copyWithCount(63));
        itemIn.setStackInSlot(1, IN_OR_2.copyWithCount(2));
        itemIn.setStackInSlot(2, COBBLE.copyWithCount(runs));
        // 1t to turn on, 2t per non- recipe run
        helper.runAfterDelay(runs * 2 + 1, () -> {
            ItemStack results = itemIn.getStackInSlot(1);

            helper.assertTrue(itemOut.isEmpty(),
                    "Singleblock Assembler (OR) should not have run, ran [" +
                            itemOut.getStackInSlot(0).getCount() + "] times");
            helper.assertTrue(TestUtils.isItemStackEqual(results, IN_OR_2.copyWithCount(2)),
                    "Singleblock Assembler (OR) should not have consumed items, consumed [" +
                            (2 - results.getCount()) + "] + [ " + (63 - itemIn.getStackInSlot(0).getCount()) + "]");

            helper.succeed();
        });
    }

    // Test for singleblock machine with two OR chanced item inputs
    @GameTest(template = "singleblock_charged_assembler", batch = "ChancedIngredientsOR")
    public static void singleblockDoubleORChancedItemInput(GameTestHelper helper) {
        SimpleTieredMachine machine = (SimpleTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        machine.setRecipeType(ASSM_RECIPE_TYPE);
        NotifiableItemStackHandler itemIn = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).get(0);
        NotifiableItemStackHandler itemOut = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP).get(0);

        int runs = 16;
        itemIn.setStackInSlot(0, IN_OR_1.copyWithCount(runs * 3));
        itemIn.setStackInSlot(1, IN_OR_2.copyWithCount(runs * 3));
        itemIn.setStackInSlot(2, COBBLE.copyWithCount(runs));
        // 1t to turn on, 2t per recipe run
        // check the results of all rolls together
        helper.runAfterDelay(runs * 2 + 1, () -> {
            ItemStack results1 = itemIn.getStackInSlot(0);
            ItemStack results2 = itemIn.getStackInSlot(0);
            int upperLimit = 48 - (runs * 0);
            int lowerLimit = 48 - (runs * 3);
            helper.assertTrue(TestUtils.isItemStackEqual(itemOut.getStackInSlot(0), STONE.copyWithCount(runs)),
                    "Singleblock Assembler (OR) didn't complete correct number of recipes, completed [" +
                            itemOut.getStackInSlot(0).getCount() + "] not [" + runs + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results1, lowerLimit, upperLimit),
                    "Singleblock Assembler (OR) didn't consume correct number of first item, consumed [" +
                            (48 - results1.getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results2, lowerLimit, upperLimit),
                    "Singleblock Assembler (OR) didn't consume correct number of second item, consumed [" +
                            (48 - results2.getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertFalse((results1.getCount() == lowerLimit),
                    "Singleblock Assembler succeeded every chance roll!");
            helper.assertFalse((results1.getCount() == upperLimit),
                    "Singleblock Assembler failed every chance roll!");
            helper.assertFalse((results2.getCount() == lowerLimit),
                    "Singleblock Assembler succeeded every chance roll!");
            helper.assertFalse((results2.getCount() == upperLimit),
                    "Singleblock Assembler failed every chance roll!");

            helper.succeed();
        });
    }

    // Test for singleblock machine with four OR chanced item inputs
    @GameTest(template = "singleblock_charged_assembler", batch = "ChancedIngredientsOR")
    public static void singleblockQuadORChancedItemInput(GameTestHelper helper) {
        SimpleTieredMachine machine = (SimpleTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        machine.setRecipeType(ASSM_RECIPE_TYPE);
        NotifiableItemStackHandler itemIn = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).get(0);
        NotifiableItemStackHandler itemOut = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP).get(0);

        int runs = 16;
        itemIn.setStackInSlot(0, STONE.copyWithCount(runs * 3));
        itemIn.setStackInSlot(1, IN_OR_2.copyWithCount(runs * 3));
        itemIn.setStackInSlot(2, IN_OR_3.copyWithCount(runs * 3));
        itemIn.setStackInSlot(3, IN_OR_4.copyWithCount(runs * 3));
        itemIn.setStackInSlot(4, COBBLE.copyWithCount(runs));
        // 1t to turn on, 2t per recipe run
        // check the results of all rolls together
        helper.runAfterDelay(runs * 2 + 1, () -> {
            ItemStack results1 = itemIn.getStackInSlot(0);
            ItemStack results2 = itemIn.getStackInSlot(0);
            ItemStack results3 = itemIn.getStackInSlot(0);
            ItemStack results4 = itemIn.getStackInSlot(0);
            int upperLimit = 48 - (runs * 0);
            int lowerLimit = 48 - (runs * 3);
            helper.assertTrue(TestUtils.isItemStackEqual(itemOut.getStackInSlot(0), STONE.copyWithCount(runs)),
                    "Singleblock Assembler (OR) didn't complete correct number of recipes, completed [" +
                            itemOut.getStackInSlot(0).getCount() + "] not [" + runs + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results1, lowerLimit, upperLimit),
                    "Singleblock Assembler (OR) didn't consume correct number of first item, consumed [" +
                            (48 - results1.getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results2, lowerLimit, upperLimit),
                    "Singleblock Assembler (OR) didn't consume correct number of second item, consumed [" +
                            (48 - results2.getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results3, lowerLimit, upperLimit),
                    "Singleblock Assembler (OR) didn't consume correct number of third item, consumed [" +
                            (48 - results1.getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results4, lowerLimit, upperLimit),
                    "Singleblock Assembler (OR) didn't consume correct number of fourth item, consumed [" +
                            (48 - results2.getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertFalse((results1.getCount() == lowerLimit),
                    "Singleblock Assembler succeeded every chance roll!");
            helper.assertFalse((results1.getCount() == upperLimit),
                    "Singleblock Assembler failed every chance roll!");
            helper.assertFalse((results2.getCount() == lowerLimit),
                    "Singleblock Assembler succeeded every chance roll!");
            helper.assertFalse((results2.getCount() == upperLimit),
                    "Singleblock Assembler failed every chance roll!");
            helper.assertFalse((results3.getCount() == lowerLimit),
                    "Singleblock Assembler succeeded every chance roll!");
            helper.assertFalse((results3.getCount() == upperLimit),
                    "Singleblock Assembler failed every chance roll!");
            helper.assertFalse((results4.getCount() == lowerLimit),
                    "Singleblock Assembler succeeded every chance roll!");
            helper.assertFalse((results4.getCount() == upperLimit),
                    "Singleblock Assembler failed every chance roll!");

            helper.succeed();
        });
    }

    // Test for singleblock machine with single chanced item output
    @GameTest(template = "singleblock_charged_cr", batch = "ChancedIngredientsOR")
    public static void singleblockSingleChancedItemOutput(GameTestHelper helper) {
        SimpleTieredMachine machine = (SimpleTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        machine.setRecipeType(CR_RECIPE_TYPE);
        NotifiableItemStackHandler itemIn = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).get(0);
        NotifiableItemStackHandler itemOut = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP).get(0);

        int runs = 16;
        itemIn.setStackInSlot(0, IN_OR_1.copyWithCount(runs));
        // 1t to turn on, 2t per recipe run
        // check the results of all rolls together
        helper.runAfterDelay(runs * 2 + 1, () -> {
            ItemStack results = itemOut.getStackInSlot(0);
            int upperLimit = runs * 1;
            int lowerLimit = runs * 0;
            helper.assertTrue(itemIn.isEmpty(),
                    "Singleblock CR didn't complete correct number of recipes, completed [" +
                            (runs - itemIn.getStackInSlot(0).getCount()) + "] not [" + runs + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results, lowerLimit, upperLimit),
                    "Singleblock CR didn't produce correct number of items, produced [" +
                            results.getCount() + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertFalse((results.getCount() == lowerLimit),
                    "Singleblock CR succeeded every chance roll!");
            helper.assertFalse((results.getCount() == upperLimit),
                    "Singleblock CR failed every chance roll!");

            helper.succeed();
        });
    }

    // Test for singleblock machine with two OR chanced item outputs
    @GameTest(template = "singleblock_charged_cr", batch = "ChancedIngredientsOR")
    public static void singleblockDoubleORChancedItemOutput(GameTestHelper helper) {
        SimpleTieredMachine machine = (SimpleTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        machine.setRecipeType(CR_RECIPE_TYPE);
        NotifiableItemStackHandler itemIn = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.IN, ItemRecipeCapability.CAP).get(0);
        NotifiableItemStackHandler itemOut = (NotifiableItemStackHandler) machine
                .getCapabilitiesFlat(IO.OUT, ItemRecipeCapability.CAP).get(0);

        int runs = 16;
        itemIn.setStackInSlot(0, OUT_SINGLE.copyWithCount(runs));
        // 1t to turn on, 2t per recipe run
        // check the results of all rolls together
        helper.runAfterDelay(runs * 2 + 1, () -> {
            ItemStack results1 = itemOut.getStackInSlot(0);
            ItemStack results2 = itemOut.getStackInSlot(0);
            int upperLimit = runs * 1;
            int lowerLimit = runs * 0;
            helper.assertTrue(itemIn.isEmpty(),
                    "Singleblock Assembler didn't complete correct number of recipes, completed [" +
                            (runs - itemIn.getStackInSlot(0).getCount()) + "] not [" + runs + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results1, lowerLimit, upperLimit),
                    "Singleblock Assembler (OR) didn't produce correct number of first item, produce [" +
                            (64 - results1.getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertTrue(TestUtils.isItemWithinRange(results2, lowerLimit, upperLimit),
                    "Singleblock Assembler (OR) didn't produce correct number of second item, produce [" +
                            (64 - results2.getCount()) + "] not [" + lowerLimit + "-" + upperLimit + "]");
            helper.assertFalse((results1.getCount() == lowerLimit),
                    "Singleblock Assembler (OR) failed every chance roll!");
            helper.assertFalse((results1.getCount() == upperLimit),
                    "Singleblock Assembler (OR) succeeded every chance roll!");
            helper.assertFalse((results2.getCount() == lowerLimit),
                    "Singleblock Assembler (OR) failed every chance roll!");
            helper.assertFalse((results2.getCount() == upperLimit),
                    "Singleblock Assembler (OR) succeeded every chance roll!");

            helper.succeed();
        });
    }

}
