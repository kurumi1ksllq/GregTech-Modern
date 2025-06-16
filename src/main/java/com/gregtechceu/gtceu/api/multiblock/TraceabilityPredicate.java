package com.gregtechceu.gtceu.api.multiblock;

import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.pattern.CurrentBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TraceabilityPredicate {

    public static TraceabilityPredicate ANY = new TraceabilityPredicate(currentBlockInfo -> null);
    public static TraceabilityPredicate AIR = new TraceabilityPredicate(
            currentBlockInfo -> currentBlockInfo.getBlockState().isAir() ? null :
                    new PatternError(currentBlockInfo.getBlockPos(), Collections.emptyList()));

    public List<SimplePredicate> simple = new ArrayList<>();
    @Getter
    protected boolean isController;
    protected boolean hasAir = false;
    @Getter
    protected boolean isSingle = true;

    public TraceabilityPredicate() {}

    public TraceabilityPredicate(TraceabilityPredicate predicate) {
        simple.addAll(predicate.simple);
        isController = predicate.isController;
        hasAir = predicate.hasAir;
        isSingle = predicate.isSingle;
    }

    /**
     *
     * @param predicate  the testing function for if the current block information is valid
     * @param candidates the valid list of BlockInfos that this traceability predicate allows
     */
    public TraceabilityPredicate(Function<CurrentBlockInfo, PatternError> predicate,
                                 Function<Map<String, String>, BlockInfo[]> candidates) {
        simple.add(new SimplePredicate(predicate, candidates));
    }

    public TraceabilityPredicate(Function<CurrentBlockInfo, PatternError> predicate) {
        this(predicate, null);
    }

    public TraceabilityPredicate(SimplePredicate simplePredicate) {
        simple.add(simplePredicate);
    }

    /**
     * Mark it as the controller of this multi. Normally you won't call it yourself. Use plz.
     */
    public TraceabilityPredicate setController() {
        isController = true;
        return this;
    }

    public boolean hasAir() {
        return hasAir;
    }

    /**
     * Add tooltips for candidates. They are shown in JEI Pages.
     */
    public TraceabilityPredicate addTooltips(Component... tips) {
        if (tips.length > 0) {
            List<Component> tooltips = Arrays.stream(tips).toList();
            simple.forEach(predicate -> {
                if (predicate.candidates == null) return;
                if (predicate.toolTips == null) {
                    predicate.toolTips = new ArrayList<>();
                }
                predicate.toolTips.addAll(tooltips);
            });
        }
        return this;
    }

    public List<List<ItemStack>> getCandidates() {
        return simple.stream()
                .map(SimplePredicate::getCandidates)
                .collect(Collectors.toList());
    }

    /**
     * Set the minimum number of candidate blocks.
     */
    public TraceabilityPredicate setMinGlobalLimited(int min) {
        simple.forEach(p -> p.minCount = min);
        return this;
    }

    public TraceabilityPredicate setMinGlobalLimited(int min, int previewCount) {
        return this.setMinGlobalLimited(min).setPreviewCount(previewCount);
    }

    /**
     * Set the maximum number of candidate blocks.
     */
    public TraceabilityPredicate setMaxGlobalLimited(int max) {
        simple.forEach(p -> p.maxCount = max);
        return this;
    }

    public TraceabilityPredicate setMaxGlobalLimited(int max, int previewCount) {
        return this.setMaxGlobalLimited(max).setPreviewCount(previewCount);
    }

    /**
     * Set the minimum number of candidate blocks for each aisle layer.
     */
    public TraceabilityPredicate setMinLayerLimited(int min) {
        simple.forEach(p -> p.minLayerCount = min);
        return this;
    }

    public TraceabilityPredicate setMinLayerLimited(int min, int previewCount) {
        return this.setMinLayerLimited(min).setPreviewCount(previewCount);
    }

    /**
     * Set the maximum number of candidate blocks for each aisle layer.
     */
    public TraceabilityPredicate setMaxLayerLimited(int max) {
        simple.forEach(p -> p.maxLayerCount = max);
        return this;
    }

    public TraceabilityPredicate setMaxLayerLimited(int max, int previewCount) {
        return this.setMaxLayerLimited(max).setPreviewCount(previewCount);
    }

    /**
     * Sets the Minimum and Maximum limit to the passed value
     * 
     * @param limit The Maximum and Minimum limit
     */
    public TraceabilityPredicate setExactLimit(int limit) {
        return this.setMinGlobalLimited(limit).setMaxGlobalLimited(limit);
    }

    /**
     * Set the number of it appears in JEI pages. It only affects JEI preview. (The specific number)
     */
    public TraceabilityPredicate setPreviewCount(int count) {
        simple.forEach(p -> p.previewCount = count);
        return this;
    }

    /**
     * Set renderMask.
     */
    public TraceabilityPredicate disableRenderFormed() {
        simple.forEach(p -> p.disableRenderFormed = true);
        return this;
    }

    public TraceabilityPredicate setNBTParser(String nbtParser) {
        simple.forEach(predicate -> predicate.nbtParser = nbtParser);
        return this;
    }

    public PatternError test(CurrentBlockInfo currBlock, Object2IntMap<SimplePredicate> globalCache,
                             Object2IntMap<SimplePredicate> layerCache) {
        PatternError lastError = null;
        for (SimplePredicate p : simple) {
            PatternError error = p.testLimited(currBlock, globalCache, layerCache);
            if (error == null) return null;
            lastError = error;
        }
        return lastError == PatternError.PLACEHOLDER ? new PatternError(currBlock.getBlockPos(), getCandidates()) :
                lastError;
    }

    public TraceabilityPredicate or(TraceabilityPredicate other) {
        if (other != null) {
            TraceabilityPredicate newPredicate = new TraceabilityPredicate(this);
            newPredicate.hasAir = newPredicate.hasAir || this == AIR || other == AIR;
            newPredicate.simple.addAll(other.simple);
            return newPredicate;
        }
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof TraceabilityPredicate pred)) return false;

        return this.hasAir == pred.hasAir &&
                this.isController == pred.isController &&
                this.simple.equals(pred.simple);
    }
}
