package com.gregtechceu.gtceu.api.capability.gregtech;

import com.gregtechceu.gtceu.api.machine.trait.NotifiableComputationContainer;

/**
 * Used in conjunction with {@link NotifiableComputationContainer}.
 */
public interface IOpticalComputationReceiver {

    IOpticalComputationProvider getComputationProvider();
}
