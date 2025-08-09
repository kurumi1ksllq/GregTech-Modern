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
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTFluids;
import com.gregtechceu.gtceu.common.machine.electric.BatteryBufferMachine;
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


        helper.succeedOnTickWhen(12, () -> {

            long energyCreated = energyOut.getEnergyStored();
            long energyNeeded = 160;
            // test is we do the right eu generation
            helper.assertTrue( energyCreated == energyNeeded, "Recipe did not create the right amount of EU ");

            // use the right amount of fluid
            int fluidAmount = fluidIn.getFluidInTank(0).getAmount();
            helper.assertTrue( fluidAmount == 10, "Recipe did not use the proper amount of FLUID");

        });
        //


    }

}
