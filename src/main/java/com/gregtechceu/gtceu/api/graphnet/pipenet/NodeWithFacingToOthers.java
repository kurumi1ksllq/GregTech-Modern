package com.gregtechceu.gtceu.api.graphnet.pipenet;

import com.gregtechceu.gtceu.api.graphnet.net.NetNode;

import net.minecraft.core.Direction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NodeWithFacingToOthers {

    @Nullable
    Direction getFacingToOther(@NotNull NetNode other);
}
