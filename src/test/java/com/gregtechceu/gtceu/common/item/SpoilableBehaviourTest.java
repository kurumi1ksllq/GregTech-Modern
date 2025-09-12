package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.cover.filter.SimpleItemFilter;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.cover.ConveyorCover;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.common.machine.storage.CrateMachine;
import com.gregtechceu.gtceu.gametest.util.TestUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

import java.util.Objects;

import static com.gregtechceu.gtceu.gametest.util.TestUtils.getMetaMachine;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class SpoilableBehaviourTest {

    private static GTRecipeType LCR_RECIPE_TYPE;

    @BeforeBatch(batch = "spoilageTests")
    public static void prepare(Level level) {
        LCR_RECIPE_TYPE = TestUtils.createRecipeType("spoilage_lcr_tests");
        LCR_RECIPE_TYPE.getLookup().addRecipe(LCR_RECIPE_TYPE
                .recipeBuilder(GTCEu.id("test_overclock_logic"))
                .inputItems(new ItemStack(Items.JIGSAW))
                .outputItems(new ItemStack(Items.STRUCTURE_BLOCK))
                .EUt(GTValues.V[GTValues.HV])
                .duration(20)
                .buildRawRecipe());
        LCR_RECIPE_TYPE.getLookup().addRecipe(LCR_RECIPE_TYPE
                .recipeBuilder(GTCEu.id("test_overclock_logic"))
                .inputItems(new ItemStack(Items.APPLE))
                .outputItems(new ItemStack(Items.STRUCTURE_BLOCK))
                .EUt(GTValues.V[GTValues.HV])
                .duration(20)
                .keepSpoilingProgress(false)
                .buildRawRecipe());
    }

    private static void makeSpoilables(GameTestHelper helper) {
        new SpoilableBehaviour(10, Items.DIRT).attachTo(Items.JIGSAW);
        new SpoilableBehaviour(10, Items.STRUCTURE_BLOCK).attachTo(Items.APPLE);
        new SpoilableBehaviour(40, Items.STRUCTURE_VOID).attachTo(Items.STRUCTURE_BLOCK);
        new SpoilableBehaviour(10, Items.JIGSAW).attachTo(Items.STRUCTURE_VOID);
        helper.runAtTickTime(100, () -> {
            ISpoilableItem.unspoil(Items.JIGSAW);
            ISpoilableItem.unspoil(Items.STRUCTURE_VOID);
            ISpoilableItem.unspoil(Items.APPLE);
            ISpoilableItem.unspoil(Items.STRUCTURE_BLOCK);
            helper.succeed();
        });
    }

    private static BusHolder getBussesAndForm(GameTestHelper helper) {
        WorkableMultiblockMachine controller = (WorkableMultiblockMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(1, 2, 0)));
        TestUtils.formMultiblock(controller);
        controller.setRecipeType(LCR_RECIPE_TYPE);
        ItemBusPartMachine inputBus1 = (ItemBusPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(2, 1, 0)));
        ItemBusPartMachine inputBus2 = (ItemBusPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(2, 2, 0)));
        ItemBusPartMachine outputBus1 = (ItemBusPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));
        FluidHatchPartMachine outputHatch1 = (FluidHatchPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 2, 0)));
        return new BusHolder(inputBus1, inputBus2, outputBus1, outputHatch1, controller);
    }

    private record BusHolder(ItemBusPartMachine inputBus1, ItemBusPartMachine inputBus2, ItemBusPartMachine outputBus1,
                             FluidHatchPartMachine outputHatch1, WorkableMultiblockMachine controller) {}

    @GameTest(template = "empty_5x5", batch = "spoilageTests")
    public static void itemDoesntSpoilInChest(GameTestHelper helper) {
        makeSpoilables(helper);
        helper.setBlock(1, 1, 1, Blocks.CHEST);
        IItemHandler itemHandler = TestUtils.getItemHandler(helper, new BlockPos(1, 1, 1));
        itemHandler.insertItem(0, Items.JIGSAW.getDefaultInstance(), false);
        helper.failIfEver(() -> helper.assertTrue(TestUtils.isItemStackEqual(
                Items.JIGSAW.getDefaultInstance(),
                itemHandler.getStackInSlot(0)), "jigsaw spoiled when shouldn't have"));
    }

    @GameTest(template = "empty_5x5", batch = "spoilageTests")
    public static void itemSpoilsRecursively(GameTestHelper helper) {
        makeSpoilables(helper);
        helper.setBlock(1, 1, 1, Blocks.CHEST);
        IItemHandler itemHandler = TestUtils.getItemHandler(helper, new BlockPos(1, 1, 1));
        ItemStack in = Items.APPLE.getDefaultInstance().copyWithCount(41);
        ISpoilableItem.update(in, null);
        itemHandler.insertItem(0, in, false);
        helper.runAtTickTime(70, () -> helper.assertTrue(TestUtils.isItemStackEqual(
                Items.DIRT.getDefaultInstance().copyWithCount(41),
                itemHandler.getStackInSlot(0)),
                "apple didn't spoil recursively (apple -> structure block -> structure void -> jigsaw -> dirt)"));
    }

    @GameTest(template = "empty_5x5", batch = "spoilageTests")
    public static void itemSpoilsInCrate(GameTestHelper helper) {
        makeSpoilables(helper);
        CrateMachine crate = (CrateMachine) TestUtils.setMachine(helper, new BlockPos(1, 1, 1), GTMachines.STEEL_CRATE);
        crate.inventory.insertItem(0, Items.JIGSAW.getDefaultInstance().copyWithCount(23), false);
        helper.runAtTickTime(9, () -> {
            ItemStack stack = crate.inventory.getStackInSlot(0);
            helper.assertTrue(TestUtils.isItemStackEqual(
                    Items.JIGSAW.getDefaultInstance().copyWithCount(23),
                    stack), "jigsaw spoiled 1 tick earlier");
            ISpoilableItem spoilable = ISpoilableItem.getSpoilable(stack);
            helper.assertTrue(spoilable != null, "spoilable was null when shouldn't have");
            assert spoilable != null;
            helper.assertTrue(spoilable.shouldSpoil(stack), "shouldSpoil returned false on spoilable item");
            helper.assertTrue(spoilable.getTicksUntilSpoiled(stack) == 1,
                    "spoilable didn't return correct ticks until spoiled amount");
            helper.assertTrue(spoilable.getSpoilTicks(stack) == 10,
                    "spoilable didn't return correct total tick amount");
        });
        helper.runAtTickTime(10, () -> helper.assertTrue(TestUtils.isItemStackEqual(
                Items.DIRT.getDefaultInstance().copyWithCount(23),
                crate.inventory.getStackInSlot(0)), "jigsaw didn't spoil when should have"));
    }

    @GameTest(template = "lcr_input_separation", batch = "spoilageTests")
    public static void spoilageTransfersInRecipe(GameTestHelper helper) {
        makeSpoilables(helper);
        BusHolder busHolder = getBussesAndForm(helper);
        ItemStack input = new ItemStack(Items.JIGSAW);
        ISpoilableItem.update(input, null);
        Objects.requireNonNull(ISpoilableItem.getSpoilable(input)).setTicksUntilSpoiled(input, 8);
        busHolder.inputBus1.getInventory().setStackInSlot(0, input);
        helper.runAtTickTime(21, () -> {
            ItemStack stack = busHolder.outputBus1.getInventory().getStackInSlot(0);
            helper.assertTrue(TestUtils.isItemStackEqual(
                    stack,
                    new ItemStack(Items.STRUCTURE_BLOCK)),
                    "incorrect recipe output (%s != %s)".formatted(stack.toString(),
                            new ItemStack(Items.STRUCTURE_BLOCK).toString()));
            ISpoilableItem spoilable = ISpoilableItem.getSpoilable(stack);
            helper.assertTrue(spoilable != null, "recipe output was not spoilable");
            assert spoilable != null;
            TestUtils.assertEqual(helper, spoilable.getTicksUntilSpoiled(stack), 27,
                    "recipe output didn't have correct ticks until spoiled");
        });
    }

    @GameTest(template = "lcr_input_separation", batch = "spoilageTests")
    public static void spoilageDoesntTransferInRecipe(GameTestHelper helper) {
        makeSpoilables(helper);
        BusHolder busHolder = getBussesAndForm(helper);
        ItemStack input = new ItemStack(Items.APPLE);
        ISpoilableItem.update(input, null);
        Objects.requireNonNull(ISpoilableItem.getSpoilable(input)).setTicksUntilSpoiled(input, 8);
        busHolder.inputBus1.getInventory().setStackInSlot(0, input);
        helper.runAtTickTime(21, () -> {
            ItemStack stack = busHolder.outputBus1.getInventory().getStackInSlot(0);
            helper.assertTrue(TestUtils.isItemStackEqual(
                    stack,
                    new ItemStack(Items.STRUCTURE_BLOCK)),
                    "incorrect recipe output (%s != %s)".formatted(stack.toString(),
                            new ItemStack(Items.STRUCTURE_BLOCK).toString()));
            ISpoilableItem spoilable = ISpoilableItem.getSpoilable(stack);
            helper.assertTrue(spoilable != null, "recipe output was not spoilable");
            assert spoilable != null;
            TestUtils.assertEqual(helper, spoilable.getTicksUntilSpoiled(stack), 40,
                    "recipe output didn't have correct ticks until spoiled");
        });
    }

    @GameTest(template = "empty_5x5", batch = "spoilageTests")
    public static void droppedItemSpoils(GameTestHelper helper) {
        makeSpoilables(helper);
        ItemEntity item = helper.spawnItem(Items.JIGSAW, new BlockPos(1, 1, 1));
        helper.runAtTickTime(10, () -> helper.assertTrue(TestUtils.isItemStackEqual(
                item.getItem(),
                Items.DIRT.getDefaultInstance()), "item didn't spoil when dropped"));
    }

    @GameTest(template = "empty_5x5", batch = "spoilageTests")
    public static void itemSpoilsInInventory(GameTestHelper helper) {
        makeSpoilables(helper);
        Player player = helper.makeMockPlayer();
        player.getInventory().setItem(0, Items.JIGSAW.getDefaultInstance());
        player.tick();
        helper.runAtTickTime(10, () -> helper.assertTrue(TestUtils.isItemStackEqual(
                player.getInventory().getItem(0),
                Items.DIRT.getDefaultInstance()),
                "item didn't spoil in a player inventory (%s != %s)".formatted(player.getInventory().getItem(0),
                        Items.DIRT.getDefaultInstance())));
    }

    @GameTest(template = "empty_5x5", batch = "spoilageTests")
    public static void spoilableFilteringTest(GameTestHelper helper) {
        makeSpoilables(helper);
        CrateMachine crate1 = (CrateMachine) TestUtils.setMachine(helper, new BlockPos(1, 1, 1),
                GTMachines.STEEL_CRATE);
        CrateMachine crate2 = (CrateMachine) TestUtils.setMachine(helper, new BlockPos(1, 2, 1),
                GTMachines.STEEL_CRATE);
        ConveyorCover cover = (ConveyorCover) TestUtils.placeCover(helper, crate1, GTItems.CONVEYOR_MODULE_HV.asStack(),
                Direction.UP);
        CompoundTag filterTag = SimpleItemFilter.forItems(Items.STRUCTURE_BLOCK.getDefaultInstance()).saveFilter();
        ItemStack filter = GTItems.ITEM_FILTER.asStack();
        filter.setTag(filterTag);
        cover.getFilterHandler().loadFilter(filter);
        cover.setWorkingEnabled(false);
        crate1.inventory.setStackInSlot(0, Items.STRUCTURE_BLOCK.getDefaultInstance());
        helper.runAtTickTime(10, () -> cover.setWorkingEnabled(true));
        helper.runAtTickTime(20, () -> {
            ItemStack stack = crate2.inventory.getStackInSlot(0);
            ISpoilableItem spoilable = ISpoilableItem.getSpoilable(stack);
            helper.assertTrue(TestUtils.isItemStackEqual(stack, Items.STRUCTURE_BLOCK.getDefaultInstance()),
                    "wrong item");
            helper.assertTrue(spoilable != null, "spoilable was null");
            assert spoilable != null;
            TestUtils.assertEqual(helper, 20, spoilable.getTicksUntilSpoiled(stack), "wrong ticks until spoiled");
        });
    }

    @GameTest(template = "empty_5x5", batch = "spoilageTests")
    public static void spoilableFilteringWithSpoilableTest(GameTestHelper helper) {
        makeSpoilables(helper);
        CrateMachine crate1 = (CrateMachine) TestUtils.setMachine(helper, new BlockPos(1, 1, 1),
                GTMachines.STEEL_CRATE);
        CrateMachine crate2 = (CrateMachine) TestUtils.setMachine(helper, new BlockPos(1, 2, 1),
                GTMachines.STEEL_CRATE);
        ConveyorCover cover = (ConveyorCover) TestUtils.placeCover(helper, crate1, GTItems.CONVEYOR_MODULE_HV.asStack(),
                Direction.UP);
        ItemStack itemForFilter = Items.STRUCTURE_BLOCK.getDefaultInstance();
        ISpoilableItem filterSpoilable = ISpoilableItem.getSpoilable(itemForFilter);
        assert filterSpoilable != null;
        ISpoilableItem.update(itemForFilter, null);
        filterSpoilable.setTicksUntilSpoiled(itemForFilter, 5);
        CompoundTag filterTag = SimpleItemFilter.forItems(itemForFilter).saveFilter();
        ItemStack filter = GTItems.ITEM_FILTER.asStack();
        filter.setTag(filterTag);
        cover.getFilterHandler().loadFilter(filter);
        cover.setWorkingEnabled(false);
        crate1.inventory.setStackInSlot(0, Items.STRUCTURE_BLOCK.getDefaultInstance());
        helper.runAtTickTime(10, () -> cover.setWorkingEnabled(true));
        helper.runAtTickTime(20, () -> {
            ItemStack stack = crate2.inventory.getStackInSlot(0);
            ISpoilableItem spoilable = ISpoilableItem.getSpoilable(stack);
            helper.assertTrue(TestUtils.isItemStackEqual(stack, Items.STRUCTURE_BLOCK.getDefaultInstance()),
                    "wrong item");
            helper.assertTrue(spoilable != null, "spoilable was null");
            assert spoilable != null;
            TestUtils.assertEqual(helper, 20, spoilable.getTicksUntilSpoiled(stack), "wrong ticks until spoiled");
        });
    }
}
