package com.gregtechceu.gtceu.api.pattern.predicates;

import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;

public class PredicateBlocks extends SimplePredicate {

    public Block[] blocks;



    public PredicateBlocks(Block... blocks) {
        this.blocks = blocks;
        this.type = "Blocks";

        blocks = Arrays.stream(blocks).filter(Objects::nonNull).toArray(Block[]::new);
        if (blocks.length == 0) blocks = new Block[] { Blocks.BARRIER };
        Block[] finalBlocks = blocks;
        predicate = state -> ArrayUtils.contains(finalBlocks, state.getBlockState().getBlock()) ?
                null : PatternError.PLACEHOLDER;

        candidates = (map) -> Arrays.stream(finalBlocks)
                .map(BlockInfo::fromBlock)
                .toArray(BlockInfo[]::new);

        buildPredicate();
    }
}
