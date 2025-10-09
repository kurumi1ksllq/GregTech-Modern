package com.gregtechceu.gtceu.common.pipelike.fluidpipe;

import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;
import com.gregtechceu.gtceu.api.pipenet.PipeNet;
import com.gregtechceu.gtceu.common.pipelike.GTPipeNetworks;

public class FluidPipeNet extends PipeNet {

    public FluidPipeNet(LevelPipeNet world) {
        super(world, GTPipeNetworks.FLUID);
    }

    /////////////////////////////////////
    // *********** NBT ***********//
    /////////////////////////////////////
}
