package com.gregtechceu.gtceu.common.pipelike.net.energy;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.graphnet.logic.AbstractDoubleLogicData;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicEntry;

import org.jetbrains.annotations.NotNull;

public final class VoltageLossLogic extends AbstractDoubleLogicData<VoltageLossLogic> {

    public static final DoubleLogicType<VoltageLossLogic> TYPE = new DoubleLogicType<>(GTCEu.id("voltage_loss"),
            VoltageLossLogic::new, new VoltageLossLogic());

    @Override
    public @NotNull DoubleLogicType<VoltageLossLogic> getType() {
        return TYPE;
    }

    @Override
    public VoltageLossLogic union(NetLogicEntry<?, ?> other) {
        if (other instanceof VoltageLossLogic l) {
            return this.getWith(this.getValue() + l.getValue());
        } else return this;
    }
}
