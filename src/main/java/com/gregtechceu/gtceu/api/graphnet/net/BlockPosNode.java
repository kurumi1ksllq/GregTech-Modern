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

    public BlockPosNode(IGraphNet net) {
        super(net);
        pos = BlockPos.ZERO;
    }

    public BlockPosNode setPos(BlockPos pos) {
        this.pos = pos;
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

    @Override
    public @NotNull GraphClassType<? extends BlockPosNode> getType() {
        return TYPE;
    }
}
