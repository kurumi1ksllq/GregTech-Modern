package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.DiodePartMachine;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class DiodeBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if(blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            MetaMachine machine = blockEntity.getMetaMachine();
            if (machine instanceof DiodePartMachine diode) {
                CompoundTag tag = blockAccessor.getServerData();
                int voltage = tag.getInt("voltage");
                int amp = tag.getInt("amp");

                if(blockAccessor.getHitResult().getDirection() == Direction.from3DDataValue(tag.getInt("side"))) {
                    iTooltip.add(Component.translatable("gtceu.top.diode_output", GTValues.VNF[voltage], amp));
                } else {
                    iTooltip.add(Component.translatable("gtceu.top.diode_input", GTValues.VNF[voltage], amp));
                }
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if(blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            MetaMachine machine = blockEntity.getMetaMachine();
            if (machine instanceof DiodePartMachine diode) {
                compoundTag.putInt("side", diode.getFrontFacing().get3DDataValue());
                compoundTag.putInt("amp", diode.getFrontFacing().get3DDataValue());
                compoundTag.putInt("voltage", diode.getTier());
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return GTCEu.id("diode");
    }
}
