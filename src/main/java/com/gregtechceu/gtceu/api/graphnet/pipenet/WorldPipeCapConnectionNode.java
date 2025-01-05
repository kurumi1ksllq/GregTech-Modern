package com.gregtechceu.gtceu.api.graphnet.pipenet;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.graphnet.GraphClassType;
import com.gregtechceu.gtceu.api.graphnet.net.IGraphNet;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.utils.FacingPos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldPipeCapConnectionNode extends NetNode implements NodeWithFacingToOthers, NodeExposingCapabilities {

    public static final int SORTING_KEY = 432;

    public static final GraphClassType<WorldPipeCapConnectionNode> TYPE = new GraphClassType<>(GTCEu.MOD_ID,
            "WorldPipeCapConnectionNode",
            WorldPipeCapConnectionNode::resolve);

    private @NotNull FacingPos posAndFacing;

    public WorldPipeCapConnectionNode(WorldPipeNet net) {
        super(net);
        sortingKey = SORTING_KEY;
        posAndFacing = FacingPos.ZERO;
    }

    private static WorldPipeCapConnectionNode resolve(IGraphNet net) {
        if (net instanceof WorldPipeNet w) return new WorldPipeCapConnectionNode(w);
        GTCEu.LOGGER.error(
                "Attempted to initialize a WorldPipeCapConnectionNode to a non-WorldPipeNet. If relevant NPEs occur later, this is most likely the cause.");
        return null;
    }

    public WorldPipeNode getParent() {
        return getNet().getNode(getEquivalencyData().getPos());
    }

    public WorldPipeCapConnectionNode setPosAndFacing(FacingPos posAndFacing) {
        this.posAndFacing = posAndFacing;
        return this;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        tag.putLong("Pos", posAndFacing.getPos().asLong());
        tag.putByte("Facing", (byte) posAndFacing.getDirection().get3DDataValue());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        this.setPosAndFacing(new FacingPos(BlockPos.of(nbt.getLong("Pos")),
                Direction.from3DDataValue(nbt.getByte("Facing"))));
    }

    @Override
    public @NotNull WorldPipeNet getNet() {
        return (WorldPipeNet) super.getNet();
    }

    @Override
    public @NotNull FacingPos getEquivalencyData() {
        return posAndFacing;
    }

    @Override
    public @NotNull GraphClassType<? extends WorldPipeCapConnectionNode> getType() {
        return TYPE;
    }

    @Override
    public @Nullable Direction getFacingToOther(@NotNull NetNode other) {
        if (other instanceof WorldPipeNode n && n.getEquivalencyData().equals(posAndFacing.getPos()))
            return posAndFacing.getDirection().getOpposite();
        else return null;
    }

    @Override
    public @NotNull ICapabilityProvider getProvider() {
        WorldPipeNode parent = getParent();
        if (parent == null) return EMPTY;
        ICapabilityProvider prov = parent.getBlockEntity().getTargetWithCapabilities(parent,
                posAndFacing.getDirection());
        return prov != null ? prov : EMPTY;
    }

    @Override
    public Direction exposedFacing() {
        return posAndFacing.getDirection().getOpposite();
    }

    private static final ICapabilityProvider EMPTY = new ICapabilityProvider() {

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
            return LazyOptional.empty();
        }
    };
}
