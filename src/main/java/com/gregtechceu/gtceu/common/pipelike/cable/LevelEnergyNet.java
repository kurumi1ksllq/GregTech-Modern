package com.gregtechceu.gtceu.common.pipelike.cable;

import com.gregtechceu.gtceu.api.data.chemical.material.properties.WireProperties;
import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;

import net.minecraft.server.level.ServerLevel;

public class LevelEnergyNet extends LevelPipeNet<WireProperties, EnergyNet> {

    public static LevelEnergyNet getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(tag -> new LevelEnergyNet(serverLevel),
                () -> new LevelEnergyNet(serverLevel), "gtcue_energy_net");
    }

    public LevelEnergyNet(ServerLevel serverLevel) {
        super(serverLevel);
    }

    @Override
    protected EnergyNet createNetInstance() {
        return new EnergyNet(this);
    }
}
