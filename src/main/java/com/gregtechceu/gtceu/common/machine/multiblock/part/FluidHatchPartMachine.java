package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CircuitFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.component.PhantomFluidComponent;
import com.gregtechceu.gtceu.api.ui.component.TankComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.fancy.ConfiguratorPanelComponent;
import com.gregtechceu.gtceu.api.ui.fancy.FancyMachineUIComponent;
import com.gregtechceu.gtceu.api.ui.serialization.SyncedProperty;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/3/4
 * @implNote FluidHatchPartMachine
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidHatchPartMachine extends TieredIOPartMachine implements IMachineLife, IHasCircuitSlot {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidHatchPartMachine.class,
            TieredIOPartMachine.MANAGED_FIELD_HOLDER);

    public static final int INITIAL_TANK_CAPACITY_1X = 8 * FluidType.BUCKET_VOLUME;
    public static final int INITIAL_TANK_CAPACITY_4X = 2 * FluidType.BUCKET_VOLUME;
    public static final int INITIAL_TANK_CAPACITY_9X = FluidType.BUCKET_VOLUME;

    @Persisted
    public final NotifiableFluidTank tank;
    private final int slots;
    @Nullable
    protected TickableSubscription autoIOSubs;
    @Nullable
    protected ISubscription tankSubs;
    @Getter
    @Persisted
    protected final NotifiableItemStackHandler circuitInventory;

    // The `Object... args` parameter is necessary in case a superclass needs to pass any args along to createTank().
    // We can't use fields here because those won't be available while createTank() is called.
    public FluidHatchPartMachine(IMachineBlockEntity holder, int tier, IO io, int initialCapacity, int slots,
                                 Object... args) {
        super(holder, tier, io);
        this.slots = slots;
        this.tank = createTank(initialCapacity, slots, args);
        this.circuitInventory = createCircuitItemHandler(io);
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        return new NotifiableFluidTank(this, slots, getTankCapacity(initialCapacity, getTier()), io);
    }

    public static int getTankCapacity(int initialCapacity, int tier) {
        return initialCapacity * (1 << Math.min(9, tier));
    }

    protected NotifiableItemStackHandler createCircuitItemHandler(Object... args) {
        if (args.length > 0 && args[0] instanceof IO io && io == IO.IN) {
            return new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                    .setFilter(IntCircuitBehaviour::isIntegratedCircuit);
        } else {
            return new NotifiableItemStackHandler(this, 0, IO.NONE);
        }
    }

    @Override
    public void onMachineRemoved() {
        if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
            clearInventory(circuitInventory.storage);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::updateTankSubscription));
        }
        tankSubs = tank.addChangedListener(this::updateTankSubscription);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tankSubs != null) {
            tankSubs.unsubscribe();
            tankSubs = null;
        }
    }

    //////////////////////////////////////
    // ******** Auto IO *********//
    //////////////////////////////////////

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateTankSubscription();
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        updateTankSubscription();
    }

    protected void updateTankSubscription() {
        if (isWorkingEnabled() && ((io == IO.OUT && !tank.isEmpty()) || io == IO.IN) &&
                GTTransferUtils.hasAdjacentFluidHandler(getLevel(), getPos(), getFrontFacing())) {
            autoIOSubs = subscribeServerTick(autoIOSubs, this::autoIO);
        } else if (autoIOSubs != null) {
            autoIOSubs.unsubscribe();
            autoIOSubs = null;
        }
    }

    protected void autoIO() {
        if (getOffsetTimer() % 5 == 0) {
            if (isWorkingEnabled()) {
                if (io == IO.OUT) {
                    tank.exportToNearby(getFrontFacing());
                } else if (io == IO.IN) {
                    tank.importFromNearby(getFrontFacing());
                }
            }
            updateTankSubscription();
        }
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        super.setWorkingEnabled(workingEnabled);
        updateTankSubscription();
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////

    @Override
    public void attachConfigurators(ConfiguratorPanelComponent configuratorPanel) {
        super.attachConfigurators(configuratorPanel);
        if (this.io == IO.IN) {
            configuratorPanel.attachConfigurators(new CircuitFancyConfigurator(circuitInventory.storage));
        }
    }

    @Override
    public void loadServerUI(Player player, UIContainerMenu<MetaMachine> menu, MetaMachine holder) {
        super.loadServerUI(player, menu, holder);
        for (int i = 0; i < this.tank.getTanks(); i++) {
            SyncedProperty<FluidStack> prop = menu.createProperty(FluidStack.class, "tank." + i,
                    this.tank.getFluidInTank(i));
            CustomFluidTank tank = this.tank.getStorages()[i];
            tank.addOnContentsChanged(() -> prop.set(tank.getFluid()));
        }
    }

    @Override
    public ParentUIComponent createBaseUIComponent(FancyMachineUIComponent component) {
        if (slots == 1) {
            return createSingleSlotGUI();
        } else {
            return createMultiSlotGUI();
        }
    }

    protected ParentUIComponent createSingleSlotGUI() {
        var group = UIContainers.horizontalFlow(Sizing.fixed(89), Sizing.fixed(63));
        group.padding(Insets.of(4, 0, 4, 4));
        group.surface(Surface.UI_DISPLAY);
        TankComponent tankWidget;

        // Add input/output-specific widgets
        if (this.io == IO.OUT) {
            // if this is an output hatch, assign tankWidget to the phantom widget displaying the locked fluid...
            group.child(tankWidget = new PhantomFluidComponent(this.tank.getLockedFluid(), 0,
                    () -> this.tank.getLockedFluid().getFluid(), f -> {
                        if (!this.tank.getFluidInTank(0).isEmpty()) {
                            return;
                        }
                        if (f == null || f.isEmpty()) {
                            this.tank.setLocked(false);
                        } else {
                            FluidStack newFluid = f.copy();
                            newFluid.setAmount(1);
                            this.tank.setLocked(true, newFluid);
                        }
                    }).showAmount(false)
                    .configure(c -> {
                        c.positioning(Positioning.absolute(67, 40));
                    }));;

            group.child(UIComponents.toggleButton(GuiTextures.BUTTON_LOCK, this.tank::isLocked, this.tank::setLocked)
                    .setTooltipText("gtceu.gui.fluid_lock.tooltip")
                    .shouldUseBaseBackground()
                    .positioning(Positioning.absolute(7, 40))
                    .sizing(Sizing.fixed(18)))
                    // ...and add the actual tank widget separately.
                    .child(UIComponents.tank(tank.getStorages()[0], 0)
                            .showAmount(true)
                            .canExtract(true)
                            .canInsert(io.support(IO.IN))
                            .positioning(Positioning.absolute(67, 22))
                            .sizing(Sizing.fixed(18)))
                    .child(UIComponents.texture(GuiTextures.FLUID_SLOT)
                            .positioning(Positioning.absolute(67, 22))
                            .sizing(Sizing.fixed(18)));
        } else {
            group.child(tankWidget = (TankComponent) UIComponents.tank(tank.getStorages()[0])
                    .showAmount(true)
                    .canExtract(true)
                    .canInsert(io.support(IO.IN))
                    .positioning(Positioning.absolute(67, 22))
                    .sizing(Sizing.fixed(18)));
        }

        group.child(UIComponents.label(Component.translatable("gtceu.gui.fluid_amount"))
                .positioning(Positioning.absolute(4, 4)))
                .child(UIComponents.label(() -> getFluidAmountText(tankWidget))
                        .positioning(Positioning.absolute(4, 14)))
                .child(UIComponents.label(() -> getFluidNameText(tankWidget))
                        .positioning(Positioning.absolute(4, 24)));

        group.surface(Surface.UI_BACKGROUND_INVERSE);
        return group;
    }

    private Component getFluidNameText(TankComponent component) {
        Component translation;
        if (!tank.getFluidInTank(component.tank()).isEmpty()) {
            translation = tank.getFluidInTank(component.tank()).getDisplayName();
        } else {
            translation = this.tank.getLockedFluid().getFluid().getDisplayName();
        }
        return translation;
    }

    private Component getFluidAmountText(TankComponent component) {
        String fluidAmount = "";
        if (!tank.getFluidInTank(component.tank()).isEmpty()) {
            fluidAmount = getFormattedFluidAmount(tank.getFluidInTank(component.tank()));
        } else {
            // Display Zero to show information about the locked fluid
            if (!this.tank.getLockedFluid().getFluid().isEmpty()) {
                fluidAmount = "0";
            }
        }
        return Component.literal(fluidAmount);
    }

    public String getFormattedFluidAmount(FluidStack fluidStack) {
        return String.format("%,d", fluidStack.isEmpty() ? 0 : fluidStack.getAmount());
    }

    protected ParentUIComponent createMultiSlotGUI() {
        int rowSize = (int) Math.sqrt(slots);
        int colSize = rowSize;
        if (slots == 8) {
            rowSize = 4;
            colSize = 2;
        }

        var group = UIContainers.horizontalFlow(Sizing.fixed(18 * rowSize + 16), Sizing.fixed(18 * colSize + 16));
        group.padding(Insets.of(4));
        var container = UIContainers.grid(Sizing.fill(), Sizing.fill(), rowSize, colSize);
        container.padding(Insets.of(4));

        int index = 0;
        for (int y = 0; y < colSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                StackLayout layout = UIContainers.stack(Sizing.fixed(18), Sizing.fixed(18));
                layout.children(List.of(UIComponents.tank(tank.getStorages()[index++], 0)
                        .canInsert(io.support(IO.IN))
                        .canExtract(true),
                        UIComponents.texture(GuiTextures.FLUID_SLOT)
                                .sizing(Sizing.fixed(18))));
                container.child(layout, x, y);
            }
        }

        container.surface(Surface.UI_BACKGROUND_INVERSE);
        group.child(container);

        return group;
    }
}
