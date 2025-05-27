package com.gregtechceu.gtceu.api.multiblock.predicates;

import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;

public class PredicateFluids extends SimplePredicate {

    public final Fluid[] fluids;

    public PredicateFluids(Fluid... fluids) {
        if (fluids.length == 0) this.fluids = new Fluid[] { Fluids.WATER };
        else this.fluids = Arrays.stream(fluids).filter(Objects::nonNull).toArray(Fluid[]::new);
        predicate = state -> ArrayUtils.contains(this.fluids, state.getBlockState().getFluidState().getType()) ?
            null : PatternError.PLACEHOLDER;
        candidates = (map) -> Arrays.stream(this.fluids)
                .map(fluid -> BlockInfo.fromBlockState(fluid.defaultFluidState().createLegacyBlock()))
                .toArray(BlockInfo[]::new);
    }
}
