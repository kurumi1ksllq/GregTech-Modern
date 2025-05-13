package com.gregtechceu.gtceu.api.mui.utils.fakeworld;

import net.minecraft.block.state.BlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RenderWorld implements IBlockAccess {

    private final ISchema schema;
    private final World world;

    public RenderWorld(ISchema schema) {
        this.schema = schema;
        this.world = schema.getLevel();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(@NotNull BlockPos pos) {
        if (this.schema == null) return this.world.getBlockEntity(pos);
        BlockInfo.Mutable.SHARED.set(this.world, pos);
        return this.schema.getRenderFilter().test(pos, BlockInfo.Mutable.SHARED) ? BlockInfo.Mutable.SHARED.getBlockEntity() : null;
    }

    @Override
    public int getCombinedLight(@NotNull BlockPos pos, int lightValue) {
        return this.world.getCombinedLight(pos, lightValue);
    }

    @Override
    public @NotNull BlockState getBlockState(@NotNull BlockPos pos) {
        if (this.schema == null) return this.world.getBlockState(pos);
        BlockInfo.Mutable.SHARED.set(this.world, pos);
        return this.schema.getRenderFilter().test(pos, BlockInfo.Mutable.SHARED) ? BlockInfo.Mutable.SHARED.getBlockState() : Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean isAirBlock(@NotNull BlockPos pos) {
        BlockState state = getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    public @NotNull Biome getBiome(@NotNull BlockPos pos) {
        return this.world.getBiome(pos);
    }

    @Override
    public int getStrongPower(@NotNull BlockPos pos, @NotNull EnumFacing direction) {
        return this.world.getStrongPower(pos, direction);
    }

    @Override
    public @NotNull WorldType getWorldType() {
        return this.world.getWorldType();
    }

    @Override
    public boolean isSideSolid(@NotNull BlockPos pos, @NotNull EnumFacing side, boolean _default) {
        if (!world.isValid(pos)) return _default;

        Chunk chunk = world.getChunk(pos);
        if (chunk == null || chunk.isEmpty()) return _default;
        return getBlockState(pos).isSideSolid(this, pos, side);
    }
}
