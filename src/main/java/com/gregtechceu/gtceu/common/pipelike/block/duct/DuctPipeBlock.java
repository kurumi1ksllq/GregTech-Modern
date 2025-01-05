package com.gregtechceu.gtceu.common.pipelike.block.duct;

import com.gregtechceu.gtceu.api.graphnet.pipenet.IPipeNetNodeHandler;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block.PipeBlock;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.common.pipelike.handlers.DuctNetHandler;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

public class DuctPipeBlock extends PipeBlock {

    public DuctPipeBlock(Properties properties, DuctStructure structure) {
        super(properties, structure);
    }

    @Override
    @NotNull
    public IPipeNetNodeHandler getHandler(PipeBlockEntity blockEntityContext) {
        return DuctNetHandler.INSTANCE;
    }

    @Override
    protected @NotNull IPipeNetNodeHandler getHandler(@NotNull ItemStack stack) {
        return DuctNetHandler.INSTANCE;
    }
}
