package com.gregtechceu.gtceu.api.item.module;

import net.minecraft.resources.ResourceLocation;

import lombok.Getter;

public class TieredItemModule extends ItemModule {

    @Getter
    private final int tier;

    public TieredItemModule(ResourceLocation id, int tier) {
        super(id);
        this.tier = tier;
    }

    public static TieredItemModule[] createForTiersBetween(ResourceLocation id, int minTier, int maxTier) {
        TieredItemModule[] result = new TieredItemModule[maxTier - minTier + 1];
        for (int i = 0; i <= maxTier - minTier; i++)
            result[i] = new TieredItemModule(id.withSuffix("_" + (i + minTier)), i + minTier);
        return result;
    }
}
