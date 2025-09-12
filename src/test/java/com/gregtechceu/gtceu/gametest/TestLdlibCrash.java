package com.gregtechceu.gtceu.gametest;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.gregtechceu.gtceu.gametest.util.TestUtils.getMetaMachine;

@PrefixGameTestTemplate(false)
@GameTestHolder(GTCEu.MOD_ID)
public class TestLdlibCrash {

    @GameTest(template = "singleblock_charged_cr", batch = "RangedFluidIngredients")
    public static void LDLibSyncTest(GameTestHelper helper) {
        SimpleTieredMachine machine = (SimpleTieredMachine) getMetaMachine(
                helper.getBlockEntity(new BlockPos(0, 1, 0)));

        NotifiableFluidTank fluidIn = (NotifiableFluidTank) machine
                .getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP).get(0);

        fluidIn.fill(new FluidStack(Fluids.WATER, 10), IFluidHandler.FluidAction.EXECUTE);

        helper.runAfterDelay(40, () -> {
            helper.succeed();
        });
    }
}
