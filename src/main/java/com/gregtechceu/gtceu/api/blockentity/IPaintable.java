package com.gregtechceu.gtceu.api.blockentity;

import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface IPaintable {

    BooleanProperty IS_PAINTED_PROPERTY = BooleanProperty.create("is_painted");
    int UNPAINTED_COLOR = 0xffffffff;

    /**
     * Get painting color.
     * It's not the real color of this block.
     * 
     * @return -1 - non painted.
     */
    int getPaintingColor();

    void setPaintingColor(int color);

    /**
     * Default color.
     */
    int getDefaultPaintingColor();

    /**
     * If the block is painted.
     */
    default boolean isPainted() {
        return getPaintingColor() != UNPAINTED_COLOR && getPaintingColor() != getDefaultPaintingColor();
    }

    /**
     * Get the real color of this block.
     */
    default int getRealColor() {
        return isPainted() ? getPaintingColor() : getDefaultPaintingColor();
    }

    @OnlyIn(Dist.CLIENT)
    static int tintedColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int index) {
        if (pos != null && level != null && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) {
            if (pipe.isPainted()) {
                return pipe.getRealColor();
            }
        }
        return -1;
    }
}
