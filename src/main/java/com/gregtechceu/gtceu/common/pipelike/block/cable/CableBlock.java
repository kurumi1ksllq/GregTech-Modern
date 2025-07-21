package com.gregtechceu.gtceu.common.pipelike.block.cable;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.graphnet.pipenet.IPipeNetNodeHandler;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IBurnable;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block.PipeMaterialBlock;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.client.renderer.pipe.CableModel;
import com.gregtechceu.gtceu.common.data.GTDamageTypes;
import com.gregtechceu.gtceu.common.pipelike.net.energy.EnergyFlowData;
import com.gregtechceu.gtceu.common.pipelike.net.energy.EnergyFlowLogic;
import com.gregtechceu.gtceu.common.pipelike.net.energy.SuperconductorLogic;
import com.gregtechceu.gtceu.common.pipelike.net.energy.WorldEnergyNet;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CableBlock extends PipeMaterialBlock implements IBurnable {

    private static final Map<Material, Map<CableStructure, CableBlock>> CACHE = new Object2ObjectOpenHashMap<>();

    private static final ThreadLocal<Boolean> RELOCATING_TILE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public CableBlock(BlockBehaviour.Properties properties, CableStructure structure, @NotNull Material material) {
        super(properties, structure, material);
        CACHE.compute(material, (k, v) -> {
            if (v == null) v = new Object2ObjectOpenHashMap<>();
            v.put(structure, this);
            return v;
        });
    }

    public int tinted(BlockState state, @Nullable BlockAndTintGetter level,
                      @Nullable BlockPos pos, int index) {
        if (getStructure().isInsulated() && index == 1) {
            return CableModel.DEFAULT_INSULATION_COLOR;
        }
        return super.tinted(state, level, pos, index);
    }

    @Override
    public CableStructure getStructure() {
        return (CableStructure) super.getStructure();
    }

    @Override
    public GTToolType getToolClass() {
        return GTToolType.WIRE_CUTTER;
    }

    @Override
    protected String getConnectLangKey() {
        return "gtceu.tool_action.wire_cutter.connect";
    }

    @Override
    public void partialBurn(BlockState state, Level world, BlockPos pos) {
        CableStructure structure = getStructure();
        if (structure.partialBurnStructure() != null) {
            RELOCATING_TILE.set(Boolean.TRUE);
            PipeBlockEntity oldPipe = null;
            if (world.getBlockEntity(pos) instanceof PipeBlockEntity old) {
                oldPipe = old;
            }

            BlockState newState = CACHE.get(material).get(structure.partialBurnStructure()).defaultBlockState();
            world.setBlockAndUpdate(pos, newState);
            RELOCATING_TILE.set(Boolean.FALSE);

            BlockEntity newBlockEntity = world.getBlockEntity(pos);
            if (oldPipe != null && newBlockEntity instanceof PipeBlockEntity pipe) {
                pipe.load(oldPipe.saveWithoutMetadata());
                pipe.initialize();
                pipe.syncNow(true);
                pipe.markAsDirty();
            }
        }
    }

    @Override
    public @NotNull IPipeNetNodeHandler getHandler(PipeBlockEntity tileContext) {
        if (RELOCATING_TILE.get()) {
            // prevent node removal when relocating tile
            return IPipeNetNodeHandler.EMPTY;
        }
        return super.getHandler(tileContext);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (!(level instanceof ServerLevel serverLevel) || getStructure().isInsulated() ||
                !(entity instanceof LivingEntity living)) {
            return;
        }
        PipeBlockEntity tile = getBlockEntity(level, pos);
        if (tile != null && tile.getFrameMaterial().isNull() && tile.getOffsetTimer() % 10 == 0) {
            WorldPipeNode node = WorldEnergyNet.getWorldNet(serverLevel).getNode(pos);

            if (node != null) {
                if (node.getData().getLogicEntryNullable(SuperconductorLogic.TYPE) != null) return;
                EnergyFlowLogic logic = node.getData().getLogicEntryNullable(EnergyFlowLogic.TYPE);

                if (logic != null) {
                    int tick = GTCEu.getMinecraftServer().getTickCount();
                    long cumulativeDamage = 0;
                    for (EnergyFlowData data : logic.getFlow(tick)) {
                        cumulativeDamage += (GTUtil.getTierByVoltage(data.voltage()) + 1) * data.amperage() * 4;
                    }

                    if (cumulativeDamage != 0) {
                        living.hurt(GTDamageTypes.ELECTRIC.source(serverLevel), cumulativeDamage);
                        // TODO advancement
                        // if (living instanceof ServerPlayer serverPlayer) {
                        // AdvancementTriggers.ELECTROCUTION_DEATH.trigger(serverPlayer);
                        // }
                    }
                }
            }
        }
    }
}
