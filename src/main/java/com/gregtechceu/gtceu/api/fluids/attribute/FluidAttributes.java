package com.gregtechceu.gtceu.api.fluids.attribute;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.utils.GTUtil;

public final class FluidAttributes {

    /**
     * Attribute for acidic fluids.
     */
    public static final FluidAttribute ACID = new FluidAttribute(GTCEu.id("acid"),
            list -> list.accept(GTUtil.translatable("gtceu.fluid.type_acid.tooltip")),
            list -> list.accept(GTUtil.translatable("gtceu.fluid_pipe.acid_proof")));

    private FluidAttributes() {}
}
