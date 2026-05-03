package com.gregtechceu.gtceu.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SteamMixerMachine extends SteamParallelMultiblockMachine {

    public SteamMixerMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
    }
}
