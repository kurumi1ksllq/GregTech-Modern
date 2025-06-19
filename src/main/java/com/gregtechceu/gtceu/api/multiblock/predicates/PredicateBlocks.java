package com.gregtechceu.gtceu.api.multiblock.predicates;

import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

public class PredicateBlocks extends BasePredicate {

    public Block[] blocks;

    public PredicateBlocks(Block... blocks) {
        this(null, blocks);
    }

    public PredicateBlocks(String debugName, Block... blocks) {
        this.blocks = blocks;

        blocks = Arrays.stream(blocks).filter(Objects::nonNull).toArray(Block[]::new);
        if (blocks.length == 0) blocks = new Block[] { Blocks.BARRIER };
        Block[] finalBlocks = blocks;
        errorPredicate = state -> ArrayUtils.contains(finalBlocks, state.getBlockState().getBlock()) ?
                null : PatternError.PLACEHOLDER;

        candidates = (tag) -> Arrays.stream(finalBlocks)
                .map(BlockInfo::fromBlock)
                .toArray(BlockInfo[]::new);

        if (debugName == null) {
            StringJoiner sb = new StringJoiner("-");
            for (Block b : blocks) {
                sb.add(BuiltInRegistries.BLOCK.getKey(b).getPath());
            }
            this.debugName = sb.toString();
        } else {
            this.debugName = debugName;
        }
    }
}
