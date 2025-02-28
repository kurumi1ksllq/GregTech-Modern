package com.gregtechceu.gtceu.api.pattern.predicates;

import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.gregtechceu.gtceu.api.pattern.util.BlockInfo;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;

public class PredicateStates extends SimplePredicate {

    public final BlockState[] states;

    public PredicateStates(BlockState... states) {
        if (states.length == 0) this.states = new BlockState[] { Blocks.BARRIER.defaultBlockState() };
        else this.states = Arrays.stream(states).filter(Objects::nonNull).toArray(BlockState[]::new);
        predicate = state -> ArrayUtils.contains(this.states, state.getBlockState()) ?
                null : PatternError.PLACEHOLDER;
        candidates = (map) -> Arrays.stream(this.states).map(BlockInfo::fromBlockState).toArray(BlockInfo[]::new);
    }
}
