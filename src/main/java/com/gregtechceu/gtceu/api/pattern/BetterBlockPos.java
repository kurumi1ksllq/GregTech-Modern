package com.gregtechceu.gtceu.api.pattern;

import com.google.common.collect.AbstractIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;

public class BetterBlockPos {

    public static final int NUM_X_BITS = 1 + 32 - Integer.numberOfLeadingZeros(30_000_000 - 1);
    public static final int NUM_Z_BITS = NUM_X_BITS, NUM_Y_BITS = 64 - 2 * NUM_X_BITS;
    public static final int Y_SHIFT = NUM_Z_BITS;
    public static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS;
    public static final long Z_MASK = (1L << NUM_Z_BITS) - 1;
    public static final long Y_MASK = (1L << (NUM_Z_BITS + NUM_Y_BITS)) - 1;

    protected final int[] pos;

    public BetterBlockPos() {
        this(0, 0,0);
    }

    public BetterBlockPos(int x, int y, int z) {
        pos = new int[] {x, y, z};
    }

    public BetterBlockPos(BlockPos base) {
        pos = new int[] {base.getX(), base.getY(), base.getZ()};
    }

    public BetterBlockPos(long l) {
        pos = new int[3];
        fromLong(l);
    }

    public BetterBlockPos set(Direction.Axis axis, int value) {
        return set(axis.ordinal(), value);
    }

    public BetterBlockPos set(Direction.Axis axis, BetterBlockPos other) {
        return set(axis.ordinal(), other.pos[axis.ordinal()]);
    }

    public BetterBlockPos set(int index, int value) {
        pos[index] = value;
        return this;
    }

    public BetterBlockPos x(int value) {
        return set(0, value);
    }

    public BetterBlockPos y(int value) {
        return set(1, value);
    }

    public BetterBlockPos z(int value) {
        return set(2, value);
    }

    public BetterBlockPos setAxisRelative(Direction.Axis a1, Direction.Axis a2, int p1, int p2, int p3) {
        set(a1, p1);
        set(a2, p2);
        pos[3 - a1.ordinal() - a2.ordinal()] = p3;
        return this;
    }

    public BetterBlockPos offset(Direction facing, int amount) {
        pos[0] += facing.getStepX() * amount;
        pos[1] += facing.getStepY() * amount;
        pos[2] += facing.getStepZ() * amount;
        return this;
    }

    public BetterBlockPos from(BetterBlockPos other) {
        System.arraycopy(other.pos, 0, pos, 0, 3);
        return this;
    }

    public BetterBlockPos from(BlockPos other) {
        pos[0] = other.getX();
        pos[1] = other.getY();
        pos[2] = other.getZ();
        return this;
    }

    public BetterBlockPos offset(Direction direction) {
        return offset(direction, 1);
    }

    public long toLong() {
        return (long) pos[0] << X_SHIFT | ((long) pos[1] << Y_SHIFT) & Y_MASK | (pos[2] & Z_MASK);
    }

    /**
     * Sets this pos to the long
     *
     * @param l Serialized long, from {@link BetterBlockPos#toLong()}
     * @see BetterBlockPos#BetterBlockPos(long)
     */
    public BetterBlockPos fromLong(long l) {
        pos[0] = (int) (l >> X_SHIFT);
        pos[1] = (int) ((l & Y_MASK) >> Y_SHIFT);
        pos[2] = (int) (l & Z_MASK);
        return this;
    }

    /**
     * Adds the other pos's position to this pos.
     *
     * @param other The other pos, not mutated.
     */
    public BetterBlockPos add(BetterBlockPos other) {
        pos[0] += other.pos[0];
        pos[1] += other.pos[1];
        pos[2] += other.pos[2];
        return this;
    }

    /**
     * Subtracts the other pos's position to this pos.
     *
     * @param other The other pos, not mutated.
     */
    public BetterBlockPos subtract(BetterBlockPos other) {
        pos[0] -= other.pos[0];
        pos[1] -= other.pos[1];
        pos[2] -= other.pos[2];
        return this;
    }

    /**
     * Same as {@link BetterBlockPos#subtract(BetterBlockPos)} but sets this pos to be the absolute value of the
     * operation.
     *
     * @param other The other pos, not mutated.
     */
    public BetterBlockPos diff(BetterBlockPos other) {
        pos[0] = Math.abs(pos[0] - other.pos[0]);
        pos[1] = Math.abs(pos[1] - other.pos[1]);
        pos[2] = Math.abs(pos[2] - other.pos[2]);
        return this;
    }

    /**
     * Sets all 3 coordinates to 0.
     */
    public BetterBlockPos zero() {
        Arrays.fill(pos, 0);
        return this;
    }

    /**
     * @return True if all 3 of the coordinates are 0.
     */
    public boolean origin() {
        return equals(BlockPos.ZERO);
    }

    /**
     * @return A new immutable instance of {@link BlockPos}
     */
    public BlockPos immutable() {
        return new BlockPos(pos[0], pos[1], pos[2]);
    }

