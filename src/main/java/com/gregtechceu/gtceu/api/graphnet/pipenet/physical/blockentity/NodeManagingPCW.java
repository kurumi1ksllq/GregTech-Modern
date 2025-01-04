package com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity;

import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeCapConnectionNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.utils.FacingPos;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

public class NodeManagingPCW extends PipeCapabilityWrapper {

    private final EnumMap<Direction, WorldPipeCapConnectionNode> managed = new EnumMap<>(Direction.class);

    public NodeManagingPCW(@NotNull PipeBlockEntity owner, @NotNull WorldPipeNode node,
                           Object2ObjectMap<Capability<?>, IPipeCapabilityObject> capabilities, int inactiveKey,
                           int activeKey) {
        super(owner, node, capabilities, inactiveKey, activeKey);
    }

    @Override
    public void invalidate() {
        for (WorldPipeCapConnectionNode n : managed.values()) {
            n.getNet().removeNode(n);
        }
    }

    @Override
    protected void setActiveInternal(@NotNull Direction facing) {
        super.setActiveInternal(facing);
        FacingPos pos = new FacingPos(node.getEquivalencyData(), facing);
        NetNode existing = node.getNet().getNode(pos);
        WorldPipeCapConnectionNode connectionNode;
        if (existing instanceof WorldPipeCapConnectionNode c) {
            connectionNode = c;
        } else {
            connectionNode = new WorldPipeCapConnectionNode(node.getNet());
            connectionNode.setPosAndFacing(pos);
            connectionNode.getNet().addNode(connectionNode);
        }
        managed.put(facing, connectionNode);
        node.getNet().addEdge(node, connectionNode, true);
    }

    @Override
    protected void setIdleInternal(@NotNull Direction facing) {
        super.setIdleInternal(facing);
        WorldPipeCapConnectionNode n = managed.remove(facing);
        if (n != null) node.getNet().removeNode(n);
    }

    public @Nullable WorldPipeCapConnectionNode getNodeForFacing(Direction facing) {
        return managed.get(facing);
    }
}
