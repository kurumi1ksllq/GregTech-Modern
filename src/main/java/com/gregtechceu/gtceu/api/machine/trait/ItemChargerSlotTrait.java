package com.gregtechceu.gtceu.api.machine.trait;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.ISubscription;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class ItemChargerSlotTrait extends MachineTrait {

    public static final MachineTraitType<ItemChargerSlotTrait> TYPE = new MachineTraitType<>(ItemChargerSlotTrait.class);

    @Getter
    private final CustomItemStackHandler inventory;
    private final NotifiableEnergyContainer energyContainer;

    @Nullable
    protected TickableSubscription batterySubs;
    @Nullable
    protected ISubscription energySubs;

    public ItemChargerSlotTrait(MetaMachine machine, NotifiableEnergyContainer energyContainer) {
        super(machine);
        inventory = new CustomItemStackHandler(1);
        this.energyContainer = energyContainer;
        inventory.setFilter(item -> GTCapabilityHelper.getElectricItem(item) != null ||
                (ConfigHolder.INSTANCE.compat.energy.nativeEUToFE &&
                        GTCapabilityHelper.getForgeEnergyItem(item) != null));
    }

    @Override
    public void onMachineLoad() {
        super.onMachineLoad();
        energySubs = energyContainer.addChangedListener(this::updateBatterySubscription);
        inventory.setOnContentsChanged(this::updateBatterySubscription);
    }

    @Override
    public void onMachineUnload() {
        if (energySubs != null) {
            energySubs.unsubscribe();
            energySubs = null;
        }
    }

    protected void updateBatterySubscription() {
        if (energyContainer.dischargeOrRechargeEnergyContainers(inventory, 0, true))
            batterySubs = subscribeServerTick(batterySubs, this::chargeBattery);
        else if (batterySubs != null) {
            batterySubs.unsubscribe();
            batterySubs = null;
        }
    }

    protected void chargeBattery() {
        if (!energyContainer.dischargeOrRechargeEnergyContainers(inventory, 0, false))
            updateBatterySubscription();
    }



    @Override
    public void onMachineDestroyed() {
        inventory.dropInventoryInWorld(getLevel(), getBlockPos());
    }

    @Override
    public MachineTraitType<ItemChargerSlotTrait> getTraitType() {
        return TYPE;
    }
}
