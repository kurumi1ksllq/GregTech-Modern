package com.gregtechceu.gtceu.common.pipelike.handlers;

import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.IPipeNetNodeHandler;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeStructure;
import com.gregtechceu.gtceu.common.pipelike.block.duct.DuctStructure;
import com.gregtechceu.gtceu.common.pipelike.net.duct.WorldDuctNet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class DuctNetHandler implements IPipeNetNodeHandler {

    public static final DuctNetHandler INSTANCE = new DuctNetHandler();

    @Override
    public @NotNull Collection<WorldPipeNode> getOrCreateFromNets(ServerLevel world, BlockPos pos,
                                                                  IPipeStructure structure) {
        if (structure instanceof DuctStructure duct) {
            WorldPipeNode node = WorldDuctNet.getWorldNet(world).getOrCreateNode(pos);
            duct.mutateData(node.getData());
            return Collections.singletonList(node);
        }
        return Collections.emptyList();
    }

    @Override
    public @NotNull Collection<WorldPipeNode> getFromNets(ServerLevel world, BlockPos pos,
                                                          IPipeStructure structure) {
        if (structure instanceof DuctStructure duct) {
            WorldPipeNode node = WorldDuctNet.getWorldNet(world).getNode(pos);
            if (node != null) return Collections.singletonList(node);
        }
        return Collections.emptyList();
    }

    @Override
    public void removeFromNets(ServerLevel world, BlockPos pos, IPipeStructure structure) {
        if (structure instanceof DuctStructure) {
            WorldDuctNet net = WorldDuctNet.getWorldNet(world);
            NetNode node = net.getNode(pos);
            if (node != null) net.removeNode(node);
        }
    }

    @Override
    public void addInformation(@NotNull ItemStack stack, BlockGetter worldIn, @NotNull List<Component> tooltip,
                               @NotNull TooltipFlag flagIn, IPipeStructure structure) {
        tooltip.add(Component.translatable("block.gtceu.normal_optical_pipe.tooltip"));
    }
}
