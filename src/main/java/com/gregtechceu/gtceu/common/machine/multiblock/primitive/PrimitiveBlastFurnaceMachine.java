package com.gregtechceu.gtceu.common.machine.multiblock.primitive;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
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
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/3/17
 * @implNote PrimitiveBlastFurnaceMachine
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PrimitiveBlastFurnaceMachine extends PrimitiveWorkableMachine implements IUIMachine {

    private TickableSubscription hurtSubscription;

    public PrimitiveBlastFurnaceMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }

    @Override
    protected NotifiableItemStackHandler createImportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, getRecipeType().getMaxInputs(ItemRecipeCapability.CAP), IO.IN,
                IO.NONE);
    }

    @Override
    protected NotifiableItemStackHandler createExportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, getRecipeType().getMaxOutputs(ItemRecipeCapability.CAP), IO.OUT,
                IO.NONE);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        unsubscribe(hurtSubscription);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        this.hurtSubscription = subscribeServerTick(this::hurtEntities);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        unsubscribe(hurtSubscription);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        super.clientTick();
        if (isFormed) {
            var pos = this.getPos();
            var facing = this.getFrontFacing().getOpposite();
            float xPos = facing.getStepX() * 0.76F + pos.getX() + 0.5F;
            float yPos = facing.getStepY() * 0.76F + pos.getY() + 0.25F;
            float zPos = facing.getStepZ() * 0.76F + pos.getZ() + 0.5F;

            var up = RelativeDirection.UP.getRelativeFacing(getFrontFacing(), getUpwardsFacing(), isFlipped());
            var sign = up.getAxisDirection().getStep();
            var shouldX = up.getAxis() == Direction.Axis.X;
            var shouldY = up.getAxis() == Direction.Axis.Y;
            var shouldZ = up.getAxis() == Direction.Axis.Z;
            var speed = ((shouldY ? facing.getStepY() : shouldX ? facing.getStepX() : facing.getStepZ()) * 0.1F + 0.2F +
                    0.1F * GTValues.RNG.nextFloat()) * sign;
            if (getOffsetTimer() % 20 == 0) {
                getLevel().addParticle(ParticleTypes.LAVA, xPos, yPos, zPos,
                        shouldX ? speed * 2 : 0,
                        shouldY ? speed * 2 : 0,
                        shouldZ ? speed * 2 : 0);
            }
            if (isActive()) {
                getLevel().addParticle(ParticleTypes.LARGE_SMOKE, xPos, yPos, zPos,
                        shouldX ? speed : 0,
                        shouldY ? speed : 0,
                        shouldZ ? speed : 0);
            }
        }
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
        for (int i = 0; i < this.importItems.getSlots(); i++) {
            generator.slot(this.importItems, i, 0, 0);
        }
        for (int i = 0; i < this.exportItems.getSlots(); i++) {
            generator.slot(this.exportItems, i, 0, 0);
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
                .positioning(Positioning.absolute(52, 20)))
                .child(UIComponents
                        .texture(UITextures.group(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_INGOT_OVERLAY))
                        .positioning(Positioning.absolute(52, 20))
                        .sizing(Sizing.fixed(18)))
                .child(UIComponents.slot(menu.getSlot(1))
                        .positioning(Positioning.absolute(52, 38)))
                .child(UIComponents
                        .texture(UITextures.group(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_DUST_OVERLAY))
                        .positioning(Positioning.absolute(52, 38))
                        .sizing(Sizing.fixed(18)))
                .child(UIComponents.slot(menu.getSlot(2))
                        .positioning(Positioning.absolute(52, 38)))
                .child(UIComponents
                        .texture(UITextures.group(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_FURNACE_OVERLAY))
                        .positioning(Positioning.absolute(52, 38))
                        .sizing(Sizing.fixed(18)));

        rootComponent.child(UIComponents.progress(adapter.menu().<Double>getProperty("progress")::get)
                .progressTexture(GuiTextures.PRIMITIVE_BLAST_FURNACE_PROGRESS_BAR)
                .positioning(Positioning.absolute(77, 39))
                .sizing(Sizing.fixed(20), Sizing.fixed(15)));

        rootComponent.child(UIComponents.slot(menu.getSlot(4))
                .canInsert(false)
                .canExtract(true)
                .positioning(Positioning.absolute(104, 38)))
                .child(UIComponents
                        .texture(UITextures.group(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_INGOT_OVERLAY))
                        .positioning(Positioning.absolute(104, 38))
                        .sizing(Sizing.fixed(18)))
                .child(UIComponents.slot(menu.getSlot(5))
                        .canInsert(false)
                        .canExtract(true)
                        .positioning(Positioning.absolute(122, 38)))
                .child(UIComponents
                        .texture(UITextures.group(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_DUST_OVERLAY))
                        .positioning(Positioning.absolute(122, 38))
                        .sizing(Sizing.fixed(18)))
                .child(UIComponents.slot(menu.getSlot(6))
                        .canInsert(false)
                        .canExtract(true)
                        .positioning(Positioning.absolute(140, 38)))
                .child(UIComponents
                        .texture(UITextures.group(GuiTextures.PRIMITIVE_SLOT, GuiTextures.PRIMITIVE_DUST_OVERLAY))
                        .positioning(Positioning.absolute(140, 38))
                        .sizing(Sizing.fixed(18)));

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

    private void hurtEntities() {
        BlockPos middlePos = self().getPos().offset(getFrontFacing().getOpposite().getNormal());
        getLevel().getEntities(null,
                new AABB(middlePos)).forEach(e -> e.hurt(e.damageSources().lava(), 3.0f));
    }
}
