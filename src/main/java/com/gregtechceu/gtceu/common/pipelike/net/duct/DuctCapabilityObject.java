package com.gregtechceu.gtceu.common.pipelike.net.duct;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IHazardParticleContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.HazardProperty;
import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.graphnet.group.NetGroup;
import com.gregtechceu.gtceu.api.graphnet.group.PathCacheGroupData;
import com.gregtechceu.gtceu.api.graphnet.logic.ChannelCountLogic;
import com.gregtechceu.gtceu.api.graphnet.logic.ThroughputLogic;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.NodeExposingCapabilities;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.IWorldPipeNetTile;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.NodeManagingPCW;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.common.capability.EnvironmentalHazardSavedData;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.TickTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;

public class DuctCapabilityObject implements IPipeCapabilityObject, IHazardParticleContainer {

    public static final int ACTIVE_KEY = 155;

    private @Nullable PipeBlockEntity blockEntity;
    private NodeManagingPCW capabilityWrapper;

    private final EnumMap<Direction, DuctCapabilityObject.Wrapper> wrappers = new EnumMap<>(Direction.class);
    private final @NotNull WorldPipeNode node;

    private boolean transferring = false;

    public DuctCapabilityObject(@NotNull WorldPipeNode node) {
        this.node = node;
        for (Direction facing : GTUtil.DIRECTIONS) {
            wrappers.put(facing, new DuctCapabilityObject.Wrapper(facing));
        }
    }

    @Override
    public void init(@NotNull PipeBlockEntity tile, @NotNull PipeCapabilityWrapper wrapper) {
        this.blockEntity = tile;
        if (!(wrapper instanceof NodeManagingPCW p))
            throw new IllegalArgumentException("DuctCapabilityObjects must be initialized to NodeManagingPCWs!");
        this.capabilityWrapper = p;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // can't expose the sided capability if there is no node to interact with
        if (side != null && capabilityWrapper.getNodeForFacing(side) == null) return LazyOptional.empty();
        return GTCapability.CAPABILITY_HAZARD_CONTAINER.orEmpty(cap,
                LazyOptional.of(() -> side == null ? this : wrappers.get(side)));
    }

    private boolean inputDisallowed(Direction side) {
        if (side == null) return false;
        if (blockEntity == null) return true;
        else return blockEntity.isBlocked(side);
    }

    protected @Nullable NetNode getRelevantNode(Direction facing) {
        return facing == null ? node : capabilityWrapper.getNodeForFacing(facing);
    }

    @Override
    public boolean inputsHazard(Direction side, MedicalCondition condition) {
        return !inputDisallowed(side);
    }

    @Override
    public float changeHazard(MedicalCondition condition, float amount, boolean simulate) {
        return changeHazard(condition, amount, simulate, null);
    }

    public float changeHazard(MedicalCondition condition, float differenceAmount, boolean simulate,
                              @Nullable Direction side) {
        if (blockEntity == null || this.transferring || inputDisallowed(side)) return 0;
        NetGroup group = node.getGroupSafe();
        if (!(group.getData() instanceof DuctGroupData data)) return 0;

        this.transferring = true;

        PathCacheGroupData.SecondaryCache cache = data.getOrCreate(node);
        List<DuctPath> paths = new ObjectArrayList<>(group.getNodesUnderKey(ACTIVE_KEY).size());
        for (NetNode dest : group.getNodesUnderKey(ACTIVE_KEY)) {
            if (!(dest instanceof WorldPipeNode)) continue;

            DuctPath path = (DuctPath) cache.getOrCompute(dest);
            if (path == null) continue;
            // construct the path list in order of ascending weight
            int i = 0;
            while (i < paths.size()) {
                if (paths.get(i).getWeight() >= path.getWeight()) break;
                else i++;
            }
            paths.add(i, path);
        }
        float available = differenceAmount;
        MAIN_LOOP:
        for (int i = 0; i < paths.size(); i++) {
            DuctPath path = paths.get(i);
            NetNode target = path.getTargetNode();
            // WorldPipeNode-ness was already determined in earlier loop
            IWorldPipeNetTile pipeTile = ((WorldPipeNode) target).getBlockEntity();
            EnumMap<Direction, BlockEntity> targets = pipeTile.getTargetsWithCapabilities(((WorldPipeNode) target));
            for (Direction facing : GTUtil.DIRECTIONS) {
                if (target == node && facing == side) continue; // anti insert-to-our-source logic
                BlockEntity tile = targets.get(facing);
                if (tile == null) continue;
                IHazardParticleContainer container = tile.getCapability(GTCapability.CAPABILITY_HAZARD_CONTAINER,
                        facing.getOpposite()).resolve().orElse(null);
                if (container == null) continue;

                float allowed = container.changeHazard(condition, differenceAmount, true);
                if (allowed <= 0) continue;

                DuctPath.PathFlowReport flow = path.traverse(condition, allowed);
                available -= allowed;
                if (!simulate) {
                    flow.report();
                    if (flow.amountOut() > 0) {
                        container.changeHazard(flow.conditionOut(), flow.amountOut(), false);
                    }
                }
                if (available <= 0) {
                    break MAIN_LOOP;
                }
            }
        }

        this.transferring = false;
        return differenceAmount - available;
    }

