package com.gregtechceu.gtceu.api.machine.trait.feature;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

public interface IMultiPartTrait {

    default void addedToController(MultiblockControllerMachine controller) {

    }

    default void removedFromController(MultiblockControllerMachine controller) {

    }

}
