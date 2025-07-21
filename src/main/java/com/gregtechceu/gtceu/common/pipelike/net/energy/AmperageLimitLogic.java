package com.gregtechceu.gtceu.common.pipelike.net.energy;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.graphnet.logic.AbstractLongLogicData;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicEntry;

import org.jetbrains.annotations.NotNull;

public final class AmperageLimitLogic extends AbstractLongLogicData<AmperageLimitLogic> {

    public static final LongLogicType<AmperageLimitLogic> TYPE = new LongLogicType<>(GTCEu.id("amperage_limit"),
            AmperageLimitLogic::new, new AmperageLimitLogic());

    @Override
    public @NotNull LongLogicType<AmperageLimitLogic> getType() {
        return TYPE;
    }

    @Override
    public AmperageLimitLogic union(NetLogicEntry<?, ?> other) {
        if (other instanceof AmperageLimitLogic l) {
            return this.getValue() < l.getValue() ? this : l;
        } else return this;
    }
}
