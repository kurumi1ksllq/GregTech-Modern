package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.common.block.LaserMirrorPipeBlock;
import com.lowdragmc.lowdraglib.client.renderer.IItemRendererProvider;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LaserMirrorPipeBlockItem extends PipeBlockItem implements IItemRendererProvider {
    public LaserMirrorPipeBlockItem(PipeBlock block, Properties props) {
        super(block, props);
    }

    public static ItemColor tintColor() {
        return (stack, index) -> {
            if(stack.getItem() instanceof LaserMirrorPipeBlockItem mirror) {
                return LaserMirrorPipeBlock.tintedColor().getColor(mirror.getBlock().defaultBlockState(), null, null, index);
            }
            return -1;
        };
    }

    @Override
    public @Nullable IRenderer getRenderer(ItemStack stack) {
        return getBlock().getRenderer(getBlock().defaultBlockState());
    }
}
