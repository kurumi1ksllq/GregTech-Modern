package com.gregtechceu.gtceu.common.pipelike.item;

import com.gregtechceu.gtceu.api.data.chemical.material.properties.ItemPipeProperties;
import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public class LevelItemPipeNet extends LevelPipeNet<ItemPipeProperties, ItemPipeNet> {

    public static LevelItemPipeNet getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(tag -> new LevelItemPipeNet(serverLevel),
                () -> new LevelItemPipeNet(serverLevel), "gtceu_item_pipe_net");
    }

    public LevelItemPipeNet(ServerLevel serverLevel) {
        super(serverLevel);
    }

    @Override
    protected ItemPipeNet createNetInstance() {
        return new ItemPipeNet(this);
    }
}
