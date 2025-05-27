package com.gregtechceu.gtceu.api.multiblock.pattern;

import com.google.common.base.Joiner;
import com.gregtechceu.gtceu.api.multiblock.BetterBlockPos;
import com.gregtechceu.gtceu.api.multiblock.OriginOffset;
import com.gregtechceu.gtceu.api.multiblock.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A builder class for {@link BlockPattern}<br />
 * When the multiblock is placed, its facings are concrete. Then, the {@link RelativeDirection}s passed into
 * {@link FactoryBlockPattern#start(RelativeDirection, RelativeDirection, RelativeDirection)} are ways in which the
 * pattern progresses. It can be thought like this, where startPos() is either defined via
 * {@link FactoryBlockPattern#startOffset(OriginOffset)}
 * , or automatically detected(for legacy compat only, you should use
 * {@link FactoryBlockPattern#startOffset(OriginOffset)} always for new code):
 *
 * <pre>
 * {@code
 * for(int aisleI in 0..aisles):
 *     for(int stringI in 0..strings):
 *         for(int charI in 0..chars):
 *             pos = startPos()
 *             pos.move(aisleI in aisleDir)
 *             pos.move(stringI in stringDir)
 *             pos.move(charI in charDir)
 *             predicate = aisles[aisleI].stringAt(stringI).charAt(charI)
 * }
 * </pre>
 */
public class FactoryBlockPattern {
    protected static final Joiner COMMA_JOIN = Joiner.on(",");

    private final int[] dimensions = {-1, -1, -1};

    private OriginOffset offset;
    private char centerChar;
    private AisleStrategy aisleStrategy;

    private final List<PatternAisle> aisles = new ArrayList<>();

    private final Char2ObjectMap<TraceabilityPredicate> symbolMap = new Char2ObjectOpenHashMap<>();

    private final RelativeDirection[] directions = new RelativeDirection[3];

    private FactoryBlockPattern(RelativeDirection aisleDir, RelativeDirection stringDir, RelativeDirection charDir) {
        directions[0] = aisleDir;
        directions[1] = stringDir;
        directions[2] = charDir;
        BetterBlockPos.validateFacingsArray(directions);
        this.symbolMap.put(' ', TraceabilityPredicate.ANY);
    }

    public FactoryBlockPattern aisleRepeatable(int minRepeats, int maxRepeats, @NotNull String... aisle) {
        validateAisle(aisle);
        for(String s : aisle) {
            for (char c : s.toCharArray()) {
                if(!this.symbolMap.containsKey(c)) {
                    this.symbolMap.put(c, null);
                }
            }
        }

        if(minRepeats > maxRepeats) {
            throw new IllegalArgumentException("minRepeats must be smaller than maxRepeats");
        }
        // PA returns in gtm :lets:
        PatternAisle pa = new PatternAisle(aisle);
        pa.minRepeats = minRepeats;
        pa.maxRepeats = maxRepeats;
        aisles.add(pa);
        return this;
    }

    public FactoryBlockPattern aisle(String... aisle) {
        return aisleRepeatable(1, 1, aisle);
    }

    public FactoryBlockPattern startOffset(OriginOffset offset) {
        this.offset = offset;
        return this;
    }

    /**
     * Starts the builder, this is equivlent to calling
     * {@link FactoryBlockPattern#start(RelativeDirection, RelativeDirection, RelativeDirection)} with RIGHT, UP, BACK
     *
     * @see FactoryBlockPattern#start(RelativeDirection, RelativeDirection, RelativeDirection)
     */
    public static FactoryBlockPattern start() {
        return new FactoryBlockPattern(RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT);
    }

    /**
     * Starts the builder, each pair of {@link RelativeDirection} must be used at exactly once!
     *
     * @param charDir   The direction chars progress in, each successive char in a string progresses by this direction
     * @param stringDir The direction strings progress in, each successive string in an aisle progresses by this
     *                  direction
     * @param aisleDir  The direction aisles progress in, each successive {@link FactoryBlockPattern#aisle(String...)}
     *                  progresses in this direction
     */
    public static FactoryBlockPattern start(RelativeDirection aisleDir, RelativeDirection stringDir,
                                            RelativeDirection charDir) {
        return new FactoryBlockPattern(aisleDir, stringDir, charDir);
    }

    public FactoryBlockPattern where(char symbol, TraceabilityPredicate predicate) {
        this.symbolMap.put(symbol, predicate);
        if(predicate.isController()) centerChar = symbol;
        return this;
    }

    public FactoryBlockPattern aisleStrategy(AisleStrategy aisleStrategy) {
        this.aisleStrategy = aisleStrategy;
        return this;
    }

    public BlockPattern build() {
        checkMissingPredicates();
        this.dimensions[0] = aisles.size();
        if(aisleStrategy == null) aisleStrategy = new BasicAisleStrategy();

        aisleStrategy.finish(dimensions, directions, aisles);
        return new BlockPattern(aisles.toArray(new PatternAisle[0]), aisleStrategy, dimensions,
                directions, offset, symbolMap, centerChar);
    }

    private void checkMissingPredicates() {
        List<Character> list = new ArrayList<>();

        for (var entry : this.symbolMap.char2ObjectEntrySet()) {
            if(entry.getValue() == null) {
                list.add(entry.getCharKey());
            }
        }

        if(!list.isEmpty()) {
            throw new IllegalStateException("Predicates for character(s) " + COMMA_JOIN.join(list) + " are missing");
        }
    }

    public void validateAisle(String[] aisle) {
        if(ArrayUtils.isEmpty(aisle) || StringUtils.isEmpty(aisle[0]))
            throw new IllegalArgumentException("Empty pattern for aisle");

        if(dimensions[2] == -1) {
            dimensions[2] = aisle[0].length();
        }

        if(dimensions[1] == -1) {
            dimensions[1] = aisle.length;
        }

        if(aisle.length != dimensions[1]) {
            throw new IllegalArgumentException("Expected aisle with height of " + dimensions[1] +
                    ", but was given one with a height of " + aisle.length);
        } else {
            for(String s : aisle) {
                if(s.length() != dimensions[2]) {
                    throw new IllegalArgumentException("" +
                            "Not all rows in the given aisle are the correct width (expected " + dimensions[2] +
                            ", found one with " + s.length() + ")");
                }
            }
        }
    }
}
