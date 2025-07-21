package com.gregtechceu.gtceu.api.graphnet.pipenet.logic;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.graphnet.MultiNodeHelper;
import com.gregtechceu.gtceu.api.graphnet.logic.INetLogicEntryListener;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicEntry;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicType;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IBurnable;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IFreezable;
import com.gregtechceu.gtceu.client.particle.GTOverheatParticle;
import com.gregtechceu.gtceu.utils.TickTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

@Accessors(chain = true)
public final class TemperatureLogic extends NetLogicEntry<TemperatureLogic, CompoundTag> {

    public static final TemperatureLogicType TYPE = new TemperatureLogicType();

    public static final int DEFAULT_TEMPERATURE = 298;

    private WeakReference<INetLogicEntryListener> netListener;
    private boolean isMultiNodeHelper = false;

    @Getter
    @Setter
    private int temperatureMaximum;
    @Getter
    @Setter
    private int partialBurnTemperature = -1;
    @Getter
    @Setter
    private int temperatureMinimum;
    @Getter
    @Setter
    private float thermalEnergy;
    @Getter
    @Setter
    private int thermalMass;

    @Getter
    @Setter
    private @NotNull TemperatureLossFunction restorationFunction = new TemperatureLossFunction();
    @Getter
    @Setter
    private int functionPriority;
    @Getter
    private long lastRestorationTick;

    @Override
    public @NotNull TemperatureLogicType getType() {
        return TYPE;
    }

    @Contract("_ -> this")
    public TemperatureLogic setInitialThermalEnergy(float energy) {
        this.thermalEnergy = energy;
        return this;
    }

    @Override
    public void registerToMultiNodeHelper(MultiNodeHelper helper) {
        this.isMultiNodeHelper = true;
        this.netListener = new WeakReference<>(helper);
    }

    @Override
    public void registerToNetLogicData(NetLogicData data) {
        if (!isMultiNodeHelper) this.netListener = new WeakReference<>(data);
    }

    @Override
    public void deregisterFromNetLogicData(NetLogicData data) {
        if (!isMultiNodeHelper) this.netListener = new WeakReference<>(null);
    }

    public @NotNull TemperatureLogic getNew() {
        return new TemperatureLogic();
    }

    public boolean isOverMaximum(int temperature) {
        return temperature > getTemperatureMaximum();
    }

    public boolean isOverPartialBurnThreshold(int temperature) {
        int partial = getPartialBurnTemperature();
        return partial > 0 && temperature > getPartialBurnTemperature();
    }

    public boolean isUnderMinimum(int temperature) {
        return temperature < getTemperatureMinimum();
    }

