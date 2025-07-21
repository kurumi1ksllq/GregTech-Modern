package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block.PipeBlock;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.client.renderer.ItemWithBERModelRenderer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MetaMachineItem extends BlockItem {

    public MetaMachineItem(IMachineBlock block, Properties properties) {
        super(block.self(), properties);
    }

    public MachineDefinition getDefinition() {
        return ((IMachineBlock) getBlock()).getDefinition();
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();

        boolean superVal = super.placeBlock(context, state);

        if (!level.isClientSide) {
            BlockPos possiblePipe = pos.relative(side.getOpposite());
            Block block = level.getBlockState(possiblePipe).getBlock();
            if (block instanceof PipeBlock pipeBlock) {
                PipeBlockEntity pipeTile = pipeBlock.getBlockEntity(level, possiblePipe);
                if (pipeTile != null && pipeTile.canConnectTo(side.getOpposite())) {
                    pipeTile.updateActiveStatus(side.getOpposite(), true);
                }
            }
        }
        return superVal;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ItemWithBERModelRenderer.INSTANCE;
            }
        });
    }
}
