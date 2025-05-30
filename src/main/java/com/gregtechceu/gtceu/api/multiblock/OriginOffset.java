package com.gregtechceu.gtceu.api.multiblock;

import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class OriginOffset {

    protected final int[] offset = new int[3];

    public OriginOffset move(int xi, int yi, int zi) {
        return move(RelativeDirection.LEFT, RelativeDirection.UP, RelativeDirection.FRONT, xi, yi, zi);
    }

    public OriginOffset move(RelativeDirection x, RelativeDirection y, RelativeDirection z, int xi, int yi, int zi) {
        RelativeDirection.validateFacingsArray(new RelativeDirection[] { x, y, z });
        return move(x, xi).move(y, yi).move(z, zi);
    }

    public OriginOffset move(RelativeDirection dir, int amount) {
        amount *= dir.ordinal() % 2 == 0 ? 1 : -1;
        offset[dir.ordinal() / 2] += amount;
        return this;
    }

    public OriginOffset move(RelativeDirection dir) {
        return move(dir, 1);
    }

    public int get(RelativeDirection dir) {
        return offset[dir.ordinal() / 2] * ((dir.ordinal() % 2 == 0) ? 1 : -1);
    }

    public void apply(BlockPos.MutableBlockPos pos, Direction front, Direction up, boolean flip) {
        for (int i = 0; i < 3; i++) {
            pos.move(RelativeDirection.values()[2 * i].getRelativeFacing(front, up, flip), offset[i]);
        }
    }
}
