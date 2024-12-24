package com.gregtechceu.gtceu.api.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.feature.IEnvironmentalHazardEmitter;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.editable.EditableMachineUI;
import com.gregtechceu.gtceu.api.ui.serialization.SyncedProperty;
import com.gregtechceu.gtceu.api.ui.util.SlotGenerator;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidStack;

import com.google.common.collect.Tables;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/3/17
 * @implNote SimpleGeneratorMachine
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleGeneratorMachine extends WorkableTieredMachine
                                    implements IFancyUIMachine, IEnvironmentalHazardEmitter {

    @Getter
    private final float hazardStrengthPerOperation;

    public SimpleGeneratorMachine(IMachineBlockEntity holder, int tier,
                                  float hazardStrengthPerOperation, Int2IntFunction tankScalingFunction,
                                  Object... args) {
        super(holder, tier, tankScalingFunction, args);
        this.hazardStrengthPerOperation = hazardStrengthPerOperation;
    }

    public SimpleGeneratorMachine(IMachineBlockEntity holder, int tier, Int2IntFunction tankScalingFunction,
                                  Object... args) {
        this(holder, tier, 0.25f, tankScalingFunction, args);
    }
    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////

    @Override
    protected NotifiableEnergyContainer createEnergyContainer(Object... args) {
        var energyContainer = super.createEnergyContainer(args);
        energyContainer.setSideOutputCondition(side -> !hasFrontFacing() || side == getFrontFacing());
        return energyContainer;
    }

    @Override
    protected boolean isEnergyEmitter() {
        return true;
    }

    @Override
    protected long getMaxInputOutputAmperage() {
        return 1L;
    }

    @Override
    public int tintColor(int index) {
        if (index == 2) {
            return GTValues.VC[getTier()];
        }
        return super.tintColor(index);
    }

    //////////////////////////////////////
    // ****** RECIPE LOGIC *******//
    //////////////////////////////////////

    /**
     * Recipe Modifier for <b>Simple Generator Machines</b> - can be used as a valid {@link RecipeModifier}
     * <p>
     * Recipe is fast parallelized up to {@code desiredEUt / recipeEUt} times.
     * </p>
     *
     * @param machine a {@link SimpleGeneratorMachine}
     * @param recipe  recipe
     * @return A {@link ModifierFunction} for the given Simple Generator
     */
    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof SimpleGeneratorMachine generator)) {
            return RecipeModifier.nullWrongType(SimpleGeneratorMachine.class, machine);
        }
        long EUt = RecipeHelper.getOutputEUt(recipe);
        if (EUt <= 0) return ModifierFunction.NULL;

        int maxParallel = (int) (generator.getOverclockVoltage() / EUt);
        int parallels = ParallelLogic.getParallelAmountFast(generator, recipe, maxParallel);

        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(parallels))
                .outputModifier(ContentModifier.multiplier(parallels))
                .eutMultiplier(parallels)
                .parallels(parallels)
                .build();
    }

    @Override
    public boolean dampingWhenWaiting() {
        return false;
    }

    @Override
    public boolean canVoidRecipeOutputs(RecipeCapability<?> capability) {
        return capability != EURecipeCapability.CAP;
    }

    @Override
    public void afterWorking() {
        super.afterWorking();
        spreadEnvironmentalHazard();
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public void loadServerUI(Player player, UIContainerMenu<MetaMachine> menu, MetaMachine holder) {
        var recipeTypeProperty = menu.createProperty(int.class, "current_recipe_type", this.getActiveRecipeType());
        final int changeListener = this.addRecipeTypeChangeListener(recipeTypeProperty::set);

        var progressProperty = menu.createProperty(double.class, "progress", recipeLogic.getProgressPercent());
        final int progressListener = recipeLogic.addProgressPercentListener(progressProperty::set);

        final IntList[] inputFluidsToClose = new IntList[this.importFluids.getTanks()];
        final IntList[] outputFluidsToClose = new IntList[this.exportFluids.getTanks()];

        for (int i = 0; i < this.importFluids.getTanks(); i++) {
            SyncedProperty<FluidStack> prop = menu.createProperty(FluidStack.class, "fluid-in." + i,
                    this.importFluids.getFluidInTank(i));
            CustomFluidTank tank = this.importFluids.getStorages()[i];
            inputFluidsToClose[i] = new IntArrayList();
            inputFluidsToClose[i].add(tank.addOnContentsChanged(() -> prop.set(tank.getFluid())));
        }
        for (int i = 0; i < this.exportFluids.getTanks(); i++) {
            SyncedProperty<FluidStack> prop = menu.createProperty(FluidStack.class, "fluid-out." + i,
                    this.exportFluids.getFluidInTank(i));
            CustomFluidTank tank = this.exportFluids.getStorages()[i];
            outputFluidsToClose[i] = new IntArrayList();
            outputFluidsToClose[i].add(tank.addOnContentsChanged(() -> prop.set(tank.getFluid())));
        }
        // Position all slots at 0,0 as they'll be moved to the correct position on the client.
        SlotGenerator generator = SlotGenerator.begin(menu::addSlot, 0, 0);
        for (int i = 0; i < this.importItems.storage.getSlots(); i++) {
            generator.slot(this.importItems.storage, i, 0, 0);
        }
        for (int i = 0; i < this.exportItems.storage.getSlots(); i++) {
            generator.slot(this.exportItems.storage, i, 0, 0);
        }
        generator.playerInventory(menu.getPlayerInventory());

        // clear up all listener references
        menu.setCloseCallback(p -> {
            this.removeRecipeTypeChangeListener(changeListener);
            recipeLogic.removeProgressPercentListener(progressListener);
            var importStorages = this.importFluids.getStorages();
            for (int i = 0; i < inputFluidsToClose.length; i++) {
                for (int j : inputFluidsToClose[i]) {
                    importStorages[i].removeOnContersChanged(j);
                }
            }
            var exportStorages = this.exportFluids.getStorages();
            for (int i = 0; i < outputFluidsToClose.length; i++) {
                for (int j : outputFluidsToClose[i]) {
                    exportStorages[i].removeOnContersChanged(j);
                }
            }
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    public static BiFunction<ResourceLocation, GTRecipeType, EditableMachineUI> EDITABLE_UI_CREATOR = Util
            .memoize((path, recipeType) -> new EditableMachineUI(path, () -> {
                FlowLayout template = recipeType.getRecipeUI().createEditableUITemplate(false, false)
                        .createDefault();
                StackLayout group = UIContainers.stack(Sizing.content(), Sizing.content().copy().min(78));
                // template.positioning(Positioning.relative(50, 50));
                group.child(template);
                return group;
            }, (template, adapter, machine) -> {
                if (machine instanceof SimpleGeneratorMachine generatorMachine) {
                    var storages = Tables.newCustomTable(new EnumMap<>(IO.class),
                            LinkedHashMap<RecipeCapability<?>, Object>::new);
                    storages.put(IO.IN, ItemRecipeCapability.CAP, generatorMachine.importItems.storage);
                    storages.put(IO.OUT, ItemRecipeCapability.CAP, generatorMachine.exportItems.storage);
                    storages.put(IO.IN, FluidRecipeCapability.CAP, generatorMachine.importFluids);
                    storages.put(IO.OUT, FluidRecipeCapability.CAP, generatorMachine.exportFluids);

                    generatorMachine.getRecipeType().getRecipeUI().createEditableUITemplate(false, false).setupUI(
                            template, adapter,
                            new GTRecipeTypeUI.RecipeHolder(generatorMachine.recipeLogic::getProgressPercent,
                                    storages,
                                    new CompoundTag(),
                                    Collections.emptyList(),
                                    false, false));
                    createEnergyBar().setupUI(template, adapter, generatorMachine);
                }
            }));
}
