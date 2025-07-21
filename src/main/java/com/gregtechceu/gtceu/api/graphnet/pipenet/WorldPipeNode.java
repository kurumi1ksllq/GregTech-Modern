package com.gregtechceu.gtceu.api.graphnet.pipenet;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.graphnet.GraphClassType;
import com.gregtechceu.gtceu.api.graphnet.MultiNodeHelper;
import com.gregtechceu.gtceu.api.graphnet.net.BlockPosNode;
import com.gregtechceu.gtceu.api.graphnet.net.IGraphNet;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.IWorldPipeNetTile;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.common.data.GTBlockEntities;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class WorldPipeNode extends BlockPosNode
                           implements NodeWithFacingToOthers, NodeWithCovers, NodeExposingCapabilities {

    public static final GraphClassType<WorldPipeNode> TYPE = new GraphClassType<>(GTCEu.MOD_ID, "WorldPipeNode",
            WorldPipeNode::resolve);

    private static final PipeBlockEntity FALLBACK = new PipeBlockEntity(GTBlockEntities.PIPE.get(), BlockPos.ZERO,
            GTMaterialBlocks.MATERIAL_PIPE_BLOCKS.get(TagPrefix.pipeNormal, GTMaterials.Aluminium).getDefaultState());

    @Nullable
    MultiNodeHelper overlapHelper;

    private WeakReference<IWorldPipeNetTile> tileReference;

    public WorldPipeNode(WorldPipeNet net) {
        super(net);
    }

    private static WorldPipeNode resolve(IGraphNet net) {
        if (net instanceof WorldPipeNet w) return new WorldPipeNode(w);
        GTCEu.LOGGER.error(
                "Attempted to initialize a WorldPipeNode to a non-WorldPipeNet. If relevant NPEs occur later, this is most likely the cause.");
        return null;
    }

    public @NotNull IWorldPipeNetTile getBlockEntity() {
        IWorldPipeNetTile tile = getBlockEntity(true);
        if (tile == null) {
            // something went very wrong, return the fallback to prevent NPEs and remove us from the net.
            getNet().removeNode(this);
            tile = FALLBACK;
        }
        return tile;
    }

    @Nullable
    public IWorldPipeNetTile getBlockEntityNoLoading() {
        return getBlockEntity(false);
    }

    private IWorldPipeNetTile getBlockEntity(boolean allowLoading) {
        if (tileReference != null) {
            IWorldPipeNetTile tile = tileReference.get();
            if (tile != null) return tile;
        }
        Level level = getNet().getLevel();
        if (!allowLoading && !level.isLoaded(getEquivalencyData())) return null;
        BlockEntity tile = level.getBlockEntity(getEquivalencyData());
        if (tile instanceof IWorldPipeNetTile pipe) {
            this.tileReference = new WeakReference<>(pipe);
            return pipe;
        } else return null;
    }

    @Override
    public void onRemove() {
        if (this.overlapHelper != null) {
            this.overlapHelper.removeNode(this);
            this.overlapHelper = null;
        }
    }

    @Override
    public @NotNull WorldPipeNet getNet() {
        return (WorldPipeNet) super.getNet();
    }

    @Override
    public WorldPipeNode setPos(BlockPos pos) {
        super.setPos(pos);
        this.getNet().synchronizeNode(this);
        return this;
    }

    @Override
    public boolean traverse(long queryTick, boolean simulate) {
        if (overlapHelper != null) {
            return overlapHelper.traverse(this.getNet(), queryTick, simulate);
        } else return true;
    }

    @Override
    public @NotNull GraphClassType<? extends WorldPipeNode> getType() {
        return TYPE;
    }

    @Override
    public @Nullable Direction getFacingToOther(@NotNull NetNode other) {
        return other instanceof WorldPipeNode n ?
                GTUtil.getFacingToNeighbor(this.getEquivalencyData(), n.getEquivalencyData()) : null;
    }

    @Override
    public @Nullable ICoverable getCoverable() {
        return getBlockEntity().getCoverHolder();
    }

    @Override
    public @NotNull ICapabilityProvider getProvider() {
        return getBlockEntity();
    }
}
