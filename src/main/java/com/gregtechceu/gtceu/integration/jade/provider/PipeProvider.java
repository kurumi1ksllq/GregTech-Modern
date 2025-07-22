package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block.PipeBlock;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.FluidTestObject;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.ItemTestObject;
import com.gregtechceu.gtceu.common.pipelike.net.energy.EnergyFlowData;
import com.gregtechceu.gtceu.common.pipelike.net.energy.EnergyFlowLogic;
import com.gregtechceu.gtceu.common.pipelike.net.fluid.FluidFlowLogic;
import com.gregtechceu.gtceu.common.pipelike.net.item.ItemFlowLogic;
import com.gregtechceu.gtceu.integration.jade.element.FluidStackElement;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

public class PipeProvider extends BlockInfoProvider<PipeBlockEntity> {

    public PipeProvider() {
        super(GTCEu.id("pipe"));
    }

    @Override
    protected PipeBlockEntity getCapability(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipeBlockEntity) {
            return pipeBlockEntity;
        }
        return null;
    }

    @Override
    protected void write(CompoundTag compoundTag, PipeBlockEntity capability, BlockAccessor block) {
        if (capability != null) {
            for (NetLogicData data : capability.getNetLogicDatas().values()) {
                EnergyFlowLogic energy = data.getLogicEntryNullable(EnergyFlowLogic.TYPE);
                if (energy != null) {
                    addEnergyFlowInformation(compoundTag, energy);
                }
                FluidFlowLogic fluid = data.getLogicEntryNullable(FluidFlowLogic.TYPE);
                if (fluid != null) {
                    addFluidFlowInformation(compoundTag, fluid);
                }
                ItemFlowLogic item = data.getLogicEntryNullable(ItemFlowLogic.TYPE);
                if (item != null) {
                    addItemFlowInformation(compoundTag, item);
                }
            }
        }
    }

    @Override
    protected void addTooltip(CompoundTag capData, ITooltip tooltip, Player player, BlockAccessor block,
                              BlockEntity blockEntity, IPluginConfig config) {
        CompoundTag serverData = block.getServerData();
        if (serverData.contains("Energy")) {
            CompoundTag energy = serverData.getCompound("Energy");

            CompoundTag lastEnergy = energy.getCompound("LastEnergy");
            long voltage = lastEnergy.getLong("voltage");
            long amperage = lastEnergy.getLong("amperage");

            String voltageStr = FormattingUtil.formatNumbers(voltage);
            String tier = GTValues.VNF[GTUtil.getTierByVoltage(voltage)];
            String amperageStr = FormattingUtil.formatNumbers(amperage);

            tooltip.add(Component.translatable("gtceu.top.pipe.energy_last", voltageStr, tier, amperageStr));

            ListTag extraEnergy = serverData.getList("ExtraEnergy", Tag.TAG_COMPOUND);
            if (!extraEnergy.isEmpty()) {
                tooltip.add(Component.translatable("gtceu.top.pipe.energy"));
                for (int i = 0; i < extraEnergy.size(); i++) {
                    CompoundTag entry = extraEnergy.getCompound(i);
                    voltage = entry.getLong("voltage");
                    amperage = entry.getLong("amperage");

                    voltageStr = FormattingUtil.formatNumbers(voltage);
                    tier = GTValues.VNF[GTUtil.getTierByVoltage(voltage)];
                    amperageStr = FormattingUtil.formatNumbers(amperage);

                    tooltip.add(Component.translatable("gtceu.top.pipe.energy_per", voltageStr, tier, amperageStr));
                }
            }
        }

        if (serverData.contains("Fluid")) {
            CompoundTag fluid = serverData.getCompound("Fluid");

            FluidStack stack = FluidStack.loadFromTag(fluid.getCompound("LastFluid"));
            tooltip.add(IElementHelper.get().text(Component.translatable("gtceu.top.pipe.fluid_last"))
                    .align(IElement.Align.LEFT));
            tooltip.append(new FluidStackElement(stack, 14, 14));
            tooltip.append(stack.getDisplayName());

            ListTag list = fluid.getList("ExtraFluids", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                stack = FluidStack.loadFromTag(tag.getCompound("Fluid"));
                tooltip.add(new FluidStackElement(stack, 14, 14));
                tooltip.append(Component.literal(String.valueOf(tag.getLong("Amount")))
                        .append(Component.literal(" mB/s "))
                        .append(stack.getDisplayName()));
            }
        }

        if (serverData.contains("Item")) {
            CompoundTag item = serverData.getCompound("Item");

            ItemStack stack = ItemStack.of(item.getCompound("LastItem"));
            tooltip.add(IElementHelper.get().text(Component.translatable("gtceu.top.pipe.item_last"))
                    .align(IElement.Align.LEFT));
            tooltip.append(IElementHelper.get().item(stack));
            tooltip.append(stack.getDisplayName());

            ListTag list = item.getList("ExtraItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                stack = ItemStack.of(tag.getCompound("Item"));
                tooltip.add(IElementHelper.get().item(stack));
                tooltip.append(Component.literal(String.valueOf(tag.getLong("Amount")))
                        .append(Component.literal(" /s "))
                        .append(stack.getDisplayName()));
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlock() instanceof PipeBlock pipe) {
            PipeBlockEntity tile = pipe.getBlockEntity(blockAccessor.getLevel(), blockAccessor.getPosition());
            if (tile != null) {
                for (NetLogicData data : tile.getNetLogicDatas().values()) {
                    EnergyFlowLogic energy = data.getLogicEntryNullable(EnergyFlowLogic.TYPE);
                    if (energy != null) {
                        addEnergyFlowInformation(compoundTag, energy);
                    }
                    FluidFlowLogic fluid = data.getLogicEntryNullable(FluidFlowLogic.TYPE);
                    if (fluid != null) {
                        addFluidFlowInformation(compoundTag, fluid);
                    }
                    ItemFlowLogic item = data.getLogicEntryNullable(ItemFlowLogic.TYPE);
                    if (item != null) {
                        addItemFlowInformation(compoundTag, item);
                    }
                }
            }
        }
    }

    private void addEnergyFlowInformation(CompoundTag tag, EnergyFlowLogic logic) {
        CompoundTag energyTag = new CompoundTag();

        if (!logic.getSum(true).isEmpty()) {
            ListTag list = new ListTag();
            for (var entry : logic.getSum(true).long2LongEntrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putLong("voltage", entry.getLongKey());
                entryTag.putLong("amperage", entry.getLongValue() / EnergyFlowLogic.MEMORY_TICKS);
                list.add(entryTag);
            }
            energyTag.put("ExtraEnergy", list);
        }
        EnergyFlowData last = logic.getLast();
        if (last != null) {
            CompoundTag lastTag = new CompoundTag();
            lastTag.putLong("voltage", last.voltage());
            lastTag.putLong("amperage", last.amperage());
            energyTag.put("LastEnergy", lastTag);
        }
        tag.put("Energy", energyTag);
    }

    private void addFluidFlowInformation(CompoundTag tag, FluidFlowLogic logic) {
        CompoundTag fluid = new CompoundTag();
        fluid.put("LastFluid", logic.getLast().recombine().writeToNBT(new CompoundTag()));

        Object2LongOpenHashMap<FluidTestObject> counts = new Object2LongOpenHashMap<>();
        for (var memory : logic.getMemory().values()) {
            for (var entry : memory.object2LongEntrySet()) {
                counts.merge(entry.getKey(), entry.getLongValue(), Long::sum);
            }
        }

        ListTag extraFluids = new ListTag();
        for (var entry : counts.object2LongEntrySet()) {
            CompoundTag inner = new CompoundTag();
            net.minecraftforge.fluids.FluidStack stack = entry.getKey().recombine();

            inner.put("Fluid", stack.writeToNBT(new CompoundTag()));
            inner.putLong("Amount", entry.getLongValue() * 20 / FluidFlowLogic.MEMORY_TICKS);
            extraFluids.add(inner);
        }
        fluid.put("ExtraFluids", extraFluids);
        tag.put("Fluid", fluid);
    }

    private void addItemFlowInformation(CompoundTag tag, ItemFlowLogic logic) {
        CompoundTag item = new CompoundTag();
        item.put("LastItem", logic.getLast().recombine().save(new CompoundTag()));

        Object2LongOpenHashMap<ItemTestObject> counts = new Object2LongOpenHashMap<>();
        for (var memory : logic.getMemory().values()) {
            for (var entry : memory.object2LongEntrySet()) {
                counts.merge(entry.getKey(), entry.getLongValue(), Long::sum);
            }
        }

        ListTag extraItems = new ListTag();
        for (var entry : counts.object2LongEntrySet()) {
            CompoundTag inner = new CompoundTag();
            ItemStack stack = entry.getKey().recombine();

            inner.put("Item", stack.save(new CompoundTag()));
            inner.putLong("Amount", entry.getLongValue() * 20 / FluidFlowLogic.MEMORY_TICKS);
            extraItems.add(inner);
        }
        item.put("ExtraItems", extraItems);
        tag.put("Item", item);
    }
}
