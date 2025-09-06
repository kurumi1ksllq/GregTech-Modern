package com.gregtechceu.gtceu.common.pipelike.fluidpipe;

import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidPipeProperties;
import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;

import net.minecraft.server.level.ServerLevel;

public class LevelFluidPipeNet extends LevelPipeNet<FluidPipeProperties, FluidPipeNet> {

    public static LevelFluidPipeNet getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(tag -> new LevelFluidPipeNet(serverLevel),
                () -> new LevelFluidPipeNet(serverLevel), "gtcue_fluid_pipe_net");
    }

    public LevelFluidPipeNet(ServerLevel serverLevel) {
        super(serverLevel);
    }

    @Override
    protected FluidPipeNet createNetInstance() {
        return new FluidPipeNet(this);
    }
}
