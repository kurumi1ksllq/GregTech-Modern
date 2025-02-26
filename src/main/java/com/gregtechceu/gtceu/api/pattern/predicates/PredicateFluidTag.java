package com.gregtechceu.gtceu.api.pattern.predicates;

import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;

public class PredicateFluidTag extends SimplePredicate {

    public TagKey<Fluid> tag;

    public PredicateFluidTag(TagKey<Fluid> tag) {
        if (tag == null) {
            predicate = state -> null;
            candidates = (map) -> new BlockInfo[] { BlockInfo.fromBlock(Blocks.BARRIER) };
        } else {
            predicate = state -> state.getBlockState().getFluidState().is(tag) ? null :
                PatternError.PLACEHOLDER;
            candidates = (map) -> BuiltInRegistries.FLUID.getTag(tag)
                    .stream()
                    .flatMap(HolderSet.Named::stream)
                    .map(Holder::value)
                    .map(fluid -> BlockInfo.fromBlockState(fluid.defaultFluidState().createLegacyBlock()))
                    .toArray(BlockInfo[]::new);
        }
    }
}
