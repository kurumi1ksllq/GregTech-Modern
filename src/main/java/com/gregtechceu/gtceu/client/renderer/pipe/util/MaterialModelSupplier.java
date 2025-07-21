package com.gregtechceu.gtceu.client.renderer.pipe.util;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.client.renderer.pipe.AbstractPipeModel;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface MaterialModelSupplier {

    @NotNull
    AbstractPipeModel<?> getModel(@NotNull Material material);
}
