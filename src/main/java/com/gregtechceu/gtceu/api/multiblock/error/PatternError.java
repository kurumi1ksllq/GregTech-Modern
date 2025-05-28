package com.gregtechceu.gtceu.api.multiblock.error;

import com.gregtechceu.gtceu.api.multiblock.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.multiblock.pattern.CurrentBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.predicates.SimplePredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

public class PatternError {

    /**
     * Return this for your pattern errors if you want them to be a default error with the pos of the BlockWorldState
     * and candidates of the simple predicate's error.
     */
    public static final PatternError PLACEHOLDER = new PatternError(BlockPos.ZERO, Collections.emptyList());
    protected BlockPos pos;
    @Getter
    protected List<List<ItemStack>> candidates;
    @Setter
    protected CurrentBlockInfo blockInfo;

    public PatternError(BlockPos pos, List<List<ItemStack>> candidates) {
        this.pos = pos;
        this.candidates = candidates;
    }

    public PatternError(BlockPos pos, TraceabilityPredicate predicate) {
        this(pos, predicate.getCandidates());
    }

    public PatternError(BlockPos pos, SimplePredicate failingPredicate) {
        this(pos, Collections.singletonList(failingPredicate.getCandidates()));
    }

    public Level getWorld() {
        return blockInfo.getLevel();
    }

    public BlockPos getPos() {
        return blockInfo.getBlockPos();
    }

    public Component getErrorInfo() {
        StringBuilder builder = new StringBuilder();
        for (List<ItemStack> candidate : candidates) {
            if (!candidate.isEmpty()) {
                builder.append(candidate.get(0));
                builder.append(", ");
            }
        }
        builder.append("...");
        return Component.translatable("gtceu.multiblock.pattern.error", builder.toString(), pos.toString());
    }
}
