package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.ActivablePipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.MaterialPipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.common.blockentity.*;

import net.minecraft.world.level.block.entity.SignBlockEntity;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import java.util.stream.Stream;

import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;

/**
 * @author KilaBash
 * @date 2023/2/13
 * @implNote GTBlockEntities
 */
@SuppressWarnings("unchecked")
public class GTBlockEntities {

    public static final BlockEntityEntry<PipeBlockEntity> PIPE = REGISTRATE
            .blockEntity("pipe", PipeBlockEntity::new)
            .validBlocks(GTBlocks.DUCT_PIPE_BLOCKS.values().toArray(NonNullSupplier[]::new))
            .register();

    public static final BlockEntityEntry<ActivablePipeBlockEntity> ACTIVATABLE_PIPE = REGISTRATE
            .blockEntity("activatable_pipe", ActivablePipeBlockEntity::new)
            .validBlocks(Stream.of(GTBlocks.OPTICAL_PIPE, GTBlocks.LASER_PIPE).toArray(NonNullSupplier[]::new))
            .register();

    public static final BlockEntityEntry<MaterialPipeBlockEntity> MATERIAL_PIPE = REGISTRATE
            .blockEntity("material_pipe", MaterialPipeBlockEntity::new)
            .validBlocks(Stream.concat(GTMaterialBlocks.CABLE_BLOCKS.values().stream(),
                    GTMaterialBlocks.MATERIAL_PIPE_BLOCKS.values().stream())
                    .toArray(NonNullSupplier[]::new))
            .register();

    public static final BlockEntityEntry<SignBlockEntity> GT_SIGN = REGISTRATE
            .<SignBlockEntity>blockEntity("sign", SignBlockEntity::new)
            .validBlocks(GTBlocks.RUBBER_SIGN,
                    GTBlocks.RUBBER_WALL_SIGN,
                    GTBlocks.TREATED_WOOD_SIGN,
                    GTBlocks.TREATED_WOOD_WALL_SIGN)
            .register();

    public static final BlockEntityEntry<GTHangingSignBlockEntity> GT_HANGING_SIGN = REGISTRATE
            .blockEntity("hanging_sign", GTHangingSignBlockEntity::new)
            .validBlocks(GTBlocks.RUBBER_HANGING_SIGN,
                    GTBlocks.RUBBER_WALL_HANGING_SIGN,
                    GTBlocks.TREATED_WOOD_HANGING_SIGN,
                    GTBlocks.TREATED_WOOD_WALL_HANGING_SIGN)
            .register();

    public static void init() {}
}
