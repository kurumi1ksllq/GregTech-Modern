package com.gregtechceu.gtceu.api.graphnet.net;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.graphnet.GraphClassType;

import org.jetbrains.annotations.NotNull;

public final class BlankNetNode extends NetNode {

    public static final GraphClassType<BlankNetNode> TYPE = new GraphClassType<>(GTCEu.MOD_ID, "BlankNode",
            BlankNetNode::new);

    public BlankNetNode(@NotNull IGraphNet net) {
        super(net);
    }

    @Override
    public @NotNull Object getEquivalencyData() {
        return this;
    }

    @Override
    public @NotNull GraphClassType<BlankNetNode> getType() {
        return TYPE;
    }
}
