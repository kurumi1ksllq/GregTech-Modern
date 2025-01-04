package com.gregtechceu.gtceu.common.pipelike.net.duct;

import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.graphnet.path.NetPath;

import org.jetbrains.annotations.NotNull;

public interface DuctPath extends NetPath {

    @NotNull
    PathFlowReport traverse(MedicalCondition condition, float differenceAmount);

    interface PathFlowReport {

        /**
         * @return the total voltage that was allowed through the path
         */
        MedicalCondition conditionOut();

        /**
         * @return the total amperage that was allowed through the path
         */
        float amountOut();

        /**
         * Called when this flow report should stop being simulated;
         * e.g. flow should be reported and heating should occur.
         */
        void report();
    }
}
