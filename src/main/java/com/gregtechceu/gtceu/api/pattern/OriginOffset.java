package com.gregtechceu.gtceu.api.pattern;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.core.Direction;

public class OriginOffset {
    protected final int[] offset = new int[3];

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

    public void apply(BetterBlockPos pos, Direction front, Direction up, boolean flip) {
        for(int i = 0; i < 3; i++) {
            pos.offset(RelativeDirection.values()[2 * i].getRelativeFacing(front, up, flip), offset[i]);
        }
    }
}
