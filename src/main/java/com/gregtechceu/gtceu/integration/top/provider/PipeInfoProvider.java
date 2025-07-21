package com.gregtechceu.gtceu.integration.top.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;
import com.gregtechceu.gtceu.api.graphnet.pipenet.logic.TemperatureLogic;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block.PipeBlock;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.FluidTestObject;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.ItemTestObject;
import com.gregtechceu.gtceu.common.pipelike.net.energy.EnergyFlowData;
import com.gregtechceu.gtceu.common.pipelike.net.energy.EnergyFlowLogic;
import com.gregtechceu.gtceu.common.pipelike.net.fluid.FluidFlowLogic;
import com.gregtechceu.gtceu.common.pipelike.net.item.ItemFlowLogic;
import com.gregtechceu.gtceu.integration.top.element.FluidStackElement;
import com.gregtechceu.gtceu.integration.top.element.FluidStyle;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.TickTracker;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import mcjty.theoneprobe.api.*;

public class PipeInfoProvider implements IProbeInfoProvider {

    @Override
    public ResourceLocation getID() {
        return GTCEu.id("pipe");
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level,
                             BlockState state, IProbeHitData hitData) {
        if (state.getBlock() instanceof PipeBlock pipe) {
            PipeBlockEntity blockEntity = pipe.getBlockEntity(level, hitData.getPos());
            if (blockEntity != null) {
                for (NetLogicData data : blockEntity.getNetLogicDatas().values()) {
                    TemperatureLogic temp = data.getLogicEntryNullable(TemperatureLogic.TYPE);
                    if (temp != null) {
                        addTemperatureInformation(mode, info, player, hitData, temp);
                    }
                    EnergyFlowLogic energy = data.getLogicEntryNullable(EnergyFlowLogic.TYPE);
                    if (energy != null) {
                        addEnergyFlowInformation(mode, info, player, hitData, energy);
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

    private void addEnergyFlowInformation(ProbeMode mode, IProbeInfo iProbeInfo, Player player,
                                          IProbeHitData hitData, EnergyFlowLogic logic) {
        if (!logic.getSum(true).isEmpty()) {
            iProbeInfo.text(Component.translatable("gtceu.top.pipe.energy"));
            for (var entry : logic.getSum(true).entrySet()) {
                String voltage = FormattingUtil.formatNumbers(entry.getKey());
                String tier = GTValues.VNF[GTUtil.getTierByVoltage(entry.getKey())];
                String amperage = FormattingUtil.formatNumbers(entry.getValue() / EnergyFlowLogic.MEMORY_TICKS);
                iProbeInfo.text(Component.translatable("gtceu.top.pipe.energy_per", voltage, tier, amperage));
            }
        }
        EnergyFlowData last = logic.getLast();
        if (last != null) {
            String voltage = FormattingUtil.formatNumbers(last.voltage());
            String tier = GTValues.VNF[GTUtil.getTierByVoltage(last.voltage())];
            String amperage = FormattingUtil.formatNumbers(last.amperage());
            iProbeInfo.text(Component.translatable("gtceu.top.pipe.energy_last", voltage, tier, amperage));
        }
    }

    private void addFluidFlowInformation(ProbeMode mode, IProbeInfo iProbeInfo, Player player,
                                         IProbeHitData hitData, FluidFlowLogic logic) {
        if (logic.getMemory(true).isEmpty()) {
            FluidStack last = logic.getLast().recombine();
            iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                    .text(CompoundText.create().info(Component.translatable("gtceu.top.pipe.fluid_last")))
                    .element(new FluidStackElement(last, new FluidStyle()))
                    .text(last.getDisplayName());
        }

        Object2LongMap<FluidTestObject> counts = logic.getSum(true);

        for (var entry : counts.object2LongEntrySet()) {
            FluidStack stack = entry.getKey().recombine();
            String value = FormattingUtil.formatNumbers(20 * entry.getLongValue() / FluidFlowLogic.MEMORY_TICKS);
            iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                    .element(new FluidStackElement(stack, new FluidStyle()))
                    .text(" §b" + value + " L/s §f" + stack.getDisplayName());
        }
    }

    private void addItemFlowInformation(ProbeMode mode, IProbeInfo iProbeInfo, Player player,
                                        IProbeHitData hitData, ItemFlowLogic logic) {
        if (logic.getMemory(true).isEmpty()) {
            ItemStack last = logic.getLast().recombine();
            iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                    .text(CompoundText.create().info(Component.translatable("gtceu.top.pipe.item_last")))
                    .item(last)
                    .text(last.getDisplayName());
        }

        Object2LongMap<ItemTestObject> counts = logic.getSum(true);

        for (var entry : counts.object2LongEntrySet()) {
            ItemStack stack = entry.getKey().recombine();
            String value = FormattingUtil.formatNumbers(20 * entry.getLongValue() / ItemFlowLogic.MEMORY_TICKS);
            iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                    .item(stack)
                    .text(" §b" + value + " /s §f" + stack.getDisplayName());
        }
    }

    private void addTemperatureInformation(ProbeMode mode, IProbeInfo iProbeInfo, Player player,
                                           IProbeHitData hitData, TemperatureLogic logic) {
        iProbeInfo.horizontal(iProbeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER))
                .text(CompoundText.create().info(Component.translatable("gtceu.top.pipe.temperature")))
                .text(" " + ChatFormatting.RED + logic.getTemperature(TickTracker.getTick()) + "K");
    }
}
