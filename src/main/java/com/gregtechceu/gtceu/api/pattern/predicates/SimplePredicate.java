package com.gregtechceu.gtceu.api.pattern.predicates;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.gregtechceu.gtceu.api.pattern.error.SinglePredicateError;
import com.gregtechceu.gtceu.api.pattern.pattern.CurrentBlockInfo;
import com.gregtechceu.gtceu.api.pattern.util.BlockInfo;
import com.gregtechceu.gtceu.data.lang.LangHandler;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SimplePredicate {


    @Nullable
    public Function<Map<String, String>, BlockInfo[]> candidates;
    public Function<CurrentBlockInfo, PatternError> predicate;
    public List<Component> toolTips;
    public int minCount = -1;
    public int maxCount = -1;
    public int minLayerCount = -1;
    public int maxLayerCount = -1;
    public int previewCount = -1;
    public boolean disableRenderFormed = false;
    public IO io = IO.BOTH;
    public String slotName;
    public String nbtParser;

    public String type;

    public SimplePredicate() {
        this.type = "Unknown";
    }

    public SimplePredicate(Function<CurrentBlockInfo, PatternError> predicate, @Nullable Function<Map<String, String>, BlockInfo[]> candidates) {
        this.predicate = predicate;
        this.candidates = candidates;
        this.type = "Unknown";
    }

    public SimplePredicate(String type, Function<CurrentBlockInfo, PatternError> predicate, @Nullable Function<Map<String, String>, BlockInfo[]> candidates) {
        this.predicate = predicate;
        this.candidates = candidates;
        this.type = type;
    }

    public SimplePredicate buildPredicate() {
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    public List<Component> getToolTips(TraceabilityPredicate predicates) {
        List<Component> result = new ArrayList<>();
        if (toolTips != null) {
            result.addAll(toolTips);
        }
        if (minCount == maxCount && maxCount != -1) {
            result.add(Component.translatable("gtceu.multiblock.pattern.error.limited_exact", minCount));
        } else if (minCount != maxCount && minCount != -1 && maxCount != -1) {
            result.add(Component.translatable("gtceu.multiblock.pattern.error.limited_within", minCount, maxCount));
        } else {
            if (minCount != -1) {
                result.add(LangHandler.getFromMultiLang("gtceu.multiblock.pattern.error.limited", 1, minCount));
            }
            if (maxCount != -1) {
                result.add(LangHandler.getFromMultiLang("gtceu.multiblock.pattern.error.limited", 0, maxCount));
            }
        }
        if (predicates == null) return result;
        if (predicates.isSingle()) {
            result.add(Component.translatable("gtceu.multiblock.pattern.single"));
        }
        if (predicates.hasAir()) {
            result.add(Component.translatable("gtceu.multiblock.pattern.replaceable_air"));
        }
        return result;
    }

    public PatternError testRaw(CurrentBlockInfo currBlock) {
        return predicate.apply(currBlock);
    }

    public PatternError testLimited(CurrentBlockInfo currBlock,
                                    Object2IntMap<SimplePredicate> globalCache, Object2IntMap<SimplePredicate> layerCache) {
        PatternError error = testGlobal(currBlock, globalCache, layerCache);
        if(error != null) return error;
        return testLayer(currBlock, layerCache);
    }

    /*private boolean checkInnerConditions(MultiblockState blockWorldState) {
        if (disableRenderFormed) {
            blockWorldState.getMatchContext().getOrCreate("renderMask", LongOpenHashSet::new)
                    .add(blockWorldState.getPos().asLong());
        }
        if (io != IO.BOTH) {
            if (blockWorldState.io == IO.BOTH) {
                blockWorldState.io = io;
            } else if (blockWorldState.io != io) {
                blockWorldState.io = null;
            }
        }
        if (nbtParser != null && !blockWorldState.world.isClientSide) {
            BlockEntity te = blockWorldState.getTileEntity();
            if (te != null) {
                CompoundTag nbt = te.saveWithFullMetadata();
                if (Pattern.compile(nbtParser).matcher(nbt.toString()).find()) {
                    return true;
                }
            }
            blockWorldState.setError(new PatternStringError("The NBT fails to match"));
            return false;
        }
        if (slotName != null) {
            Map<Long, Set<String>> slots = blockWorldState.getMatchContext().getOrCreate("slots",
                    Long2ObjectArrayMap::new);
            slots.computeIfAbsent(blockWorldState.getPos().asLong(), s -> new HashSet<>()).add(slotName);
            return true;
        }
        return true;
    }*/

    public PatternError testGlobal(CurrentBlockInfo currBlock,
                                   Object2IntMap<SimplePredicate> globalCache, Object2IntMap<SimplePredicate> layerCache) {
        PatternError res = predicate.apply(currBlock);
        if(!globalCache.containsKey(this)) globalCache.put(this, 0);
        if((minCount == -1 && maxCount == -1) || res != null || layerCache == null) return res;
        int count = layerCache.put(this, layerCache.getInt(this) + 1) + 1 + globalCache.getInt(this);
        if(maxCount == -1 || count <= maxCount) return null;
        return new SinglePredicateError(this, 0);
    }

    public PatternError testLayer(CurrentBlockInfo currBlock, Object2IntMap<SimplePredicate> layerCache) {
        PatternError res = predicate.apply(currBlock);
        if((minLayerCount == -1 && maxLayerCount == -1) || res != null) return res;
        if(maxLayerCount == -1 || layerCache.getInt(this) <= maxLayerCount) return null;
        return new SinglePredicateError(this, 2);
    }

    public List<ItemStack> getCandidates() {
        if (GTCEu.isClientSide()) {
            return candidates == null ? Collections.emptyList() :
                    Arrays.stream(this.candidates.apply(Collections.emptyMap()))
                            .filter(info -> info.getBlockState().getBlock() != Blocks.AIR)
                            .map(blockInfo -> blockInfo.getItemStackForm(Minecraft.getInstance().level, BlockPos.ZERO))
                            .collect(Collectors.toList());
        }
        return candidates == null ? Collections.emptyList() :
                Arrays.stream(this.candidates.apply(Collections.emptyMap()))
                        .filter(info -> info.getBlockState().getBlock() != Blocks.AIR)
                        .map(BlockInfo::getItemStackForm)
                        .collect(Collectors.toList());
    }
}
