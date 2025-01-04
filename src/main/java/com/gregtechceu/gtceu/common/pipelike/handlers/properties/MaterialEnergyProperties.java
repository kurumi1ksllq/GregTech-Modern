package com.gregtechceu.gtceu.common.pipelike.handlers.properties;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.MaterialProperties;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PipeNetProperties;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;
import com.gregtechceu.gtceu.api.graphnet.logic.WeightFactorLogic;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.logic.TemperatureLogic;
import com.gregtechceu.gtceu.api.graphnet.pipenet.logic.TemperatureLossFunction;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeMaterialStructure;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeStructure;
import com.gregtechceu.gtceu.common.pipelike.block.cable.CableStructure;
import com.gregtechceu.gtceu.common.pipelike.block.pipe.MaterialPipeStructure;
import com.gregtechceu.gtceu.common.pipelike.net.energy.*;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.Fluid;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_FOIL;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.NO_UNIFICATION;

public final class MaterialEnergyProperties implements PipeNetProperties.IPipeNetMaterialProperty {

    public static final MaterialPropertyKey<MaterialEnergyProperties> KEY = new MaterialPropertyKey<>(
            "EnergyProperties");

    private static final int MINIMUM_MELT_TEMPERATURE = 1500;

    @Getter
    private final long voltageLimit;
    private final long amperageLimit;
    private int materialMeltTemperature;
    private final long lossPerAmp;
    @Getter
    private final boolean superconductor;

    /**
     * Generate a MaterialEnergyProperties
     *
     * @param voltageLimit   the voltage limit for the cable
     * @param amperageLimit  the base amperage for the cable.
     * @param lossPerAmp     the base loss per amp per block traveled.
     * @param superconductor whether the material will be treated as a superconductor. Does not override loss.
     */
    public MaterialEnergyProperties(long voltageLimit, long amperageLimit, long lossPerAmp,
                                    boolean superconductor) {
        this.voltageLimit = voltageLimit;
        this.amperageLimit = amperageLimit;
        this.lossPerAmp = lossPerAmp;
        this.superconductor = superconductor;
    }

    public static MaterialEnergyProperties create(long voltageLimit, long amperageLimit, long lossPerAmp,
                                                  boolean superconductor) {
        return new MaterialEnergyProperties(voltageLimit, amperageLimit, lossPerAmp, superconductor);
    }

    public static MaterialEnergyProperties create(long voltageLimit, long amperageLimit, long lossPerAmp) {
        return new MaterialEnergyProperties(voltageLimit, amperageLimit, lossPerAmp, false);
    }

    public static TagPrefix.MaterialRecipeHandler registrationHandler(TagPrefix.PropertyMaterialRecipeHandler<MaterialEnergyProperties> handler) {
        return (orePrefix, material, provider) -> {
            if (material.hasProperty(PropertyKey.PIPENET_PROPERTIES) && !material.hasFlag(NO_UNIFICATION) &&
                    material.getProperty(PropertyKey.PIPENET_PROPERTIES).hasProperty(KEY)) {
                handler.accept(orePrefix, material,
                        material.getProperty(PropertyKey.PIPENET_PROPERTIES).getProperty(KEY), provider);
            }
        };
    }

    @Override
    public MaterialPropertyKey<?> getKey() {
        return KEY;
    }

    @Override
    public void addInformation(@NotNull ItemStack stack, BlockGetter worldIn, @NotNull List<Component> tooltip,
                               @NotNull TooltipFlag flagIn, IPipeMaterialStructure structure) {
        int tier = GTUtil.getTierByVoltage(voltageLimit);
        if (isSuperconductor())
            tooltip.add(Component.translatable("gtceu.cable.superconductor", GTValues.VN[tier]));
        tooltip.add(Component.translatable("gtceu.cable.voltage", voltageLimit, GTValues.VNF[tier]));
        tooltip.add(Component.translatable("gtceu.cable.amperage", getAmperage(structure)));

        long loss = getLoss(structure);
        tooltip.add(Component.translatable("gtceu.cable.loss_per_block", loss));
    }

    @Override
    public void verifyProperty(MaterialProperties properties) {
        properties.ensureSet(PropertyKey.DUST, true);
        if (properties.hasProperty(PropertyKey.INGOT)) {
            // Ensure all Materials with Cables and voltage tier IV or above have a Foil for recipe generation
            Material thisMaterial = properties.getMaterial();
            if (!isSuperconductor() && voltageLimit >= GTValues.V[GTValues.IV] &&
                    !thisMaterial.hasFlag(GENERATE_FOIL)) {
                thisMaterial.addFlags(GENERATE_FOIL);
            }
        }
        this.materialMeltTemperature = computeMaterialMeltTemperature(properties);
    }

    private static int computeMaterialMeltTemperature(@NotNull MaterialProperties properties) {
        if (properties.hasProperty(PropertyKey.FLUID)) {
            // autodetermine melt temperature from registered fluid
            FluidProperty prop = properties.getProperty(PropertyKey.FLUID);
            Fluid fluid = prop.get(FluidStorageKeys.LIQUID);
            if (fluid == null) {
                FluidBuilder builder = prop.getQueuedBuilder(FluidStorageKeys.LIQUID);
                if (builder != null) {
                    return Math.max(MINIMUM_MELT_TEMPERATURE,
                            builder.getDeterminedTemperature(properties.getMaterial(), FluidStorageKeys.LIQUID));
                }
            } else {
                return Math.max(MINIMUM_MELT_TEMPERATURE, fluid.getFluidType().getTemperature());
            }
        }
        return MINIMUM_MELT_TEMPERATURE;
    }

