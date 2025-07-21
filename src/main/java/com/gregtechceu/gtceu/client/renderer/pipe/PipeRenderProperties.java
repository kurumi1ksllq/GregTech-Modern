package com.gregtechceu.gtceu.client.renderer.pipe;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraftforge.client.model.data.ModelProperty;

public class PipeRenderProperties {

    // AbstractPipeModel
    public static final ModelProperty<Material> MATERIAL_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Float> THICKNESS_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Material> FRAME_MATERIAL_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Byte> FRAME_MASK_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Byte> CONNECTED_MASK_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Byte> CLOSED_MASK_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Byte> BLOCKED_MASK_PROPERTY = new ModelProperty<>();
    public static final ModelProperty<Integer> COLOR_PROPERTY = new ModelProperty<>();
    // ActivablePipeModel
    public static final ModelProperty<Boolean> ACTIVE_PROPERTY = new ModelProperty<>();
}
