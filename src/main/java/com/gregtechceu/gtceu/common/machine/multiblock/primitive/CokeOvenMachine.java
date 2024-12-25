package com.gregtechceu.gtceu.common.machine.multiblock.primitive;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;
import com.gregtechceu.gtceu.api.ui.serialization.SyncedProperty;
import com.gregtechceu.gtceu.api.ui.texture.ProgressTexture;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;
import com.gregtechceu.gtceu.api.ui.util.SlotGenerator;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/3/16
 * @implNote CokeOvenMachine
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CokeOvenMachine extends PrimitiveWorkableMachine implements IUIMachine {

    public CokeOvenMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    public void loadServerUI(Player player, UIContainerMenu<MetaMachine> menu, MetaMachine holder) {
        var progressProperty = menu.createProperty(double.class, "progress", recipeLogic.getProgressPercent());
        recipeLogic.addProgressPercentListener(progressProperty::set);

        for (int i = 0; i < this.importFluids.getTanks(); i++) {
            SyncedProperty<FluidStack> prop = menu.createProperty(FluidStack.class, "fluid-in." + i,
                    this.importFluids.getFluidInTank(i));
            CustomFluidTank tank = this.importFluids.getStorages()[i];
            tank.addOnContentsChanged(() -> prop.set(tank.getFluid()));
        }
        for (int i = 0; i < this.exportFluids.getTanks(); i++) {
            SyncedProperty<FluidStack> prop = menu.createProperty(FluidStack.class, "fluid-out." + i,
                    this.exportFluids.getFluidInTank(i));
            CustomFluidTank tank = this.exportFluids.getStorages()[i];
            tank.addOnContentsChanged(() -> prop.set(tank.getFluid()));
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
    }

    @Override
    public void loadClientUI(Player player, UIAdapter<StackLayout> adapter, MetaMachine holder) {
        var menu = adapter.menu();
        StackLayout rootComponent;
        adapter.rootComponent.child(rootComponent = UIContainers.stack(Sizing.fixed(176), Sizing.fixed(166)));

        rootComponent.surface(GuiTextures.PRIMITIVE_BACKGROUND::draw);
        rootComponent.child(UIComponents.label(getBlockState().getBlock().getName())
                .positioning(Positioning.absolute(5, 5)));

        rootComponent.child(UIComponents.slot(menu.getSlot(0))
                .positioning(Positioning.absolute(52, 30)))
                .child(UIComponents
                        .texture(UITextures.group(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_FURNACE_OVERLAY))
                        .positioning(Positioning.absolute(52, 30))
                        .sizing(Sizing.fixed(18)));

        rootComponent.child(UIComponents.progress(adapter.menu().<Double>getProperty("progress")::get)
                .progressTexture(GuiTextures.PRIMITIVE_BLAST_FURNACE_PROGRESS_BAR)
                .positioning(Positioning.absolute(76, 32))
                .sizing(Sizing.fixed(20), Sizing.fixed(15)));

        rootComponent.child(UIComponents.slot(menu.getSlot(1))
                .canInsert(false)
                .canExtract(true)
                .positioning(Positioning.absolute(103, 30)))
                .child(UIComponents
                        .texture(UITextures.group(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_FURNACE_OVERLAY))
                        .positioning(Positioning.absolute(103, 30))
                        .sizing(Sizing.fixed(18)));

        rootComponent.child(UIComponents.texture(GuiTextures.PRIMITIVE_LARGE_FLUID_TANK_OVERLAY)
                .positioning(Positioning.absolute(134, 13))
                .sizing(Sizing.fixed(20), Sizing.fixed(58)))
                .child(UIComponents.tank(exportFluids.getStorages()[0], 0)
                        .canInsert(false)
                        .canExtract(true)
                        .fillDirection(ProgressTexture.FillDirection.DOWN_TO_UP)
                        .backgroundTexture(GuiTextures.PRIMITIVE_LARGE_FLUID_TANK)
                        .positioning(Positioning.absolute(134, 13))
                        .sizing(Sizing.fixed(20), Sizing.fixed(58)));

        rootComponent.child(UIComponents.playerInventory(player.getInventory(), GuiTextures.PRIMITIVE_SLOT)
                .positioning(Positioning.absolute(7, 84)));
    }

    @Override
    public void animateTick(RandomSource random) {
        if (this.isActive()) {
            final BlockPos pos = getPos();
            float x = pos.getX() + 0.5F;
            float z = pos.getZ() + 0.5F;

            final var facing = getFrontFacing();
            final float horizontalOffset = GTValues.RNG.nextFloat() * 0.6F - 0.3F;
            final float y = pos.getY() + GTValues.RNG.nextFloat() * 0.375F + 0.3F;

            if (facing.getAxis() == Direction.Axis.X) {
                if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) x += 0.52F;
                else x -= 0.52F;
                z += horizontalOffset;
            } else if (facing.getAxis() == Direction.Axis.Z) {
                if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) z += 0.52F;
                else z -= 0.52F;
                x += horizontalOffset;
            }
            if (ConfigHolder.INSTANCE.machines.machineSounds && GTValues.RNG.nextDouble() < 0.1) {
                getLevel().playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F,
                        false);
            }
            getLevel().addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0, 0);
            getLevel().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
        }
    }
}
