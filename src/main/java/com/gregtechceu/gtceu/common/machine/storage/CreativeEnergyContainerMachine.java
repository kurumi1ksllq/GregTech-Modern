package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.ILaserContainer;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.component.*;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeEnergyContainerMachine extends MetaMachine implements ILaserContainer, IUIMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CreativeEnergyContainerMachine.class, MetaMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private long voltage = 0;
    @Persisted
    private int amps = 1;
    @Persisted
    private int setTier = 0;
    @Persisted
    private boolean active = false;
    @Persisted
    private boolean source = true;
    @Persisted
    private long energyIOPerSec = 0;
    private long lastAverageEnergyIOPerTick = 0;
    private long ampsReceived = 0;
    private boolean doExplosion = false;

    public CreativeEnergyContainerMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    //////////////////////////////////////
    // ***** Initialization ******//

    /// ///////////////////////////////////
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        subscribeServerTick(this::updateEnergyTick);
    }

    //////////////////////////////////////
    // ********** MISC ***********//

    /// ///////////////////////////////////

    protected void updateEnergyTick() {
        if (getOffsetTimer() % 20 == 0) {
            this.setIOSpeed(energyIOPerSec / 20);
            energyIOPerSec = 0;
            if (doExplosion) {
                getLevel().explode(null, getPos().getX() + 0.5, getPos().getY() + 0.5, getPos().getZ() + 0.5,
                        1, Level.ExplosionInteraction.NONE);
                doExplosion = false;
            }
        }
        ampsReceived = 0;
        if (!active || !source || voltage <= 0 || amps <= 0) return;
        int ampsUsed = 0;
        for (var facing : GTUtil.DIRECTIONS) {
            var opposite = facing.getOpposite();
            IEnergyContainer container = GTCapabilityHelper.getEnergyContainer(getLevel(), getPos().relative(facing),
                    opposite);
            // Try to get laser capability
            if (container == null)
                container = GTCapabilityHelper.getLaser(getLevel(), getPos().relative(facing), opposite);

            if (container != null && container.inputsEnergy(opposite) && container.getEnergyCanBeInserted() > 0) {
                ampsUsed += container.acceptEnergyFromNetwork(opposite, voltage, amps - ampsUsed);
                if (ampsUsed >= amps) {
                    break;
                }
            }
        }
        energyIOPerSec += ampsUsed * voltage;
    }

    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
        if (source || !active || ampsReceived >= amps) {
            return 0;
        }
        if (voltage > this.voltage) {
            if (doExplosion)
                return 0;
            doExplosion = true;
            return Math.min(amperage, getInputAmperage() - ampsReceived);
        }
        long amperesAccepted = Math.min(amperage, getInputAmperage() - ampsReceived);
        if (amperesAccepted > 0) {
            ampsReceived += amperesAccepted;
            energyIOPerSec += amperesAccepted * voltage;
            return amperesAccepted;
        }
        return 0;
    }

    @Override
    public boolean inputsEnergy(Direction side) {
        return !source;
    }

    @Override
    public boolean outputsEnergy(Direction side) {
        return source;
    }

    @Override
    public long changeEnergy(long differenceAmount) {
        if (source || !active) {
            return 0;
        }
        energyIOPerSec += differenceAmount;
        return differenceAmount;
    }

    @Override
    public long getEnergyStored() {
        return 69;
    }

    @Override
    public long getEnergyCapacity() {
        return 420;
    }

    @Override
    public long getInputAmperage() {
        return source ? 0 : amps;
    }

    @Override
    public long getInputVoltage() {
        return source ? 0 : voltage;
    }

    @Override
    public long getOutputVoltage() {
        return source ? voltage : 0;
    }

    @Override
    public long getOutputAmperage() {
        return source ? amps : 0;
    }

    public void setIOSpeed(long energyIOPerSec) {
        if (this.lastAverageEnergyIOPerTick != energyIOPerSec) {
            this.lastAverageEnergyIOPerTick = energyIOPerSec;
        }
    }

    //////////////////////////////////////
    // *********** GUI ***********//

    /// ///////////////////////////////////

    @Override
    public void loadServerUI(Player player, UIContainerMenu<MetaMachine> menu, MetaMachine holder) {
        // TODO implement
        // NEEDS MESSAGES FOR TICKS/EU/AMPS PER CYCLE & ACTIVE STATE, REMEMBER TO ADD THOSE!!
    }

    @Override
    public void loadClientUI(Player player, UIAdapter<StackLayout> adapter, MetaMachine holder) {
        adapter.rootComponent.child(UIContainers.verticalFlow(Sizing.fixed(176), Sizing.fixed(166))
                .gap(8)
                .<FlowLayout>configure(c -> {
                    c.surface(Surface.UI_BACKGROUND)
                            .padding(Insets.both(7, 32));
                })
                .child(UIComponents.label(Component.translatable("gtceu.creative.energy.voltage")))
                .child(UIComponents.textBox(Sizing.fixed(152))
                        .textSupplier(() -> String.valueOf(voltage))
                        .<TextBoxComponent>configure(c -> {
                            c.onChanged().subscribe(value -> {
                                voltage = Long.parseLong(value);
                                setTier = GTUtil.getTierByVoltage(voltage);
                            });
                        })
                        .verticalSizing(Sizing.fixed(16)))
                .child(UIComponents.label(Component.translatable("gtceu.creative.energy.amperage")))
                .child(UIComponents.button(Component.literal("-"), cd -> amps = --amps == -1 ? 0 : amps)
                        .renderer(ButtonComponent.Renderer.texture(GuiTextures.VANILLA_BUTTON))
                        .positioning(Positioning.absolute(0, 55))
                        .sizing(Sizing.fixed(20)))
                .child(UIComponents.textBox(Sizing.fixed(114))
                        .textSupplier(() -> String.valueOf(amps))
                        .<TextBoxComponent>configure(c -> {
                            c.onChanged().subscribe(value -> amps = Integer.parseInt(value));
                        }).numbersOnly(0, Integer.MAX_VALUE)
                        .positioning(Positioning.absolute(24, 53))
                        .verticalSizing(Sizing.fixed(16)))
                .child(UIComponents.button(Component.literal("-"), cd -> {
                            if (amps < Integer.MAX_VALUE) {
                                amps++;
                            }
                        }).renderer(ButtonComponent.Renderer.texture(GuiTextures.VANILLA_BUTTON))
                        .positioning(Positioning.absolute(142, 55))
                        .sizing(Sizing.fixed(20)))
                // FIXME MAKE TRANSLATABLE
                .child(UIComponents.label(() -> Component
                        .translatable("Average Energy I/O per tick: " + this.lastAverageEnergyIOPerTick)))
                .child(UIComponents.switchComponent((clickData, value) -> active = value)
                        .texture(UITextures.group(GuiTextures.VANILLA_BUTTON, UITextures.text(Component.translatable(
                                        "gtceu.creative.activity.off"))),
                                UITextures.group(GuiTextures.VANILLA_BUTTON, UITextures.text(Component.translatable(
                                        "gtceu.creative.activity.on"))))
                        .pressed(active)
                        .positioning(Positioning.absolute(0, 107))
                        .sizing(Sizing.fixed(77), Sizing.fixed(20)))
                .child(UIComponents.switchComponent((clickData, value) -> {
                            // TODO send message to server :)
                            source = value;
                            if (source) {
                                voltage = 0;
                                amps = 0;
                                setTier = 0;
                            } else {
                                voltage = GTValues.V[14];
                                amps = Integer.MAX_VALUE;
                                setTier = 14;
                            }
                        })
                        .texture(UITextures.group(GuiTextures.VANILLA_BUTTON, UITextures.text(Component.translatable(
                                        "gtceu.creative.energy.sink"))),
                                UITextures.group(GuiTextures.VANILLA_BUTTON, UITextures.text(Component.translatable(
                                        "gtceu.creative.energy.source"))))
                        .pressed(source)
                        .positioning(Positioning.absolute(78, 107))
                        .sizing(Sizing.fixed(77), Sizing.fixed(20)))
                .child(UIComponents.dropdown(Sizing.fixed(20))
                        .<DropdownComponent>configure(c -> {
                            for (String tierName : GTValues.VNF) {
                                c.button(Component.literal(tierName),
                                        (clickData, dropdownComponent) -> {
                                            setTier = ArrayUtils.indexOf(GTValues.VNF, tierName);
                                            voltage = GTValues.VEX[setTier];
                                        });
                            }
                        })
                        .closeWhenNotHovered(true)
                        .surface(Surface.flat(Color.BLACK.argb()))
                        .positioning(Positioning.absolute(0, -25))));
    }

}