    public void defaultHandleTemperature(Level world, BlockPos pos) {
        int temp = getTemperature(TickTracker.getTick());
        if (isUnderMinimum(temp)) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof IFreezable freezable) {
                freezable.fullyFreeze(state, world, pos);
            } else {
                world.removeBlock(pos, false);
            }
        } else if (isOverMaximum(temp)) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof IBurnable burnable) {
                burnable.fullyBurn(state, world, pos);
            } else {
                world.removeBlock(pos, false);
            }
        } else if (isOverPartialBurnThreshold(temp)) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof IBurnable burnable) {
                burnable.partialBurn(state, world, pos);
            }
        }
    }

    public void applyThermalEnergy(float energy, long tick) {
        restoreTemperature(tick);
        this.thermalEnergy += energy;
        // since the decay logic is synced and deterministic,
        // the only time client and server will desync is on external changes.
        INetLogicEntryListener listener = this.netListener.get();
        if (listener != null) listener.markLogicEntryAsUpdated(this, false);
    }

    public void moveTowardsTemperature(int temperature, long tick, float mult, boolean noParticle) {
        int temp = getTemperature(tick);
        float thermalEnergy = (float) (this.thermalMass * (temperature - temp) *
                (1 - Math.pow(0.5, mult / this.thermalMass)));
        if (noParticle) {
            float thermalMax = this.thermalMass * (GTOverheatParticle.TEMPERATURE_CUTOFF - DEFAULT_TEMPERATURE);
            if (thermalEnergy + this.thermalEnergy > thermalMax) {
                thermalEnergy = thermalMax - this.thermalEnergy;
            }
        }
        applyThermalEnergy(thermalEnergy, tick);
    }

    public int getTemperature(long tick) {
        restoreTemperature(tick);
        return (int) (this.thermalEnergy / this.thermalMass) + DEFAULT_TEMPERATURE;
    }

    private void restoreTemperature(long tick) {
        long timePassed = tick - lastRestorationTick;
        // sometimes the tick time randomly warps backward for no explicable reason, on both server and client.
        if (timePassed > 0) {
            float energy = this.thermalEnergy;
            this.lastRestorationTick = tick;
            if (timePassed >= Integer.MAX_VALUE) {
                this.thermalEnergy = 0;
            } else this.thermalEnergy = restorationFunction.restoreTemperature(energy, (int) timePassed);
        }
    }

    @Override
    public boolean mergedToMultiNodeHelper() {
        return true;
    }

    @Override
    public void merge(NetNode otherOwner, NetLogicEntry<?, ?> unknown) {
        if (!(unknown instanceof TemperatureLogic other)) {
            return;
        }
        if (other.getTemperatureMinimum() > this.getTemperatureMinimum()) {
            this.setTemperatureMinimum(other.getTemperatureMinimum());
        }
        if (other.getTemperatureMaximum() < this.getTemperatureMaximum()) {
            this.setTemperatureMaximum(other.getTemperatureMaximum());
        }
        // since merge also occurs during nbt load, ignore the other's thermal energy.
        if (other.getThermalMass() < this.getThermalMass()) {
            this.setThermalMass(other.getThermalMass());
        }
        if (other.getFunctionPriority() > this.getFunctionPriority()) {
            this.setRestorationFunction(other.getRestorationFunction());
            this.setFunctionPriority(other.getFunctionPriority());
        }
        if (other.getPartialBurnTemperature() < this.getPartialBurnTemperature()) {
            this.setPartialBurnTemperature(other.getPartialBurnTemperature());
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("ThermalEnergy", this.thermalEnergy);
        tag.putInt("TemperatureMax", this.temperatureMaximum);
        tag.putInt("TemperatureMin", this.temperatureMinimum);
        tag.putInt("ThermalMass", this.thermalMass);
        tag.put("RestorationFunction", this.restorationFunction.serializeNBT());
        tag.putInt("FunctionPrio", this.functionPriority);
        if (partialBurnTemperature != -1) tag.putInt("PartialBurn", partialBurnTemperature);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.thermalEnergy = nbt.getFloat("ThermalEnergy");
        this.temperatureMaximum = nbt.getInt("TemperatureMax");
        this.temperatureMinimum = nbt.getInt("TemperatureMin");
        this.thermalMass = nbt.getInt("ThermalMass");
        this.restorationFunction = new TemperatureLossFunction(nbt.getCompound("RestorationFunction"));
        this.functionPriority = nbt.getInt("FunctionPrio");
        if (nbt.contains("PartialBurn")) {
            this.partialBurnTemperature = nbt.getInt("PartialBurn");
        } else {
            this.partialBurnTemperature = -1;
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf, boolean fullChange) {
        buf.writeFloat(this.thermalEnergy);
        if (fullChange) {
            buf.writeVarInt(this.temperatureMaximum);
            buf.writeVarInt(this.temperatureMinimum);
            buf.writeVarInt(this.thermalMass);
            this.restorationFunction.encode(buf);
            buf.writeVarInt(this.functionPriority);
            // laughs in java 9
            // noinspection ReplaceNullCheck
            if (this.partialBurnTemperature == -1) {
                buf.writeVarInt(-1);
            } else {
                buf.writeVarInt(this.partialBurnTemperature);
            }
        }
    }

    @Override
    public void decode(FriendlyByteBuf buf, boolean fullChange) {
        this.thermalEnergy = buf.readFloat();
        if (fullChange) {
            this.temperatureMaximum = buf.readVarInt();
            this.temperatureMinimum = buf.readVarInt();
            this.thermalMass = buf.readVarInt();
            this.restorationFunction.decode(buf);
            this.functionPriority = buf.readVarInt();
            this.partialBurnTemperature = buf.readVarInt();
        }
    }

    public static class TemperatureLogicType extends NetLogicType<TemperatureLogic> {

        public TemperatureLogicType() {
            super(GTCEu.id("temperature"), TemperatureLogic::new, new TemperatureLogic());
        }

        public TemperatureLogic getWith(@NotNull TemperatureLossFunction temperatureRestorationFunction,
                                        int temperatureMaximum) {
            return getWith(temperatureRestorationFunction, temperatureMaximum, 1);
        }

        public TemperatureLogic getWith(@NotNull TemperatureLossFunction temperatureRestorationFunction,
                                        int temperatureMaximum, int temperatureMinimum) {
            return getWith(temperatureRestorationFunction, temperatureMaximum, temperatureMinimum, 1000);
        }

        public TemperatureLogic getWith(@NotNull TemperatureLossFunction temperatureRestorationFunction,
                                        int temperatureMaximum, int temperatureMinimum, int thermalMass) {
            return getWith(temperatureRestorationFunction, temperatureMaximum, temperatureMinimum, thermalMass, -1);
        }

        public TemperatureLogic getWith(@NotNull TemperatureLossFunction temperatureRestorationFunction,
                                        int temperatureMaximum, int temperatureMinimum, int thermalMass,
                                        int partialBurnTemperature) {
            return getWith(temperatureRestorationFunction, temperatureMaximum, temperatureMinimum, thermalMass,
                    partialBurnTemperature, 0);
        }

        public TemperatureLogic getWith(@NotNull TemperatureLossFunction temperatureRestorationFunction,
                                        int temperatureMaximum, int temperatureMinimum, int thermalMass,
                                        int partialBurnTemperature, int functionPriority) {
            return getNew()
                    .setRestorationFunction(temperatureRestorationFunction)
                    .setTemperatureMaximum(temperatureMaximum)
                    .setTemperatureMinimum(temperatureMinimum)
                    .setThermalMass(thermalMass)
                    .setPartialBurnTemperature(partialBurnTemperature)
                    .setFunctionPriority(functionPriority);
        }
    }
}
