package com.gregtechceu.gtceu.api.multiblock.predicates;

import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class PredicateBlockTag extends SimplePredicate {

    public TagKey<Block> tag;

    public PredicateBlockTag(TagKey<Block> tag) {
        this.tag = tag;
        if (tag == null) {
            predicate = state -> PatternError.PLACEHOLDER;
            candidates = (map) -> new BlockInfo[] { BlockInfo.fromBlock(Blocks.BARRIER) };
        } else {
            predicate = state -> state.getBlockState().is(tag) ? null : PatternError.PLACEHOLDER;
            candidates = (map) -> BuiltInRegistries.BLOCK.getTag(tag)
                    .stream()
                    .flatMap(HolderSet.Named::stream)
                    .map(Holder::value)
                    .map(BlockInfo::fromBlock)
                    .toArray(BlockInfo[]::new);
        }
    }
}
