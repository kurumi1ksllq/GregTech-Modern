package com.gregtechceu.gtceu.api.block;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.pipenet.*;
import com.gregtechceu.gtceu.client.model.PipeModel;
import com.gregtechceu.gtceu.client.renderer.block.PipeBlockRenderer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * MaterialPipeBlock is an abstract class that provides methods to get the properties of Material Pipes.
 * This is useful for Material Pipes that have different properties based on the type of pipe.
 * For example, a Material Pipe that provides different textures based on the material of the pipe.
 * @param <PipeType> the type of pipe
 * @param <NodeDataType> the type of data stored in the pipe
 * @param <WorldPipeNetType> the type of pipe network in the world
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class MaterialPipeBlock<
        PipeType extends Enum<PipeType> & IPipeType<NodeDataType> & IMaterialPipeType<NodeDataType>, NodeDataType,
        WorldPipeNetType extends LevelPipeNet<NodeDataType, ? extends PipeNet<NodeDataType>>>
                                       extends PipeBlock<PipeType, NodeDataType, WorldPipeNetType> {

    public final Material material;
    public final PipeBlockRenderer renderer;
    public final PipeModel model;

    /**
     * MaterialPipeBlock is a constructor that creates a Material Pipe Block.
     * @param properties the properties of the block
     * @param pipeType the type of pipe
     * @param material the material of the pipe
     */
    public MaterialPipeBlock(Properties properties, PipeType pipeType, Material material) {
        super(properties, pipeType);
        this.material = material;
        this.model = createPipeModel();
        this.renderer = new PipeBlockRenderer(this.model);
    }

    /**
     * The function that returns the tinted color of the pipe.
     * @implNote This is only used on the client side.
     * @return the tinted color of the pipe
     */
    @OnlyIn(Dist.CLIENT)
    public static BlockColor tintedColor() {
        return (blockState, level, blockPos, index) -> {
            if (blockState.getBlock() instanceof MaterialPipeBlock<?, ?, ?> block) {
                if (blockPos != null && level != null &&
                        level.getBlockEntity(blockPos) instanceof PipeBlockEntity<?, ?> pipe) {
                    if (pipe.getFrameMaterial() != null) {
                        if (index == 3) {
                            return pipe.getFrameMaterial().getMaterialRGB();
                        } else if (index == 4) {
                            return pipe.getFrameMaterial().getMaterialSecondaryRGB();
                        }
                    }
                    else if (pipe.isPainted()) {
                        return pipe.getRealColor();
                    }
                }
                return block.tinted(blockState, level, blockPos, index);
            }
            return -1;
        };
    }

    /**
     * The function that returns the tinted color of the pipe.
     * @param blockState the {@link BlockState} of the pipe
     * @param blockAndTintGetter the {@link BlockAndTintGetter} of the pipe
     * @param blockPos the {@link BlockPos} of the pipe
     * @param index the index of the tinted color
     * @return the tinted color of the pipe
     */
    public int tinted(BlockState blockState, @Nullable BlockAndTintGetter blockAndTintGetter,
                      @Nullable BlockPos blockPos, int index) {
        return index == 0 || index == 1 ? material.getMaterialRGB() : -1;
    }

    /**
     * The function that returns the model of the pipe.
     * @return the model of the pipe
     */
    @Override
    protected PipeModel getPipeModel() {
        return model;
    }

    /**
     * TODO: The function that creates the raw data of the pipe?
     * @param pState the {@link BlockState} of the pipe
     * @param pStack the {@link ItemStack} of the pipe
     */
    @Override
    public final NodeDataType createRawData(BlockState pState, @Nullable ItemStack pStack) {
        return createMaterialData();
    }

    /**
     * The function that creates the properties of the pipe.
     * @param pipeTile the {@link IPipeNode} of the pipe
     * @return the properties of the pipe
     */
    @Override
    public NodeDataType createProperties(IPipeNode<PipeType, NodeDataType> pipeTile) {
        PipeType pipeType = pipeTile.getPipeType();
        Material material = ((MaterialPipeBlock<PipeType, NodeDataType, WorldPipeNetType>) pipeTile
                .getPipeBlock()).material;
        if (pipeType == null || material == null) {
            return getFallbackType();
        }
        return createProperties(pipeType, material);
    }

    /**
     * The function that creates the properties of the pipe.
     * @param pipeType the type of pipe
     * @param material the material of the pipe
     * @return the properties of the pipe
     */
    protected abstract NodeDataType createProperties(PipeType pipeType, Material material);

    /**
     * The function that returns the renderer of the pipe.
     * @param state the {@link BlockState} of the pipe
     * @return the renderer of the pipe
     */
    @Override
    public @Nullable PipeBlockRenderer getRenderer(BlockState state) {
        return renderer;
    }

    /**
     * The function that returns the fallback type of the pipe.
     * @return the fallback type of the pipe
     */
    @Override
    public final NodeDataType getFallbackType() {
        return createMaterialData();
    }

    /**
     * The function that creates the material data of the pipe.
     * @return the material data of the pipe
     */
    protected abstract NodeDataType createMaterialData();

    /**
     * The function that creates the pipe model of the pipe.
     * @return the pipe model of the pipe
     */
    protected abstract PipeModel createPipeModel();

    /**
     * The function that returns the description ID of the pipe.
     * @return the unlocalized name of the pipe
     */
    @Override
    public String getDescriptionId() {
        return pipeType.getTagPrefix().getUnlocalizedName(material);
    }

    /**
     * The function that returns the name of the pipe.
     * @return the localized name of the pipe
     */
    @Override
    public MutableComponent getName() {
        return pipeType.getTagPrefix().getLocalizedName(material);
    }
}