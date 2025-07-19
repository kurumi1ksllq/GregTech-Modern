package com.gregtechceu.gtceu.core.mixins.client;

import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DefaultedVertexConsumer.class)
public interface DefaultedVertexConsumerAccessor {

    @Accessor("defaultColorSet")
    boolean gtceu$isDefaultColorSet();

    @Accessor("defaultA")
    int gtceu$getDefaultA();

    @Accessor("defaultR")
    int gtceu$getDefaultR();

    @Accessor("defaultG")
    int gtceu$getDefaultG();

    @Accessor("defaultB")
    int gtceu$getDefaultB();
}
