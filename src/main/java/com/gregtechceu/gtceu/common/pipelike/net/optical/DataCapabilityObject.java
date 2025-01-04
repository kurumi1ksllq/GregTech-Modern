package com.gregtechceu.gtceu.common.pipelike.net.optical;

import com.gregtechceu.gtceu.api.capability.data.IDataAccess;
import com.gregtechceu.gtceu.api.capability.data.query.DataAccessFormat;
import com.gregtechceu.gtceu.api.capability.data.query.DataQueryObject;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.graphnet.group.PathCacheGroupData;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.path.NetPath;
import com.gregtechceu.gtceu.api.graphnet.path.SingletonNetPath;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.common.cover.ShutterCover;
import com.gregtechceu.gtceu.common.pipelike.net.SlowActiveWalker;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Set;

public class DataCapabilityObject implements IPipeCapabilityObject, IDataAccess {

    public static final int ACTIVE_KEY = 122;

    private final WorldPipeNode node;

    private @Nullable PipeBlockEntity tile;

    private final EnumMap<Direction, Wrapper> wrappers = new EnumMap<>(Direction.class);

    public DataCapabilityObject(@NotNull WorldPipeNode node) {
        this.node = node;
        for (Direction facing : GTUtil.DIRECTIONS) {
            wrappers.put(facing, new Wrapper(facing));
        }
    }

    @Override
    public void init(@NotNull PipeBlockEntity tile, @NotNull PipeCapabilityWrapper wrapper) {
        this.tile = tile;
    }

    @Override
    public boolean accessData(@NotNull DataQueryObject queryObject) {
        return accessData(queryObject, null);
    }

    private boolean accessData(@NotNull DataQueryObject queryObject, @Nullable Direction facing) {
        if (tile == null) return false;

        NetPath path;
        if (node.getGroupUnsafe() == null || node.getGroupSafe().getNodes().size() == 1)
            path = new SingletonNetPath(node);
        else if (node.getGroupSafe().getData() instanceof PathCacheGroupData cache) {
            Set<NetNode> actives = node.getGroupSafe().getNodesUnderKey(ACTIVE_KEY);
            if (actives.size() > 2) return false; // single-destination contract violated
            var iter = actives.iterator();
            NetNode target = iter.next();
            if (target == node) {
                if (!iter.hasNext()) return false; // no destinations
                target = iter.next();
            }
            if (!(target instanceof WorldPipeNode)) return false; // useless target
            path = cache.getOrCreate(node).getOrCompute(target);
            if (path == null) return false; // no path
        } else return false; // no cache to lookup with

        WorldPipeNode destination = path.getTargetNode();
        for (var capability : destination.getBlockEntity().getTargetsWithCapabilities(destination).entrySet()) {
            if (destination == node && capability.getKey() == facing) continue; // anti insert-to-our-source logic
            IDataAccess access = capability.getValue()
                    .getCapability(GTCapability.CAPABILITY_DATA_ACCESS,
                            capability.getKey().getOpposite())
                    .resolve().orElse(null);
            if (access != null && !(destination.getBlockEntity().getCoverHolder()
                    .getCoverAtSide(capability.getKey()) instanceof ShutterCover)) {
                queryObject.setShouldTriggerWalker(false);
                boolean cancelled = access.accessData(queryObject);
                if (queryObject.isShouldTriggerWalker()) {
                    // since we are a pull-based system, we need to reverse the path for it to look correct
                    SlowActiveWalker.dispatch(tile.getLevel(), path.reversed(), 1, 1, 5);
                }
                if (cancelled) return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull DataAccessFormat getFormat() {
        return DataAccessFormat.UNIVERSAL;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction facing) {
        return GTCapability.CAPABILITY_DATA_ACCESS.orEmpty(cap,
                LazyOptional.of(() -> facing == null ? this : wrappers.get(facing)));
    }

    protected class Wrapper implements IDataAccess {

        private final Direction facing;

        public Wrapper(Direction facing) {
            this.facing = facing;
        }

        @Override
        public boolean accessData(@NotNull DataQueryObject queryObject) {
            return DataCapabilityObject.this.accessData(queryObject, facing);
        }

        @Override
        public @NotNull DataAccessFormat getFormat() {
            return DataCapabilityObject.this.getFormat();
        }

        @Override
        public boolean supportsQuery(@NotNull DataQueryObject queryObject) {
            return DataCapabilityObject.this.supportsQuery(queryObject);
        }
    }
}
