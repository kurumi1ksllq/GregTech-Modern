package com.gregtechceu.gtceu.api.multiblock.util;

import com.gregtechceu.gtceu.utils.GTUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.function.BinaryOperator;
import java.util.function.ToIntFunction;

/**
 * Relative direction when facing horizontally
 */
public enum RelativeDirection {

    UP((f, u) -> u),
    DOWN((f, u) -> u.getOpposite()),
    LEFT((f, u) -> GTUtil.cross(f, u).getOpposite()),
    RIGHT(GTUtil::cross),
    FRONT((f, u) -> f),
    BACK((f, u) -> f.getOpposite());

    final BinaryOperator<Direction> facingFunction;

    public static final RelativeDirection[] VALUES = values();

    RelativeDirection(BinaryOperator<Direction> facingFunction) {
        this.facingFunction = facingFunction;
    }

    public RelativeDirection getOpposite() {
        return VALUES[oppositeOrdinal()];
    }

    public int oppositeOrdinal() {
        return ordinal() ^ 1;
    }

    public Direction getRelativeFacing(Direction frontFacing, Direction upwardsFacing) {
        if(frontFacing.getAxis() == upwardsFacing.getAxis()) {
            throw new IllegalArgumentException("front facing and up facing must be on different axes");
        }
        return facingFunction.apply(frontFacing, upwardsFacing);
    }

    public Direction getRelativeFacing(Direction frontFacing, Direction upwardsFacing, boolean isFlipped) {
        return (isFlipped && (this == LEFT || this == RIGHT)) ?
                getRelativeFacing(frontFacing, upwardsFacing).getOpposite() :
                getRelativeFacing(frontFacing, upwardsFacing);
    }

    public ToIntFunction<BlockPos> getSorter(Direction frontFacing, Direction upwardsFacing, boolean isFlipped) {
        // get the direction to go in for the part sorter
        Direction sorterDirection = getRelativeFacing(frontFacing, upwardsFacing, isFlipped);

        // Determined by Direction.Axis + Direction.AxisDirection
        return switch (sorterDirection) {
            case UP -> BlockPos::getY;
            case DOWN -> pos -> -pos.getY();
            case EAST -> BlockPos::getX;
            case WEST -> pos -> -pos.getX();
            case NORTH -> pos -> -pos.getZ();
            case SOUTH -> BlockPos::getZ;
        };
    }

    /**
     * Simulates rotating the controller around an axis to get to a new front facing.
     *
     * @return Returns the new upwards facing.
     */
    public static Direction simulateAxisRotation(Direction newFrontFacing, Direction oldFrontFacing,
                                                 Direction upwardsFacing) {
        if(newFrontFacing.getAxis() == oldFrontFacing.getAxis()) return upwardsFacing;

        Direction cross = GTUtil.cross(newFrontFacing, oldFrontFacing);

        assert cross != null;
        if(cross.getAxis() == upwardsFacing.getAxis()) return upwardsFacing;

        if(oldFrontFacing.getClockWise(cross.getAxis()) == newFrontFacing)
            return oldFrontFacing.getOpposite();

        return oldFrontFacing;
    }

    /**
     * Offset a BlockPos relatively in any direction by any amount. Pass negative values to offset down, right or
     * backwards.
     */
    public static BlockPos offsetPos(BlockPos pos, Direction frontFacing, Direction upwardsFacing, boolean isFlipped,
                                     int upOffset, int leftOffset, int forwardOffset) {
        BlockPos.MutableBlockPos mbp = pos.mutable();
        mbp.move(UP.getRelativeFacing(frontFacing, upwardsFacing, isFlipped), upOffset);
        mbp.move(LEFT.getRelativeFacing(frontFacing, upwardsFacing, isFlipped), leftOffset);
        mbp.move(FRONT.getRelativeFacing(frontFacing, upwardsFacing, isFlipped), forwardOffset);
        return mbp.immutable();
    }

    public static <T extends Enum<T>> void validateFacingsArray(T[] facings) {
        if(facings.length != 3) throw new IllegalArgumentException("Facings must be array of length 3!");

        int c = 0;
        for(int i = 0; i < 3; i++) {
            c |= (1 << facings[i].ordinal() / 2);
        }

        if(c != 7) throw new IllegalArgumentException("The 3 facings must use each axis exactly once!");
    }
}
