package com.gregtechceu.gtceu.common.pipelike.block.pipe;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IBurnable;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IFreezable;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block.PipeMaterialBlock;

import org.jetbrains.annotations.NotNull;

public class MaterialPipeBlock extends PipeMaterialBlock implements IBurnable, IFreezable {

    public MaterialPipeBlock(Properties properties, MaterialPipeStructure structure, @NotNull Material material) {
        super(properties, structure, material);
    }

    @Override
    public MaterialPipeStructure getStructure() {
        return (MaterialPipeStructure) super.getStructure();
    }
}
