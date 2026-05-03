package com.gregtechceu.gtceu.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SteamCentrifugeMachine extends SteamParallelMultiblockMachine {

    public SteamCentrifugeMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }
}
