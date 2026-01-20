package com.gregtechceu.gtceu.client.util;

import net.minecraft.core.Direction;

import lombok.Getter;

/**
 * All possible rotations for a fully orientable block.
 * <p>
 * This code is from
 * <a href=
 * "https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/forge/1.20.1/src/main/java/appeng/api/orientation/BlockOrientation.java">Applied
 * Energistics 2</a>,
 * licensed as LGPL 3.0.
 */
public enum ExtendedBlockModelRotation {

    DOWN_SOUTH(90, 0, 180),
    DOWN_WEST(90, 0, 270),
    DOWN_NORTH(90, 0, 0),
    DOWN_EAST(90, 0, 90),

    UP_SOUTH(270, 0, 0),
    UP_WEST(270, 0, 270),
    UP_NORTH(270, 0, 180),
    UP_EAST(270, 0, 90),

    NORTH_DOWN(0, 0, 180),
    NORTH_WEST(0, 0, 90),
    NORTH_UP(0, 0, 0), // Default
    NORTH_EAST(0, 0, 270),

    SOUTH_DOWN(0, 180, 180),
    SOUTH_WEST(0, 180, 90),
    SOUTH_UP(0, 180, 0),
    SOUTH_EAST(0, 180, 270),

    WEST_DOWN(0, 270, 180),
    WEST_NORTH(0, 270, 90),
    WEST_UP(0, 270, 0),
    WEST_SOUTH(0, 270, 270),

    EAST_DOWN(0, 90, 180),
    EAST_SOUTH(0, 90, 90),
    EAST_UP(0, 90, 0),
    EAST_NORTH(0, 90, 270);

    public static final ExtendedBlockModelRotation[] VALUES = values();

    @Getter
    private final int angleX;
    @Getter
    private final int angleY;
    @Getter
    private final int angleZ;

    ExtendedBlockModelRotation(int angleX, int angleY, int angleZ) {
        this.angleX = angleX;
        this.angleY = angleY;
        this.angleZ = angleZ;
    }

    /**
     * Gets the block orientation in which the block's front and top are facing the specified directions.
     */
    public static ExtendedBlockModelRotation get(Direction frontFacing, Direction upwardsFacing) {
        int z = -1;
        switch (frontFacing) {

            case UP: {
                z = switch (upwardsFacing) {
                    case SOUTH -> 0;
                    case WEST -> 1;
                    case NORTH -> 2;
                    case EAST -> 3;
                    default -> -1;
                };
            }
            case DOWN: {
                z = switch (upwardsFacing) {
                    case SOUTH -> 0;
                    case EAST -> 1;
                    case NORTH -> 2;
                    case WEST -> 3;
                    default -> -1;
                };
            }

            case WEST: {
                z = switch (upwardsFacing) {
                    case DOWN -> 0;
                    case NORTH -> 1;
                    case UP -> 2;
                    case SOUTH -> 3;
                    default -> -1;
                };
            }
            case EAST: {
                z = switch (upwardsFacing) {
                    case DOWN -> 0;
                    case SOUTH -> 1;
                    case UP -> 2;
                    case NORTH -> 3;
                    default -> -1;
                };
            }

            case NORTH: {
                z = switch (upwardsFacing) {
                    case DOWN -> 0;
                    case WEST -> 1;
                    case UP -> 2;
                    case EAST -> 3;
                    default -> -1;
                };
            }
            case SOUTH: {
                z = switch (upwardsFacing) {
                    case DOWN -> 0;
                    case WEST -> 1;
                    case UP -> 2;
                    case EAST -> 3;
                    default -> -1;
                };
            }
        }

        if (z == -1) return NORTH_UP;

        return VALUES[frontFacing.get3DDataValue() * 4 + z];
    }
}
