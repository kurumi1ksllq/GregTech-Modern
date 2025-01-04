package com.gregtechceu.gtceu.common.pipelike.net.laser;

import com.gregtechceu.gtceu.api.capability.ILaserRelay;
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

public class LaserCapabilityObject implements IPipeCapabilityObject, ILaserRelay {

    public static final int ACTIVE_KEY = 122;

    protected final WorldPipeNode node;
    private @Nullable PipeBlockEntity tile;

    private final EnumMap<Direction, Wrapper> wrappers = new EnumMap<>(Direction.class);

    private boolean transmitting;

    public LaserCapabilityObject(@NotNull WorldPipeNode node) {
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
    public long receiveLaser(long laserVoltage, long laserAmperage) {
        return receiveLaser(laserVoltage, laserAmperage, null);
    }

    protected long receiveLaser(long laserVoltage, long laserAmperage, Direction facing) {
        long result = 0;
        boolean earlyReturn = false;
        if (tile != null && !this.transmitting) {
            this.transmitting = true;
            NetPath path = null;
            if (node.getGroupUnsafe() == null || node.getGroupSafe().getNodes().size() == 1)
                path = new SingletonNetPath(node);
            else if (node.getGroupSafe().getData() instanceof PathCacheGroupData cache) {
                Set<NetNode> actives = node.getGroupSafe().getNodesUnderKey(ACTIVE_KEY);
                if (actives.size() > 2) {
                    earlyReturn = true;// single-destination contract violated
                } else {
                    var iter = actives.iterator();
                    NetNode target = iter.next();
                    if (target == node) {
                        if (!iter.hasNext()) {
                            earlyReturn = true;// no destinations
                        } else {
                            target = iter.next();
                        }
                    }
                    if (!earlyReturn) {
                        if (!(target instanceof WorldPipeNode)) {
                            earlyReturn = true;// useless target
                        } else {
                            path = cache.getOrCreate(node).getOrCompute(target);
                            if (path == null) {
                                earlyReturn = true;// no path
                            }
                        }
                    }
                }
            } else {
                earlyReturn = true;// no cache to lookup with
            }
            if (!earlyReturn) {
                long available = laserAmperage;
                WorldPipeNode destination = (WorldPipeNode) path.getTargetNode();
                for (var capability : destination.getBlockEntity().getTargetsWithCapabilities(destination).entrySet()) {
                    if (destination == node && capability.getKey() == facing)
                        continue; // anti insert-to-our-source logic
                    ILaserRelay laser = capability.getValue()
                            .getCapability(GTCapability.CAPABILITY_LASER,
                                    capability.getKey().getOpposite())
                            .resolve().orElse(null);
                    if (laser != null && !(destination.getBlockEntity().getCoverHolder()
                            .getCoverAtSide(capability.getKey()) instanceof ShutterCover)) {
                        long transmitted = laser.receiveLaser(laserVoltage, laserAmperage);
                        if (transmitted > 0) {
                            SlowActiveWalker.dispatch(tile.getLevel(), path, 1, 2, 2);
                            available -= transmitted;
                            if (available <= 0) {
                                result = laserAmperage;
                                earlyReturn = true;
                                break;
                            }
                        }
                    }
                }
                if (!earlyReturn) {
                    result = laserAmperage - available;
                }
            }
            this.transmitting = false;
        }

        return result;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction facing) {
        return GTCapability.CAPABILITY_LASER.orEmpty(cap,
                LazyOptional.of(() -> facing == null ? this : wrappers.get(facing)));
    }

    protected class Wrapper implements ILaserRelay {

        private final Direction facing;

        public Wrapper(Direction facing) {
            this.facing = facing;
        }

        @Override
        public long receiveLaser(long laserVoltage, long laserAmperage) {
            return LaserCapabilityObject.this.receiveLaser(laserVoltage, laserAmperage, facing);
        }
    }
}
