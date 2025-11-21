package com.gregtechceu.gtceu.common.valueprovider;

import com.gregtechceu.gtceu.common.data.GTValueProviderTypes;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderType;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class CentralLimit extends IntProvider {

    public static final Codec<CentralLimit> CODEC = RecordCodecBuilder.create(
            (instance) -> instance.group(
                    Codec.INT.fieldOf("min_inclusive").forGetter((provider) -> provider.minInclusive),
                    Codec.INT.fieldOf("max_inclusive").forGetter((provider) -> provider.maxInclusive),
                    Codec.INT.fieldOf("parallel").forGetter((provider) -> provider.parallel))
                    .apply(instance, CentralLimit::new));
    private final int minInclusive;
    private final int maxInclusive;
    @Getter
    private final int parallel;

    private CentralLimit(int minInclusive, int maxInclusive, int parallel) {
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
        this.parallel = parallel;
    }

    public static CentralLimit of(int minInclusive, int maxInclusive, int parallel) {
        return new CentralLimit(minInclusive, maxInclusive, parallel);
    }

    public int sample(@NotNull RandomSource random) {
        // this is supposed to be Central Limit Theorem but mojang gaussian doesn't take params
        // double muTotal = parallel * 0.5;
        // double stddev = Math.sqrt(parallel / 12.0);
        double roll = random.nextGaussian() / 2.0;
        return (int) Math.round(roll * (maxInclusive - minInclusive + 1) + minInclusive);
    }

    public int getMinValue() {
        return this.minInclusive;
    }

    public int getMaxValue() {
        return this.maxInclusive;
    }

    public @NotNull IntProviderType<?> getType() {
        return GTValueProviderTypes.CENTRAL_LIMIT.get();
    }

    public String toString() {
        return "[" + this.minInclusive + "-" + this.maxInclusive + "] x " + this.parallel;
    }
}
