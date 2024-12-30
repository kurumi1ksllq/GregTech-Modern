package com.gregtechceu.gtceu.api.capability.gregtech;

public interface IOpticalDataAccessHatch extends IDataAccessHatch {

    /**
     * @return if this hatch transmits data through cables
     */
    boolean isTransmitter();
}
