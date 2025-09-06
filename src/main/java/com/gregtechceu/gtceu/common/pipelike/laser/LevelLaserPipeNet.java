package com.gregtechceu.gtceu.common.pipelike.laser;

import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;

import net.minecraft.server.level.ServerLevel;

public class LevelLaserPipeNet extends LevelPipeNet<LaserPipeProperties, LaserPipeNet> {

    private static final String DATA_ID = "gtceu_laser_pipe_net";

    public static LevelLaserPipeNet getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(tag -> new LevelLaserPipeNet(serverLevel),
                () -> new LevelLaserPipeNet(serverLevel), DATA_ID);
    }

    public LevelLaserPipeNet(ServerLevel serverLevel) {
        super(serverLevel);
    }

    @Override
    protected LaserPipeNet createNetInstance() {
        return new LaserPipeNet(this);
    }
}
