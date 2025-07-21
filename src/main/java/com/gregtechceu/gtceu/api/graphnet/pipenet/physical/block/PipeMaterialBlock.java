package com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block;

import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.graphnet.pipenet.IPipeNetNodeHandler;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeMaterialStructure;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.MaterialPipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.common.data.GTBlockEntities;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public abstract class PipeMaterialBlock extends PipeBlock {

    public final @NotNull Material material;

    public PipeMaterialBlock(Properties properties, IPipeMaterialStructure structure, @NotNull Material material) {
        super(properties, structure);
        this.material = material;
    }

    @OnlyIn(Dist.CLIENT)
    public static BlockColor tintedColor() {
        return (state, level, pos, index) -> {
            if (level != null && pos != null && (index == 0 || index == 1)) {
                if (level.getBlockEntity(pos) instanceof IPaintable paintable && paintable.isPainted()) {
                    return paintable.getPaintingColor();
                }
            }
            if (state.getBlock() instanceof PipeMaterialBlock block) {
                return block.tinted(state, level, pos, index);
            }
            return -1;
        };
    }

    public int tinted(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int index) {
        if (index == 0) {
            return material.getMaterialRGB();
        }
        return index == 1 ? material.getMaterialSecondaryRGB() : -1;
    }

    @Nullable
    public static PipeMaterialBlock getBlockFromItem(@NotNull ItemStack stack) {
        if (stack.getItem() instanceof MaterialPipeBlockItem block) return block.getBlock();
        else return null;
    }

    @Override
    public IPipeMaterialStructure getStructure() {
        return (IPipeMaterialStructure) super.getStructure();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MaterialPipeBlockEntity(GTBlockEntities.MATERIAL_PIPE.get(), pos, state);
    }

    @Override
    @NotNull
    public IPipeNetNodeHandler getHandler(PipeBlockEntity tile) {
        return material.getProperty(PropertyKey.PIPENET_PROPERTIES);
    }

    @Override
    protected @NotNull IPipeNetNodeHandler getHandler(@NotNull ItemStack stack) {
        return material.getProperty(PropertyKey.PIPENET_PROPERTIES);
    }

    @Override
    public @NotNull String getDescriptionId() {
        return getStructure().getPrefix().getUnlocalizedName(material);
    }

    @Override
    public @NotNull MutableComponent getName() {
        return getStructure().getPrefix().getLocalizedName(material);
    }

    // tile entity //

    @Override
    public @Nullable MaterialPipeBlockEntity getBlockEntity(@NotNull BlockGetter world, @NotNull BlockPos pos) {
        if (lastTilePos.get().equals(pos)) {
            PipeBlockEntity tile = lastTile.get().get();
            if (tile != null && !tile.isRemoved()) return (MaterialPipeBlockEntity) tile;
        }
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof MaterialPipeBlockEntity pipe) {
            lastTilePos.set(pos.immutable());
            lastTile.set(new WeakReference<>(pipe));
            return pipe;
        } else return null;
    }
}