    /**
     * Gets a coordinate associated with the index, X = 0, Y = 1, Z = 2
     */
    public int get(int index) {
        return pos[index];
    }

    /**
     * Gets a coordinate associated with the axis
     */
    public int get(Direction.Axis axis) {
        return pos[axis.ordinal()];
    }

    /**
     * Gets the x value.
     */
    public int x() {
        return get(0);
    }

    /**
     * Gets the y value.
     */
    public int y() {
        return get(1);
    }

    /**
     * Gets the z value.
     */
    public int z() {
        return get(2);
    }

    /**
     * Gets a copy of the internal array, in xyz.
     */
    public int[] getAll() {
        return Arrays.copyOf(pos, 3);
    }

    public BetterBlockPos copy() {
        return new BetterBlockPos().from(this);
    }

    @Override
    public int hashCode() {
        // should be identical to blockpos
        return (pos[1] + pos[2] * 31) * 31 + pos[0];
    }

    @Override
    public String toString() {
        return super.toString() + "{x=" + pos[0] + ", y=" + pos[1] + ", z=" + pos[2] + "}";
    }

    /**
     * Compares for the same coordinate.
     *
     * @param other The object to compare to, can be either GreggyBlockPos or BlockPos
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) return false;

        if (other instanceof BetterBlockPos bbp) {
            return Arrays.equals(pos, bbp.pos);
        } else if (other instanceof BlockPos p) {
            return p.getX() == pos[0] && p.getY() == pos[1] && p.getZ() == pos[2];
        }

        return false;
    }

    /**
     * Static version of {@link BetterBlockPos#toLong()}
     */
    public static long toLong(int x, int y, int z) {
        return (long) x << X_SHIFT | ((long) y << Y_SHIFT) & Y_MASK | (z & Z_MASK);
    }

    public static <T extends Enum<T>> void validateFacingsArray(T[] facings) {
        if(facings.length != 3) throw new IllegalArgumentException("Facings must be array of length 3!");

        int c = 0;
        for(int i = 0; i < 3; i++) {
            c |= (1 << facings[i].ordinal() / 2);
        }

        if(c != 7) throw new IllegalArgumentException("The 3 facings must use each axis exactly once!");
    }

    public static BetterBlockPos startPos(BetterBlockPos start, BetterBlockPos end, Direction... facings) {
        validateFacingsArray(facings);

        BetterBlockPos s = new BetterBlockPos();
        for(int i = 0; i < 3; i++) {
            int a = start.get(facings[i].getAxis());
            int b = end.get(facings[i].getAxis());
            int mult = facings[i].getAxisDirection().getStep();

            s.set(facings[i].getAxis(), Math.min(a * mult, b * mult) * mult);
        }

        return s;
    }

    /**
     * Returns an iterable going over all blocks in the cube. Although this iterator returns a mutable pos, it has
     * an internal pos that sets the mutable one so modifying the mutable pos is safe. The iterator starts at one of the
     * 8 points on the cube.
     * Which of the 8 is determined by the 3 facings, the selected one is the one in the least {facing} direction for
     * all 3 facings. The ending
     * point is simply the point on the opposite corner to the first.
     * For example, if the 3 facings are UP, NORTH, and WEST, then the first is the point in the most DOWN, SOUTH, and
     * EAST direction.
     * The 3 facings must be all in distinct axis, that is, their .getAxis() must all be distinct.
     *
     * @param start   One corner of the cube.
     * @param end  Other corner of the cube.
     * @param facings 3 facings in the order of [ point, line, plane ]
     */
    public static Iterable<BetterBlockPos> allInBox(BetterBlockPos start, BetterBlockPos end, Direction... facings) {
        validateFacingsArray(facings);

        BetterBlockPos s = new BetterBlockPos();
        int[] length = new int[3];

        for(int i = 0; i < 3; i++) {
            int a = start.get(facings[i].getAxis());
            int b = end.get(facings[i].getAxis());
            int mult = facings[i].getAxisDirection().getStep();

            start.set(facings[i].getAxis(), Math.min(a * mult, b * mult) * mult);

            length[i] = Math.abs(a - b);
        }

        return new Iterable<>() {
            @Override
            public @NotNull Iterator<BetterBlockPos> iterator() {
                return new AbstractIterator<>() {
                    private final int[] offset = new int[] {-1, 0, 0};
                    private final BetterBlockPos result = start.copy();

                    @Override
                    protected @Nullable BetterBlockPos computeNext() {
                        offset[0]++;
                        if(offset[0] > length[0]) {
                            offset[0] = 0;
                            offset[1]++;
                        }

                        if(offset[1] > length[1]) {
                            offset[1] = 0;
                            offset[2]++;
                        }

                        if(offset[2] > length[2]) return endOfData();

                        return result.from(start).offset(facings[0], offset[0]).offset(facings[1], offset[1]).offset(facings[2], offset[2]);
                    }
                };
            }
        };
    }

    public static int getAxis(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

}
