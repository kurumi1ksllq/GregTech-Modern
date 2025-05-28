package com.gregtechceu.gtceu.api.multiblock.error;

import com.gregtechceu.gtceu.api.multiblock.predicates.SimplePredicate;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

public class SinglePredicateError extends PatternError {

    public final SimplePredicate predicate;
    public final ErrorType type;

    public SinglePredicateError(SimplePredicate failingPredicate, ErrorType type) {
        super(null, failingPredicate);
        this.predicate = failingPredicate;
        this.type = type;
    }

    @Override
    public List<List<ItemStack>> getCandidates() {
        return Collections.singletonList(predicate.getCandidates());
    }

    @Override
    public Component getErrorInfo() {
        int number = -1;
        if (type == ErrorType.MAX_COUNT) {
            number = predicate.maxCount;
        }
        if (type == ErrorType.MIN_COUNT) {
            number = predicate.minCount;
        }
        if (type == ErrorType.MAX_LAYER_COUNT) {
            number = predicate.maxLayerCount;
        }
        if (type == ErrorType.MIN_LAYER_COUNT) {
            number = predicate.minLayerCount;
        }
        return Component.translatable("gtceu.multiblock.pattern.error.limited." + type.ordinal(), number);
    }

    public enum ErrorType {
        MAX_COUNT,
        MIN_COUNT,
        MAX_LAYER_COUNT,
        MIN_LAYER_COUNT
    }
}
