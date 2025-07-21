package com.gregtechceu.gtceu.api.graphnet.logic;

import com.gregtechceu.gtceu.GTCEu;

import org.jetbrains.annotations.NotNull;

public final class WeightFactorLogic extends AbstractDoubleLogicData<WeightFactorLogic> {

    public static final DoubleLogicType<WeightFactorLogic> TYPE = new DoubleLogicType<>(GTCEu.id("weight_factor"),
            WeightFactorLogic::new, new WeightFactorLogic().setValue(0));

    @Override
    public @NotNull DoubleLogicType<WeightFactorLogic> getType() {
        return TYPE;
    }

    @Override
    public WeightFactorLogic union(NetLogicEntry<?, ?> other) {
        if (other instanceof WeightFactorLogic l) {
            return TYPE.getWith(this.getValue() + l.getValue());
        } else return this;
    }
}
