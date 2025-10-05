package com.gregtechceu.gtceu.api.item.module;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.resources.ResourceLocation;

import lombok.Getter;

import java.util.function.BiFunction;

public abstract class TieredItemModule extends ItemModule implements ITieredItemModule {

    @Getter
    private final int tier;

    public TieredItemModule(ResourceLocation id, int tier) {
        super(id);
        this.tier = tier;
    }

    public static TieredItemModule[] create(ResourceLocation id, int minTier, int maxTier,
                                            BiFunction<ResourceLocation, Integer, TieredItemModule> constructor) {
        TieredItemModule[] result = new TieredItemModule[maxTier - minTier + 1];
        for (int i = 0; i <= maxTier - minTier; i++) {
            ResourceLocation resourceLocation = id.withSuffix("_" + (i + minTier));
            result[i] = constructor.apply(resourceLocation, i + minTier);
        }
        return result;
    }

    public static TieredItemModule[] create(ResourceLocation id,
                                            BiFunction<ResourceLocation, Integer, TieredItemModule> constructor) {
        return create(id, GTValues.LV, GTValues.MAX, constructor);
    }
}
