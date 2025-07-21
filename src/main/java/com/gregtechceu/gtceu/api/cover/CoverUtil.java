package com.gregtechceu.gtceu.api.cover;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.NotNull;

public class CoverUtil {

    public static AABB getCoverPlateBox(@NotNull Direction side, double plateThickness) {
        return switch (side) {
            case UP -> new AABB(0.0, 1.0 - plateThickness, 0.0, 1.0, 1.0, 1.0);
            case DOWN -> new AABB(0.0, 0.0, 0.0, 1.0, plateThickness, 1.0);
            case NORTH -> new AABB(0.0, 0.0, 0.0, 1.0, 1.0, plateThickness);
            case SOUTH -> new AABB(0.0, 0.0, 1.0 - plateThickness, 1.0, 1.0, 1.0);
            case WEST -> new AABB(0.0, 0.0, 0.0, plateThickness, 1.0, 1.0);
            case EAST -> new AABB(1.0 - plateThickness, 0.0, 0.0, 1.0, 1.0, 1.0);
        };
    }
}
