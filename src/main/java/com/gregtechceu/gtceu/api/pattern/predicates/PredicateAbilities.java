package com.gregtechceu.gtceu.api.pattern.predicates;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.world.level.block.Block;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public class PredicateAbilities extends SimplePredicate {

    public PartAbility[] abilities = new PartAbility[0];

    public PredicateAbilities() {
        super("abilities");
    }

    public PredicateAbilities(PartAbility... abilities) {
        this();
        this.abilities = abilities;
        buildPredicate();
    }

    @Override
    public SimplePredicate buildPredicate() {
        abilities = Arrays.stream(abilities).filter(Objects::nonNull).toArray(PartAbility[]::new);
        if (abilities.length == 0) abilities = new PartAbility[] { PartAbility.NONE };
        var blocks = Arrays.stream(abilities).map(PartAbility::getAllBlocks).flatMap(Collection::stream)
                .toArray(Block[]::new);
        predicate = state -> ArrayUtils.contains(blocks, state.getBlockState().getBlock());
        candidates = () -> Arrays.stream(blocks).map(BlockInfo::fromBlock).toArray(BlockInfo[]::new);
        return this;
    }
}
