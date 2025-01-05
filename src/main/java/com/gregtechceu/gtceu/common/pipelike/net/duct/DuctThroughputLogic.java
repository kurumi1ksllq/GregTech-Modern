package com.gregtechceu.gtceu.common.pipelike.net.duct;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.graphnet.logic.AbstractFloatLogicData;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicEntry;

import org.jetbrains.annotations.NotNull;

public final class DuctThroughputLogic extends AbstractFloatLogicData<DuctThroughputLogic> {

    public static final FloatLogicType<DuctThroughputLogic> TYPE = new FloatLogicType<>(GTCEu.MOD_ID, "DuctThroughput",
            DuctThroughputLogic::new, new DuctThroughputLogic());

    @Override
    public @NotNull FloatLogicType<DuctThroughputLogic> getType() {
        return TYPE;
    }

    @Override
    public DuctThroughputLogic union(NetLogicEntry<?, ?> other) {
        if (other instanceof DuctThroughputLogic l) {
            return this.getValue() < l.getValue() ? this : l;
        } else return this;
    }
}
