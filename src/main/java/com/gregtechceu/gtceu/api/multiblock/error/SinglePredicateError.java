package com.gregtechceu.gtceu.api.multiblock.error;

import com.gregtechceu.gtceu.api.multiblock.predicates.BasePredicate;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

public class SinglePredicateError extends PatternError {

    public final BasePredicate predicate;
    public final ErrorType type;

    public SinglePredicateError(BasePredicate failingPredicate, ErrorType type) {
        super(null, failingPredicate);
        this.predicate = failingPredicate;
        this.type = type;
    }

    @Override
    public List<List<ItemStack>> getCandidates() {
        return Collections.singletonList(predicate.getCandidates());
    }

    @Override
    public List<Component> getErrorInfo() {
        int number = switch (type) {
            case MAX_COUNT -> predicate.maxCount;
            case MIN_COUNT -> predicate.minCount;
            case MAX_LAYER_COUNT -> predicate.maxLayerCount;
            case MIN_LAYER_COUNT -> predicate.minLayerCount;
        };

        return List.of(Component.translatable("gtceu.multiblock.pattern.error.limited." + type.getName(), number));
    }

    public enum ErrorType {

        MAX_COUNT("max_count"),
        MIN_COUNT("min_count"),
        MAX_LAYER_COUNT("max_layer_count"),
        MIN_LAYER_COUNT("min_layer_count");

        @Getter
        String name;

        ErrorType(String name) {
            this.name = name;
        }
    }
}
