package com.gregtechceu.gtceu.api.cover;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;

import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public interface IIOCover {

    ModelProperty<IO> IO_PROPERTY = new ModelProperty<>();

    int getTransferRate();

    IO getIo();

    ManualIOMode getManualIOMode();

    default ModelData getModelData() {
        return ModelData.builder()
                .with(IO_PROPERTY, getIo())
                .build();
    }
}
