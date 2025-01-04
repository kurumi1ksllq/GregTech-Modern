package com.gregtechceu.gtceu.integration.top.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block.PipeBlock;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.FluidTestObject;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.ItemTestObject;
import com.gregtechceu.gtceu.common.pipelike.block.cable.CableBlock;
import com.gregtechceu.gtceu.common.pipelike.handlers.properties.MaterialEnergyProperties;
import com.gregtechceu.gtceu.common.pipelike.net.energy.EnergyFlowData;
import com.gregtechceu.gtceu.common.pipelike.net.energy.EnergyFlowLogic;
import com.gregtechceu.gtceu.common.pipelike.net.fluid.FluidFlowLogic;
import com.gregtechceu.gtceu.common.pipelike.net.item.ItemFlowLogic;
import com.gregtechceu.gtceu.integration.top.element.FluidStackElement;
import com.gregtechceu.gtceu.integration.top.element.FluidStyle;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import mcjty.theoneprobe.api.*;

import static com.gregtechceu.gtceu.utils.FormattingUtil.DECIMAL_FORMAT_1F;

public class PipeInfoProvider implements IProbeInfoProvider {

    @Override
    public ResourceLocation getID() {
        return GTCEu.id("pipe");
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level,
                             BlockState state, IProbeHitData hitData) {
        if (state.getBlock() instanceof PipeBlock pipe) {
            PipeBlockEntity tile = pipe.getBlockEntity(level, hitData.getPos());
            if (tile != null) {
                for (NetLogicData data : tile.getNetLogicDatas().values()) {
                    EnergyFlowLogic energy = data.getLogicEntryNullable(EnergyFlowLogic.TYPE);
                    if (energy != null) {
                        addEnergyFlowInformation(mode, info, player, state, hitData, energy);
                    }
                    FluidFlowLogic fluid = data.getLogicEntryNullable(FluidFlowLogic.TYPE);
                    if (fluid != null) {
                        addFluidFlowInformation(mode, info, player, hitData, fluid);
                    }
                    ItemFlowLogic item = data.getLogicEntryNullable(ItemFlowLogic.TYPE);
                    if (item != null) {
                        addItemFlowInformation(mode, info, player, hitData, item);
                    }
                }
            }
        }
    }

    private void addEnergyFlowInformation(ProbeMode probeMode, IProbeInfo iProbeInfo,
                                          Player entityPlayer, BlockState state,
                                          IProbeHitData iProbeHitData, EnergyFlowLogic logic) {
        long cumulativeVoltage = 0;
        long cumulativeAmperage = 0;
        for (var memory : logic.getMemory().values()) {
            int count = 0;
            double voltage = 0;
            long amperage = 0;
            for (EnergyFlowData flow : memory) {
                count++;
                long prev = amperage;
                amperage += flow.amperage();
                // weighted average
                voltage = voltage * prev / amperage + (double) (flow.voltage() * flow.amperage()) / amperage;
            }
            if (count != 0) {
                cumulativeVoltage += voltage / count;
                cumulativeAmperage += amperage / count;
            }
        }

        if (state.getBlock() instanceof CableBlock cableBlock) {

            iProbeInfo.text(CompoundText.create().info(Component.translatable("gtceu.top.cable_voltage"))
                    .important(Component.literal(String.valueOf(cumulativeVoltage / EnergyFlowLogic.MEMORY_TICKS)))
                    .text(" / ")
                    .important(Component.literal(GTValues.VNF[GTUtil.getTierByVoltage(
                            cableBlock.material.getProperty(PropertyKey.PIPENET_PROPERTIES)
                                    .getProperty(MaterialEnergyProperties.KEY).getVoltageLimit())])));
            iProbeInfo.text(CompoundText.create().info(Component.translatable("gtceu.top.cable_amperage"))
                    .important(Component.literal(String.valueOf(cumulativeAmperage / EnergyFlowLogic.MEMORY_TICKS)))
                    .text(" / ")
                    .important(Component.literal(DECIMAL_FORMAT_1F.format(
                            cableBlock.material.getProperty(PropertyKey.PIPENET_PROPERTIES)
                                    .getProperty(MaterialEnergyProperties.KEY).getAmperage(cableBlock.getStructure())) +
                            "A")));
        } else {
            iProbeInfo.text(CompoundText.create().info(Component.translatable("gtceu.top.cable_voltage"))
                    .important(Component.literal(String.valueOf(cumulativeVoltage / EnergyFlowLogic.MEMORY_TICKS))));
            iProbeInfo.text(CompoundText.create().info(Component.translatable("gtceu.top.cable_amperage"))
                    .important(Component.literal(String.valueOf(cumulativeAmperage / EnergyFlowLogic.MEMORY_TICKS))));
        }
    }

    private void addFluidFlowInformation(ProbeMode probeMode, IProbeInfo iProbeInfo, Player entityPlayer,
                                         IProbeHitData iProbeHitData, FluidFlowLogic logic) {
        if (logic.getMemory().isEmpty()) {
            iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                    .text(CompoundText.create().info(Component.translatable("gtceu.top.pipe.fluid_last")))
                    .element(new FluidStackElement(logic.getLast(), new FluidStyle()))
                    .text(logic.getLast().getDisplayName());
        }

        Object2LongMap<FluidTestObject> counts = logic.getSum();

        for (var entry : counts.object2LongEntrySet()) {
            net.minecraftforge.fluids.FluidStack stack = entry.getKey().recombine();
            String value = FormattingUtil.formatNumbers(20 * entry.getLongValue() / FluidFlowLogic.MEMORY_TICKS);
            iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                    .element(new FluidStackElement(stack, new FluidStyle()))
                    .text(" §b" + value + " L/s §f" + stack.getDisplayName());
        }
    }

    private void addItemFlowInformation(ProbeMode probeMode, IProbeInfo iProbeInfo, Player entityPlayer,
                                        IProbeHitData iProbeHitData, ItemFlowLogic logic) {
        if (logic.getMemory().isEmpty()) {
            ItemStack countlessLast = logic.getLast().copy();
            countlessLast.setCount(1);
            iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                    .text(CompoundText.create().info(Component.translatable("gtceu.top.pipe.item_last")))
                    .item(countlessLast)
                    .text(logic.getLast().getDisplayName());
        }

        Object2LongMap<ItemTestObject> counts = logic.getSum();

        for (var entry : counts.object2LongEntrySet()) {
            ItemStack stack = entry.getKey().recombine();
            String value = FormattingUtil.formatNumbers(20 * entry.getLongValue() / ItemFlowLogic.MEMORY_TICKS);
            iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                    .item(stack)
                    .text(" §b" + value + " /s §f" + stack.getDisplayName());
        }
    }
}
