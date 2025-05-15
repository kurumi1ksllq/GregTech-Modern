package com.gregtechceu.gtceu.api.pattern.pattern;

import com.gregtechceu.gtceu.api.pattern.BetterBlockPos;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CurrentBlockInfo {
    @Setter
    @Getter
    protected Level level;
    @Getter
    private BetterBlockPos pos;
    @Getter
    private BlockState blockState;
    @Getter
    private BlockEntity tileEntity;
    private boolean teResolved;

    public BlockState retrieveCurrentBlockState() {
        if(this.blockState == null) {
            this.blockState = level.getBlockState(pos.immutable());
        }
        return blockState;
    }

    public BlockEntity retrieveCurrentBlockEntity() {
        if(!retrieveCurrentBlockState().hasBlockEntity()) {
            return null;
        }
        if(tileEntity == null && !teResolved) {
            tileEntity = level.getBlockEntity(pos.immutable());
            teResolved = true;
        }
        return tileEntity;
    }

    public void setCurrentPos(BetterBlockPos pos) {
        this.pos = pos;
        updateStateAndEntity();
    }

    public void setCurrentPos(BlockPos pos) {
        this.pos = new BetterBlockPos(pos);
        updateStateAndEntity();
    }

    public BlockPos getBlockPos() {
        return pos.immutable();
    }

    private void updateStateAndEntity() {
        blockState = level.getBlockState(pos.immutable());
        tileEntity = level.getBlockEntity(pos.immutable());
        teResolved = true;
    }

}