    public static float getFlowLimit(NetNode node, DuctTestObject testObject) {
        ThroughputLogic throughput = node.getData().getLogicEntryNullable(ThroughputLogic.TYPE);
        if (throughput == null) return Float.MAX_VALUE;
        DuctFlowLogic history = node.getData().getLogicEntryNullable(DuctFlowLogic.TYPE);
        if (history == null) return throughput.getValue() * DuctFlowLogic.MEMORY_TICKS;
        Object2FloatMap<DuctTestObject> sum = history.getSum();
        if (sum.isEmpty()) return GTMath.saturatedCast(throughput.getValue() * DuctFlowLogic.MEMORY_TICKS);
        if (sum.size() < node.getData().getLogicEntryDefaultable(ChannelCountLogic.TYPE).getValue() ||
                sum.containsKey(testObject)) {
            return throughput.getValue() * DuctFlowLogic.MEMORY_TICKS - sum.getFloat(testObject);
        }
        return 0;
    }

    public static void reportFlow(NetNode node, float flow, DuctTestObject testObject) {
        DuctFlowLogic logic = node.getData().getLogicEntryNullable(DuctFlowLogic.TYPE);
        if (logic == null) {
            logic = DuctFlowLogic.TYPE.getNew();
            node.getData().setLogicEntry(logic);
        }
        logic.recordFlow(TickTracker.getTick(), testObject, flow);
    }

    public static float getSupplyOrDemand(NetNode node, DuctTestObject testObject, boolean supply) {
        if (node instanceof NodeExposingCapabilities exposer) {
            IHazardParticleContainer handler = exposer.getProvider()
                    .getCapability(GTCapability.CAPABILITY_HAZARD_CONTAINER, exposer.exposedFacing())
                    .resolve().orElse(null);
            if (handler != null && instanceOf(handler) == null) {
                if (supply) {
                    return handler.removeHazard(testObject.condition, Float.MAX_VALUE, true);
                } else {
                    return handler.addHazard(testObject.condition, Float.MAX_VALUE, true);
                }
            } else if (handler == null) {
                return Float.MAX_VALUE;
            }
        }
        return 0;
    }

    public void reportExtractedInserted(NetNode node, float flow, DuctTestObject testObject, boolean extracted) {
        if (flow <= 0) return;
        if (node instanceof NodeExposingCapabilities exposer) {
            IHazardParticleContainer handler = exposer.getProvider()
                    .getCapability(GTCapability.CAPABILITY_HAZARD_CONTAINER, exposer.exposedFacing())
                    .resolve().orElse(null);
            if (handler != null) {
                if (extracted) {
                    handler.removeHazard(testObject.condition, flow, false);
                } else {
                    handler.addHazard(testObject.condition, flow, false);
                }
            } else if (node instanceof WorldPipeNode pipeNode) {
                ServerLevel level = pipeNode.getNet().getLevel();
                BlockPos pos = pipeNode.getEquivalencyData();
                Direction openDir = exposer.exposedFacing();
                var savedData = EnvironmentalHazardSavedData.getOrCreate(level);
                savedData.addZone(pos.relative(openDir), flow,
                        true, HazardProperty.HazardTrigger.INHALATION, testObject.condition);
                emitPollutionParticles(level, pos, openDir);
            }
        }
    }

    @Override
    public float getHazardStored(MedicalCondition condition) {
        return 0;
    }

    @Override
    public float getHazardCapacity(MedicalCondition condition) {
        return Float.MAX_VALUE;
    }

    public static void emitPollutionParticles(ServerLevel level, BlockPos pos, Direction frontFacing) {
        float xPos = frontFacing.getStepX() * 0.76F + pos.getX() + 0.25F;
        float yPos = frontFacing.getStepY() * 0.76F + pos.getY() + 0.25F;
        float zPos = frontFacing.getStepZ() * 0.76F + pos.getZ() + 0.25F;

        float ySpd = frontFacing.getStepY() * 0.1F + 0.2F + 0.1F * GTValues.RNG.nextFloat();
        float xSpd;
        float zSpd;

        if (frontFacing.getStepY() == -1) {
            float temp = GTValues.RNG.nextFloat() * 2 * (float) Math.PI;
            xSpd = (float) Math.sin(temp) * 0.1F;
            zSpd = (float) Math.cos(temp) * 0.1F;
        } else {
            xSpd = frontFacing.getStepX() * (0.1F + 0.2F * GTValues.RNG.nextFloat());
            zSpd = frontFacing.getStepZ() * (0.1F + 0.2F * GTValues.RNG.nextFloat());
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                xPos + GTValues.RNG.nextFloat() * 0.5F,
                yPos + GTValues.RNG.nextFloat() * 0.5F,
                zPos + GTValues.RNG.nextFloat() * 0.5F,
                1,
                xSpd, ySpd, zSpd,
                0.1);
    }

    @Nullable
    public static DuctCapabilityObject instanceOf(IHazardParticleContainer container) {
        if (container instanceof DuctCapabilityObject f) return f;
        if (container instanceof DuctCapabilityObject.Wrapper w) return w.getParent();
        return null;
    }

    protected class Wrapper implements IHazardParticleContainer {

        private final Direction facing;

        public Wrapper(Direction facing) {
            this.facing = facing;
        }

        @Override
        public boolean inputsHazard(Direction side, MedicalCondition condition) {
            return DuctCapabilityObject.this.inputsHazard(facing, condition);
        }

        @Override
        public float changeHazard(MedicalCondition condition, float amount, boolean simulate) {
            return DuctCapabilityObject.this.changeHazard(condition, amount, simulate, facing);
        }

        @Override
        public float getHazardStored(MedicalCondition condition) {
            return 0;
        }

        @Override
        public float getHazardCapacity(MedicalCondition condition) {
            return Float.MAX_VALUE;
        }

        public DuctCapabilityObject getParent() {
            return DuctCapabilityObject.this;
        }
    }
}
