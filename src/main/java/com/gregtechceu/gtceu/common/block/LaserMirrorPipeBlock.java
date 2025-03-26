package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.client.model.PipeModel;
import com.gregtechceu.gtceu.client.renderer.block.PipeBlockRenderer;
import com.gregtechceu.gtceu.common.blockentity.LaserMirrorPipeBlockEntity;
import com.gregtechceu.gtceu.common.blockentity.LaserPipeBlockEntity;
import com.gregtechceu.gtceu.common.data.GTBlockEntities;
import com.gregtechceu.gtceu.common.pipelike.laser.LaserPipeProperties;
import com.gregtechceu.gtceu.common.pipelike.laser.LaserPipeType;
import com.gregtechceu.gtceu.common.pipelike.laser.LevelLaserPipeNet;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LaserMirrorPipeBlock extends PipeBlock<LaserPipeType, LaserPipeProperties, LevelLaserPipeNet> {

    private final LaserPipeProperties properties;
    public final PipeModel model;
    public final PipeBlockRenderer renderer;

    public LaserMirrorPipeBlock(Properties props, LaserPipeType type) {
        super(props, type);
        this.properties = LaserPipeProperties.INSTANCE;
        this.model = new PipeModel(LaserPipeType.NORMAL.getThickness(), () -> GTCEu.id(""), () -> GTCEu.id(""), null, null);
        this.renderer = new PipeBlockRenderer(this.model);
    }

    public static BlockColor tintedColor() {
        return (state, level, pos, index) -> {
            if (pos != null && level != null && level.getBlockEntity(pos) instanceof PipeBlockEntity<?, ?> pipe) {

                if(pipe.getFrameMaterial() != null) {
                    if(index == 3) {
                        return pipe.getFrameMaterial().getMaterialRGB();
                    } else if (index == 4) {
                        return pipe.getFrameMaterial().getMaterialSecondaryRGB();
                    }
                }
                if(pipe.isPainted()) {
                    return pipe.getRealColor();
                }
            }
            return -1;
        };
    }

    @Override
    public LevelLaserPipeNet getWorldPipeNet(ServerLevel level) {
        return LevelLaserPipeNet.getOrCreate(level);
    }

    @Override
    public BlockEntityType<? extends PipeBlockEntity<LaserPipeType, LaserPipeProperties>> getBlockEntityType() {
        return GTBlockEntities.LASER_MIRROR.get();
    }

    @Override
    public LaserPipeProperties createRawData(BlockState pState, @Nullable ItemStack pStack) {
        return LaserPipeProperties.INSTANCE;
    }

    @Override
    public LaserPipeProperties createProperties(IPipeNode<LaserPipeType, LaserPipeProperties> pipeTile) {
        LaserPipeType pipeType = pipeTile.getPipeType();
        if(pipeType == null) return getFallbackType();
        return this.pipeType.modifyProperties(properties);
    }

    @Override
    public LaserPipeProperties getFallbackType() {
        return LaserPipeProperties.INSTANCE;
    }

    @Override
    public @Nullable PipeBlockRenderer getRenderer(BlockState state) {
        return renderer;
    }

    @Override
    protected PipeModel getPipeModel() {
        return model;
    }

    @Override
    public boolean canPipesConnect(IPipeNode<LaserPipeType, LaserPipeProperties> selfTile, Direction side, IPipeNode<LaserPipeType, LaserPipeProperties> sideTile) {
        return selfTile instanceof LaserMirrorPipeBlockEntity && sideTile instanceof LaserPipeBlockEntity;
    }

    @Override
    public boolean canPipeConnectToBlock(IPipeNode<LaserPipeType, LaserPipeProperties> selfTile, Direction side, @Nullable BlockEntity tile) {
        return tile instanceof LaserPipeBlockEntity;
    }
}
