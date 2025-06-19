package com.gregtechceu.gtceu.api.multiblock.predicates;

import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;

public class PredicateFluidTag extends BasePredicate {

    public TagKey<Fluid> tag;

    public PredicateFluidTag(TagKey<Fluid> tag) {
        this(null, tag);
    }

    public PredicateFluidTag(String debugName, TagKey<Fluid> tag) {
        this.tag = tag;
        if (tag == null) {
            errorPredicate = state -> null;
            candidates = (map) -> new BlockInfo[] { BlockInfo.fromBlock(Blocks.BARRIER) };
            this.debugName = "nullTag";
            return;
        } else {
            errorPredicate = state -> state.getBlockState().getFluidState().is(tag) ? null :
                    PatternError.PLACEHOLDER;
            candidates = (compoundTag) -> BuiltInRegistries.FLUID.getTag(tag)
                    .stream()
                    .flatMap(HolderSet.Named::stream)
                    .map(Holder::value)
                    .map(fluid -> BlockInfo.fromBlockState(fluid.defaultFluidState().createLegacyBlock()))
                    .toArray(BlockInfo[]::new);
        }

        if (debugName == null) {
            this.debugName = tag.registry().location() + "/" + tag.location();
        } else {
            this.debugName = debugName;
        }
    }
}