    @Override
    @Nullable
    public WorldPipeNode getOrCreateFromNet(ServerLevel world, BlockPos pos, IPipeStructure structure) {
        if (structure instanceof CableStructure) {
            WorldPipeNode node = WorldEnergyNet.getWorldNet(world).getOrCreateNode(pos);
            mutateData(node.getData(), structure);
            return node;
        } else if (structure instanceof MaterialPipeStructure pipe) {
            long amperage = amperageLimit * pipe.material() / 2;
            if (amperage == 0) return null; // skip pipes that are too small
            WorldPipeNode node = WorldEnergyNet.getWorldNet(world).getOrCreateNode(pos);
            mutateData(node.getData(), pipe);
            return node;
        }
        return null;
    }

    @Override
    public void mutateData(NetLogicData data, IPipeStructure structure) {
        if (structure instanceof CableStructure cable) {
            long loss = getLoss(structure);
            long amperage = getAmperage(structure);
            boolean insulated = cable.partialBurnStructure() != null;
            // insulated cables cool down half as fast
            float coolingFactor = (float) (Math.sqrt(cable.material()) / (insulated ? 8 : 4));
            TemperatureLogic existing = data.getLogicEntryNullable(TemperatureLogic.TYPE);
            float energy = existing == null ? 0 : existing.getThermalEnergy();
            data.setLogicEntry(VoltageLossLogic.TYPE.getWith(loss))
                    .setLogicEntry(WeightFactorLogic.TYPE.getWith(loss + 0.001 / amperage))
                    .setLogicEntry(AmperageLimitLogic.TYPE.getWith(amperage))
                    .setLogicEntry(VoltageLimitLogic.TYPE.getWith(voltageLimit))
                    .setLogicEntry(TemperatureLogic.TYPE
                            .getWith(TemperatureLossFunction.getOrCreateCable(coolingFactor), materialMeltTemperature,
                                    1,
                                    100 * cable.material(), cable.partialBurnThreshold())
                            .setInitialThermalEnergy(energy));
            if (superconductor) {
                data.setLogicEntry(SuperconductorLogic.TYPE.getNew());
            }
        } else if (structure instanceof MaterialPipeStructure pipe) {
            long amperage = getAmperage(structure);
            if (amperage == 0) return; // skip pipes that are too small
            long loss = getLoss(structure);
            float coolingFactor = (float) Math.sqrt((double) pipe.material() / (4 + pipe.channelCount()));
            TemperatureLogic existing = data.getLogicEntryNullable(TemperatureLogic.TYPE);
            float energy = existing == null ? 0 : existing.getThermalEnergy();
            data.setLogicEntry(VoltageLossLogic.TYPE.getWith(loss))
                    .setLogicEntry(WeightFactorLogic.TYPE.getWith(loss + 0.001 / amperage))
                    .setLogicEntry(AmperageLimitLogic.TYPE.getWith(amperage))
                    .setLogicEntry(VoltageLimitLogic.TYPE.getWith(voltageLimit))
                    .setLogicEntry(TemperatureLogic.TYPE
                            .getWith(TemperatureLossFunction.getOrCreatePipe(coolingFactor), materialMeltTemperature, 1,
                                    50 * pipe.material(), null)
                            .setInitialThermalEnergy(energy));
            if (superconductor) {
                data.setLogicEntry(SuperconductorLogic.TYPE.getNew());
            }
        }
    }

    private long getLoss(IPipeStructure structure) {
        if (structure instanceof CableStructure cable) {
            return lossPerAmp * cable.costFactor();
        } else if (structure instanceof MaterialPipeStructure pipe) {
            return lossPerAmp * (pipe.material() > 6 ? 3 : 2);
        } else return lossPerAmp;
    }

    public long getAmperage(IPipeStructure structure) {
        if (structure instanceof CableStructure cable) {
            return amperageLimit * cable.material();
        } else if (structure instanceof MaterialPipeStructure pipe) {
            return amperageLimit * pipe.material() / 2;
        } else return amperageLimit;
    }

    @Override
    @Nullable
    public WorldPipeNode getFromNet(ServerLevel world, BlockPos pos, IPipeStructure structure) {
        if (structure instanceof CableStructure || structure instanceof MaterialPipeStructure)
            return WorldEnergyNet.getWorldNet(world).getNode(pos);
        else return null;
    }

    @Override
    public void removeFromNet(ServerLevel world, BlockPos pos, IPipeStructure structure) {
        if (structure instanceof CableStructure || structure instanceof MaterialPipeStructure) {
            WorldEnergyNet net = WorldEnergyNet.getWorldNet(world);
            NetNode node = net.getNode(pos);
            if (node != null) net.removeNode(node);
        }
    }

    @Override
    public boolean generatesStructure(IPipeStructure structure) {
        return structure instanceof CableStructure cable && (!isSuperconductor() || !cable.isInsulated());
    }

    @Override
    public boolean supportsStructure(IPipeStructure structure) {
        return structure instanceof CableStructure || structure instanceof MaterialPipeStructure;
    }
}
