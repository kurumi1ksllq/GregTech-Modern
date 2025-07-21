package com.gregtechceu.gtceu.api.graphnet.net;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.graphnet.GraphClassType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import org.jetbrains.annotations.NotNull;

public class BlockPosNode extends NetNode {

    public static final GraphClassType<BlockPosNode> TYPE = new GraphClassType<>(GTCEu.MOD_ID, "BlockPosNode",
            BlockPosNode::new);

    private @NotNull BlockPos pos;
    private int hash;

    public BlockPosNode(IGraphNet net) {
        super(net);
        this.pos = BlockPos.ZERO;
        this.hash = pos.hashCode();
    }

    public BlockPosNode setPos(BlockPos pos) {
        this.pos = pos;
        this.hash = pos.hashCode();
        return this;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putLong("Pos", pos.asLong());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        this.setPos(BlockPos.of(nbt.getLong("Pos")));
    }

    @Override
    public @NotNull BlockPos getEquivalencyData() {
        return pos;
    }

    // cache the hash to improve hashmap performance
    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public @NotNull GraphClassType<? extends BlockPosNode> getType() {
        return TYPE;
    }
}
