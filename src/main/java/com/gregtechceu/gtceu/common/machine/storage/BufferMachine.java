package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.TieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.trait.AutoOutputTrait;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.mui.factory.PosGuiData;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.value.sync.PanelSyncManager;
import com.gregtechceu.gtceu.api.mui.value.sync.SyncHandlers;
import com.gregtechceu.gtceu.api.mui.widget.ParentWidget;
import com.gregtechceu.gtceu.api.mui.widgets.SlotGroupWidget;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Flow;
import com.gregtechceu.gtceu.api.mui.widgets.slot.FluidSlot;
import com.gregtechceu.gtceu.api.mui.widgets.slot.ItemSlot;
import com.gregtechceu.gtceu.api.mui.widgets.slot.ModularSlot;
import com.gregtechceu.gtceu.api.mui.widgets.slot.SlotGroup;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.client.mui.screen.UISettings;

import net.minecraft.MethodsReturnNonnullByDefault;

import lombok.Getter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BufferMachine extends TieredMachine implements IMuiMachine {

    public static final int TANK_SIZE = 64000;

    @SaveField
    @Getter
    protected final NotifiableItemStackHandler inventory;

    @SaveField
    @Getter
    protected final NotifiableFluidTank tank;
    @SaveField
    @SyncToClient

    public final AutoOutputTrait autoOutput;

    public BufferMachine(BlockEntityCreationInfo info, int tier) {
        super(info, tier);
        this.inventory = createInventory();
        this.tank = createTank();
        this.autoOutput = new AutoOutputTrait(this, List.of(inventory), List.of(tank));
    }

    ////////////////////////////////
    // ***** Initialization ******//
    ////////////////////////////////

    public static int getInventorySize(int tier) {
        return (int) Math.pow(tier + 2, 2);
    }

    public static int getTankSize(int tier) {
        return tier + 2;
    }

    protected NotifiableItemStackHandler createInventory() {
        return new NotifiableItemStackHandler(this, getInventorySize(tier), IO.BOTH);
    }

    protected NotifiableFluidTank createTank() {
        return new NotifiableFluidTank(this, getTankSize(tier), TANK_SIZE, IO.BOTH);
    }

    ////////////////////////////////
    // ********** GUI *********** //
    ////////////////////////////////

    // TODO MUI: Needs EIO widget
    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        for (int i = 0; i < tank.getTanks(); i++) {
            syncManager.syncValue("fluids", i, SyncHandlers.fluidSlot(tank.getStorages()[i]));
        }

        SlotGroup slotGroup = new SlotGroup("inventory", inventory.getSlots());

        int size = tank.getTanks();
        String[] matrix = new String[size];
        for (int i = 0; i < size; i++) {
            String row = "I".repeat(size) + "F";
            matrix[i] = row;
        }

        SlotGroupWidget slotWidget = SlotGroupWidget.builder()
                .matrix(matrix)
                .key('I', i -> new ItemSlot()
                        .slot(new ModularSlot(inventory, i)
                                .slotGroup(slotGroup)))
                .key('F', i -> new FluidSlot()
                        .syncHandler("fluids", i))
                .build();

        mainWidget
                .height(20 + 18 * size)
                .child(slotWidget.center());
    }

    ////////////////////////////////
    // ********** Misc ***********//
    ////////////////////////////////

    @Override
    public void onMachineDestroyed() {
        super.onMachineDestroyed();
        inventory.dropInventoryInWorld();
    }
}
