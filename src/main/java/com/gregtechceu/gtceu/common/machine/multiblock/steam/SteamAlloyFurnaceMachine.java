package com.gregtechceu.gtceu.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SteamAlloyFurnaceMachine extends SteamParallelMultiblockMachine {

    public SteamAlloyFurnaceMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }
}
