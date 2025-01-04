package com.gregtechceu.gtceu.common.pipelike.net.energy;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.graphnet.group.GroupData;
import com.gregtechceu.gtceu.api.graphnet.group.NetGroup;
import com.gregtechceu.gtceu.api.graphnet.group.PathCacheGroupData;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.common.cover.ShutterCover;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyCapabilityObject implements IPipeCapabilityObject, IEnergyContainer {

    public static final int ACTIVE_KEY = 122;

    private @Nullable PipeBlockEntity blockEntity;

    private final @NotNull WorldPipeNode node;

    private boolean transferring = false;

    public EnergyCapabilityObject(@NotNull WorldPipeNode node) {
        this.node = node;
    }

    private boolean inputDisallowed(Direction side) {
        if (side == null) return false;
        if (blockEntity == null) return true;
        else return blockEntity.isBlocked(side);
    }

    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage, boolean simulate) {
        if (blockEntity == null || this.transferring || inputDisallowed(side)) return 0;
        NetGroup group = node.getGroupSafe();
        if (!(group.getData() instanceof EnergyGroupData data)) return 0;

        this.transferring = true;

        PathCacheGroupData.SecondaryCache cache = data.getOrCreate(node);
        List<EnergyPath> paths = new ObjectArrayList<>(group.getNodesUnderKey(ACTIVE_KEY).size());
        for (NetNode dest : group.getNodesUnderKey(ACTIVE_KEY)) {
            EnergyPath path = (EnergyPath) cache.getOrCompute(dest);
            if (path == null) continue;
            // construct the path list in order of ascending weight
            int i = 0;
            while (i < paths.size()) {
                if (paths.get(i).getWeight() >= path.getWeight()) break;
                else i++;
            }
            paths.add(i, path);
        }
        long available = amperage;
        for (EnergyPath path : paths) {
            NetNode target = path.getTargetNode();
            if (!(target instanceof WorldPipeNode n)) continue;
            for (var capability : n.getBlockEntity().getTargetsWithCapabilities(n).entrySet()) {
                if (n == node && capability.getKey() == side) continue; // anti insert-to-our-source logic

                IEnergyContainer container = capability.getValue().getCapability(
                        GTCapability.CAPABILITY_ENERGY_CONTAINER, capability.getKey().getOpposite())
                        .resolve().orElse(null);
                if (container != null && !(n.getBlockEntity().getCoverHolder()
                        .getCoverAtSide(capability.getKey()) instanceof ShutterCover)) {
                    long allowed = container.acceptEnergyFromNetwork(capability.getKey(), voltage, amperage, true);
                    EnergyPath.PathFlowReport flow = path.traverse(voltage, allowed);
                    if (flow.euOut() > 0) {
                        available -= allowed;
                        if (!simulate) {
                            flow.report();
                            container.acceptEnergyFromNetwork(capability.getKey(), flow.voltageOut(),
                                    flow.amperageOut(), false);
                        }
                    }
                }
            }
        }

        this.transferring = false;
        return amperage - available;
    }

    @Nullable
    private EnergyGroupData getGroupData() {
        NetGroup group = node.getGroupUnsafe();
        if (group == null) return null;
        GroupData data = group.getData();
        if (!(data instanceof EnergyGroupData e)) return null;
        return e;
    }

    @Override
    public long getInputAmperage() {
        return node.getData().getLogicEntryDefaultable(AmperageLimitLogic.TYPE).getValue();
    }

    @Override
    public long getInputVoltage() {
        return node.getData().getLogicEntryDefaultable(VoltageLimitLogic.TYPE).getValue();
    }

    @Override
    public void init(@NotNull PipeBlockEntity tile, @NotNull PipeCapabilityWrapper wrapper) {
        this.blockEntity = tile;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        return GTCapability.CAPABILITY_ENERGY_CONTAINER.orEmpty(capability, LazyOptional.of(() -> this));
    }

    @Override
    public long getInputPerSec() {
        EnergyGroupData data = getGroupData();
        if (data == null) return 0;
        else return data.getEnergyInPerSec(GTUtil.getCurrentServerTick());
    }

    @Override
    public long getOutputPerSec() {
        EnergyGroupData data = getGroupData();
        if (data == null) return 0;
        else return data.getEnergyOutPerSec(GTUtil.getCurrentServerTick());
    }

    @Override
    public boolean inputsEnergy(Direction side) {
        return !inputDisallowed(side);
    }

    @Override
    public boolean outputsEnergy(Direction side) {
        return true;
    }

    @Override
    public long changeEnergy(long differenceAmount) {
        GTCEu.LOGGER.error("Do not use changeEnergy() for cables! Use acceptEnergyFromNetwork()");
        return acceptEnergyFromNetwork(null,
                differenceAmount / getInputAmperage(),
                differenceAmount / getInputVoltage(), false) * getInputVoltage();
    }

    @Override
    public long getEnergyStored() {
        return 0;
    }

    @Override
    public long getEnergyCapacity() {
        return getInputAmperage() * getInputVoltage();
    }

    @Override
    public boolean isOneProbeHidden() {
        return true;
    }
}
