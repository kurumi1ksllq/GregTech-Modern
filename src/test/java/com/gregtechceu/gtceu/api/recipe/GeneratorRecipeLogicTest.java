package com.gregtechceu.gtceu.api.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.TieredMachine;
import com.gregtechceu.gtceu.api.machine.WorkableTieredMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTFluids;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.gametest.util.TestUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Naphtha;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Oil;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.COMBUSTION_GENERATOR_FUELS;
import static com.gregtechceu.gtceu.gametest.util.TestUtils.getMetaMachine;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class GeneratorRecipeLogicTest {

    @BeforeBatch(batch = "Generator")
    public static void prepare(ServerLevel level) {
        COMBUSTION_GENERATOR_FUELS.getLookup().removeAllRecipes();
        COMBUSTION_GENERATOR_FUELS.getLookup().addRecipe(COMBUSTION_GENERATOR_FUELS.recipeBuilder("naphtha")
                .inputFluids(Naphtha.getFluid(1))
                .duration(5)
                .EUt(-V[LV])
                .buildRawRecipe());
    }


    @GameTest(template = "singleblock_generator" , batch = "Generator")
    //tests if the recipe logic works with proper fluid (example fluid naphtha)
    public static void SingleBlockGeneratorRecipeLogic(GameTestHelper helper){
        WorkableTieredMachine machine = (WorkableTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        NotifiableFluidTank fluidIn =  (NotifiableFluidTank) machine.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP).get(0);
        NotifiableEnergyContainer energyOut = (NotifiableEnergyContainer) machine.getCapabilitiesFlat(IO.OUT, EURecipeCapability.CAP).get(0);
        fluidIn.setFluidInTank(0, Naphtha.getFluid(1));


        // 1 tick to turn on, 5 ticks to run
        helper.succeedOnTickWhen(6, () -> {

            long energyCreated = energyOut.getEnergyStored();
            long energyNeeded = 160;
            // test is we do the right eu generation
            helper.assertTrue( energyCreated == energyNeeded, "Recipe did not create the right amount of EU ");

            // use the right amount of fluid, namely the 1 in the slot, so it's empty after
            int fluidAmount = fluidIn.getFluidInTank(0).getAmount();
            helper.assertTrue( fluidAmount == 0, "Recipe did not use the proper amount of FLUID");

        });


    }


    @GameTest(template = "singleblock_generator" , batch = "Generator")
    public static void SingleBlockGeneratorWithWrongFluid(GameTestHelper helper){

        WorkableTieredMachine machine = (WorkableTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        NotifiableFluidTank fluidIn =  (NotifiableFluidTank) machine.getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP).get(0);
        NotifiableEnergyContainer energyOut = (NotifiableEnergyContainer) machine.getCapabilitiesFlat(IO.OUT, EURecipeCapability.CAP).get(0);
        fluidIn.setFluidInTank(0, Oil.getFluid(1));

        helper.succeedOnTickWhen(0, () -> {

            int fluidAmount = fluidIn.getFluidInTank(0).getAmount();
            helper.assertTrue(fluidAmount == 1, "Recipe consumed the wrong FLUID and ran anyways");


        });
    }


    private record hatchHolder(FluidHatchPartMachine inputHatch, FluidHatchPartMachine lubeHatch, FluidHatchPartMachine oxyHatch,
                             MultiblockControllerMachine controller, EnergyHatchPartMachine energyOutput) {}


    public static hatchHolder getHatchandForm( GameTestHelper helper){

        MultiblockControllerMachine controller = (MultiblockControllerMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(1, 2, 0)));
        FluidHatchPartMachine inputHatch = (FluidHatchPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 2, 1)));

        FluidHatchPartMachine lubeHatch = (FluidHatchPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 2, 3)));
        FluidHatchPartMachine oxyHatch = (FluidHatchPartMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(2, 2, 1)));
        EnergyHatchPartMachine energyHatch = (EnergyHatchPartMachine) getMetaMachine( helper.getBlockEntity(new BlockPos(1, 2, 4)));
        TestUtils.formMultiblock(controller);
        return new hatchHolder(inputHatch, lubeHatch, oxyHatch, controller, energyHatch);
    }


    @GameTest(template = "multiblock_generator" , batch = "Generator")
    public static void multiblockRecipeLogicTest(GameTestHelper helper){
        hatchHolder generator = getHatchandForm(helper);
        generator.inputHatch.tank.setFluidInTank(0, Naphtha.getFluid(1));
        helper.succeedOnTickWhen(6, () -> {
            generator.energyOutput.energyContainer.getEnergyStored();
           helper.assertTrue(false, "this will fail");

        });


    }

}
