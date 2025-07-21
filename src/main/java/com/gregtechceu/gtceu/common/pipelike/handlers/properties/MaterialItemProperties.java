package com.gregtechceu.gtceu.common.pipelike.handlers.properties;

import com.gregtechceu.gtceu.api.data.chemical.material.properties.MaterialProperties;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PipeNetProperties;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.graphnet.logic.ChannelCountLogic;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;
import com.gregtechceu.gtceu.api.graphnet.logic.ThroughputLogic;
import com.gregtechceu.gtceu.api.graphnet.logic.WeightFactorLogic;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeMaterialStructure;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeStructure;
import com.gregtechceu.gtceu.common.pipelike.block.pipe.MaterialPipeStructure;
import com.gregtechceu.gtceu.common.pipelike.net.item.WorldItemNet;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MaterialItemProperties implements PipeNetProperties.IPipeNetMaterialProperty {

    public static final MaterialPropertyKey<MaterialItemProperties> KEY = new MaterialPropertyKey<>("item_properties");

    private final long baseItemsPer10Ticks;
    private final float priority;

    public MaterialItemProperties(long baseItemsPer10Ticks, float priority) {
        this.baseItemsPer10Ticks = baseItemsPer10Ticks;
        this.priority = priority;
    }

    public static MaterialItemProperties create(long baseThroughput) {
        return new MaterialItemProperties(baseThroughput, 2048f / baseThroughput);
    }

    @Override
    public MaterialPropertyKey<?> getKey() {
        return KEY;
    }

    @Override
    public void addInformation(@NotNull ItemStack stack, BlockGetter worldIn, @NotNull List<Component> tooltip,
                               @NotNull TooltipFlag flagIn, IPipeMaterialStructure structure) {
        tooltip.add(Component.translatable("gtceu.pipe.item_pipe"));
        long items = getThroughput(structure);
        if (items % 32 != 0) {
            tooltip.add(Component.translatable("gtceu.universal.tooltip.item_transfer_rate", items * 2));
        } else {
            tooltip.add(Component.translatable("gtceu.universal.tooltip.item_transfer_rate_stacks", items / 32));
        }
        tooltip.add(Component.translatable("gtceu.pipe.priority",
                FormattingUtil.formatNumbers(getFlowPriority(structure))));
    }

    private long getThroughput(IPipeStructure structure) {
        if (structure instanceof MaterialPipeStructure pipe) {
            return baseItemsPer10Ticks * pipe.material();
        } else {
            return baseItemsPer10Ticks;
        }
    }

    @Override
    public void verifyProperty(MaterialProperties properties) {
        if (!properties.hasProperty(PropertyKey.WOOD)) {
            properties.ensureSet(PropertyKey.INGOT, true);
        }
    }

    @Override
    @Nullable
    public WorldPipeNode getOrCreateFromNet(ServerLevel world, BlockPos pos, IPipeStructure structure) {
        if (structure instanceof MaterialPipeStructure) {
            WorldPipeNode node = WorldItemNet.getWorldNet(world).getOrCreateNode(pos);
            mutateData(node.getData(), structure);
            return node;
        }
        return null;
    }

    @Override
    public void mutateData(NetLogicData data, IPipeStructure structure) {
        if (structure instanceof MaterialPipeStructure pipe) {
            long throughput = baseItemsPer10Ticks * pipe.material();
            data.setLogicEntry(WeightFactorLogic.TYPE.getWith(getFlowPriority(structure)))
                    .setLogicEntry(ThroughputLogic.TYPE.getWith(throughput));
            if (pipe.channelCount() > 1) {
                data.setLogicEntry(ChannelCountLogic.TYPE.getWith(pipe.channelCount()));
            }
        }
    }

    private double getFlowPriority(IPipeStructure structure) {
        if (structure instanceof MaterialPipeStructure pipe) {
            return priority * (pipe.restrictive() ? 100d : 1d) * pipe.channelCount() / pipe.material();
        } else return priority;
    }

    @Override
    public @Nullable WorldPipeNode getFromNet(ServerLevel world, BlockPos pos, IPipeStructure structure) {
        if (structure instanceof MaterialPipeStructure) {
            return WorldItemNet.getWorldNet(world).getNode(pos);
        } else {
            return null;
        }
    }

    @Override
    public void removeFromNet(ServerLevel world, BlockPos pos, IPipeStructure structure) {
        if (structure instanceof MaterialPipeStructure) {
            WorldItemNet net = WorldItemNet.getWorldNet(world);
            NetNode node = net.getNode(pos);
            if (node != null) net.removeNode(node);
        }
    }

    @Override
    public boolean generatesStructure(IPipeStructure structure) {
        return structure.getClass() == MaterialPipeStructure.class;
    }

    @Override
    public boolean supportsStructure(IPipeStructure structure) {
        return structure instanceof MaterialPipeStructure;
    }
}
