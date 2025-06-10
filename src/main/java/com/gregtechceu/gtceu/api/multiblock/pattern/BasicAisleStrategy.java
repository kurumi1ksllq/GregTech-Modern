package com.gregtechceu.gtceu.api.multiblock.pattern;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.utils.GTStringUtils;

import net.minecraft.util.Mth;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BasicAisleStrategy extends AisleStrategy {

    protected final List<int[]> multiAisles = new ArrayList<>();
    protected final List<PatternAisle> aisles = new ArrayList<>();
    protected final int[] result = new int[2];

    @Override
    public boolean check(PatternState state, boolean flip) {
        int offset = 0;
        for (int[] multiAisle : multiAisles) {
            int result = checkMultiAisle(state, multiAisle, offset, flip);
            if (result == -1) return false;
            offset += result;
        }
        return true;
    }

    public int getMultiAisleRepeats(int index) {
        return multiAisles.get(index)[4];
    }

    protected int checkMultiAisle(PatternState state, int[] multiAisle, int offset, boolean flip) {
        int aisleOffset = 0;
        int temp = 0;
        for (int i = 1; i <= multiAisle[1]; i++) {
            for (int j = multiAisle[2]; j < multiAisle[3]; j++) {
                int res = checkRepeatAisle(state, j, offset + temp, flip);
                if (res == -1) {
                    if (i <= multiAisle[0]) return -1;
                    multiAisle[4] = i - 1;
                    return aisleOffset;
                }
                temp += res;
            }
            aisleOffset += temp;
        }

        multiAisle[4] = multiAisle[1];
        return aisleOffset;
    }

    protected int checkRepeatAisle(PatternState state, int index, int offset, boolean flip) {
        PatternAisle aisle = aisles.get(index);
        for (int i = 1; i <= aisle.maxRepeats; i++) {
            boolean res = checkAisle(state, index, offset + i - 1, flip);
            if (!res) {
                if (i <= aisle.minRepeats) return -1;

                return aisles.get(index).actualRepeats = i - 1;
            }
        }
        return aisles.get(index).actualRepeats = aisle.maxRepeats;
    }

    @Override
    public int @NotNull [] getDefaultAisles(@Nullable Map<String, String> map) {
        IntList list = new IntArrayList();
        for (int i = 0; i < multiAisles.size(); i++) {
            int[] multi = multiAisles.get(i);
            int multiRepeats = 0;
            if(map == null) {
                multiRepeats = multi[0];
            } else {
                multiRepeats = Mth.clamp(GTStringUtils.parseInt(map.get("multi." + 1)), multi[0], multi[1]);
            }
            for (int j = 0; j < multiRepeats; j++) {
                for (int k = multi[2]; k < multi[3]; k++) {
                    int aisleRepeats = 0;
                    if(map == null) {
                        aisleRepeats = aisles.get(k).minRepeats;
                    } else {
                        aisleRepeats = Mth.clamp(GTStringUtils.parseInt(map.get("multi." + i + "." + (k - multi[2]))),
                                aisles.get(k).minRepeats, aisles.get(k).maxRepeats);
                    }
                    for (int l = 0; l < aisleRepeats; l++) {
                        list.add(k);
                    }
                }
            }
        }
        return list.toIntArray();
    }

    @Override
    protected void finish(int[] dimensions, RelativeDirection[] directions, List<PatternAisle> aisles) {
        super.finish(dimensions, directions, aisles);

        this.aisles.addAll(aisles);

        BitSet covered = new BitSet(aisles.size());
        int sum = 0;
        for (int[] arr : multiAisles) {
            covered.set(arr[2], arr[3]);
            sum += arr[3] - arr[2];
        }

        if (sum != covered.cardinality()) {
            GTCEu.LOGGER.error("Overlapping multi-aisles. " +
                    "Total of {} aisles in the multiAisles but only {} distinct aisles.", sum, covered.cardinality());
            multiAisleError();
        }
        if (sum > aisles.size()) {
            GTCEu.LOGGER.error("multiAisles out of bounds. total of {} aisles but {} aisles in multiAisles",
                    aisles.size(), sum);
            multiAisleError();
        }

        int i = covered.nextClearBit(0);
        while ((i = covered.nextClearBit(i)) < aisles.size()) {
            multiAisles.add(new int[] { 1, 1, i, i + 1, -1 });
            covered.set(i);
        }

        multiAisles.sort(Comparator.comparingInt(a -> a[2]));
    }

    protected void multiAisleError() {
        GTCEu.LOGGER.error(
                "multiAisles in the pattern, formatted as [minRepeats, maxRepeats, startInclusive, endExclusive, actualRepeats]");
        for (var arr : multiAisles) {
            GTCEu.LOGGER.error(Arrays.toString(arr));
        }
        throw new IllegalStateException("Illegal multiAisles, see log above.");
    }

    public BasicAisleStrategy multiAisle(int min, int max, int from, int to) {
        Preconditions.checkArgument(max >= min, "max: %s is less than min: %s", max, min);
        Preconditions.checkArgument(from >= 0, "from argument is negative: %s", from);
        Preconditions.checkArgument(to > 0, "to argument is not positive: %s", to);
        multiAisles.add(new int[] { min, max, from, to, -1 });
        return this;
    }
}
