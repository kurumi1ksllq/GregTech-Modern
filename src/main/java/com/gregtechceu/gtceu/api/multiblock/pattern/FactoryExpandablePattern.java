package com.gregtechceu.gtceu.api.multiblock.pattern;

import com.gregtechceu.gtceu.api.multiblock.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.utils.QuadFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.function.BiFunction;

public class FactoryExpandablePattern {
    protected QuadFunction<Level, BlockPos.MutableBlockPos, Direction, Direction, int[]> boundsFunc;
    protected BiFunction<BlockPos.MutableBlockPos, int[], TraceabilityPredicate> predicateFunc;
    protected final RelativeDirection[] directions = new RelativeDirection[3];

    private FactoryExpandablePattern(RelativeDirection aisleDir, RelativeDirection stringDir, RelativeDirection charDir) {
        directions[0] = aisleDir;
        directions[1] = stringDir;
        directions[2] = charDir;
        RelativeDirection.validateFacingsArray(directions);
    }

    public static FactoryExpandablePattern start(RelativeDirection aisleDir, RelativeDirection stringDir, RelativeDirection charDir) {
        return new FactoryExpandablePattern(aisleDir, stringDir, charDir);
    }

    public static FactoryExpandablePattern start() {
        return new FactoryExpandablePattern(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT);
    }

    public FactoryExpandablePattern boundsFunction(QuadFunction<Level, BlockPos.MutableBlockPos, Direction, Direction, int[]> func) {
        this.boundsFunc = func;
        return this;
    }

    public FactoryExpandablePattern predicateFunction(BiFunction<BlockPos.MutableBlockPos, int[], TraceabilityPredicate> func) {
        this.predicateFunc = func;
        return this;
    }

    public ExpandablePattern build() {
        if(boundsFunc == null)
            throw new IllegalStateException("Bound function is null, use .boundsFunction(...) on the builder");
        if(predicateFunc == null)
            throw new IllegalStateException("Predicate function is null, use .predicateFunction(...) on the builder");

        return new ExpandablePattern(boundsFunc, predicateFunc, directions);
    }
}
