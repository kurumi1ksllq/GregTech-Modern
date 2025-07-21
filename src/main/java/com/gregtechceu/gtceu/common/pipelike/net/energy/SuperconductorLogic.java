package com.gregtechceu.gtceu.common.pipelike.net.energy;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.graphnet.logic.AbstractByteLogicData;

import org.jetbrains.annotations.NotNull;

public final class SuperconductorLogic extends AbstractByteLogicData<SuperconductorLogic> {

    public static final SuperconductorLogic INSTANCE = new SuperconductorLogic();

    public static final ByteLogicType<SuperconductorLogic> TYPE = new ByteLogicType<>(GTCEu.id("superconductor"),
            () -> INSTANCE, INSTANCE);

    @Override
    public @NotNull ByteLogicType<SuperconductorLogic> getType() {
        return TYPE;
    }
}
