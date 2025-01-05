package com.gregtechceu.gtceu.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import lombok.Getter;

import java.util.Objects;

public class FacingPos {

    public static final FacingPos ZERO = new FacingPos(BlockPos.ZERO, null);

    @Getter
    private final BlockPos pos;
    @Getter
    private final Direction direction;
    private final int hashCode;

    public FacingPos(BlockPos pos, Direction direction) {
        this.pos = pos;
        this.direction = direction;
        this.hashCode = Objects.hash(pos, direction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FacingPos facingPos = (FacingPos) o;
        return pos.equals(facingPos.pos) && direction == facingPos.getDirection();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
