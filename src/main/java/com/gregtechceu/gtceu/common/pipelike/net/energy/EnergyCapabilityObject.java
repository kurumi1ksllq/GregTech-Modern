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
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.IWorldPipeNetTile;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.TickTracker;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
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
            if (!(dest instanceof WorldPipeNode)) continue;

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
        MAIN_LOOP:
        for (int i = 0; i < paths.size(); i++) {
            EnergyPath path = paths.get(i);
            NetNode target = path.getTargetNode();
            // WorldPipeNode-ness was already determined in earlier loop
            IWorldPipeNetTile pipeTile = ((WorldPipeNode) target).getBlockEntity();
            EnumMap<Direction, BlockEntity> targets = pipeTile.getTargetsWithCapabilities(((WorldPipeNode) target));
            for (Direction facing : GTUtil.DIRECTIONS) {
                if (target == node && facing == side) continue; // anti insert-to-our-source logic
                BlockEntity tile = targets.get(facing);
                if (tile == null) continue;
                IEnergyContainer container = tile.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER,
                        facing.getOpposite()).resolve().orElse(null);
                if (container == null) continue;

                long allowed = container.acceptEnergyFromNetwork(facing, voltage, available, true);
                if (allowed <= 0) continue;

                EnergyPath.PathFlowReport flow = path.traverse(voltage, allowed);
                available -= allowed;
                if (!simulate) {
                    flow.report();
                    if (flow.euOut() > 0) {
                        container.acceptEnergyFromNetwork(facing, flow.voltageOut(), flow.amperageOut(), false);
                    }
                }
                if (available <= 0) {
                    break MAIN_LOOP;
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
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
        return GTCapability.CAPABILITY_ENERGY_CONTAINER.orEmpty(capability, LazyOptional.of(() -> this));
    }

    @Override
    public long getInputPerSec() {
        EnergyGroupData data = getGroupData();
        if (data == null) return 0;
        else return data.getEnergyInPerSec(TickTracker.getTick());
    }

    @Override
    public long getOutputPerSec() {
        EnergyGroupData data = getGroupData();
        if (data == null) return 0;
        else return data.getEnergyOutPerSec(TickTracker.getTick());
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
