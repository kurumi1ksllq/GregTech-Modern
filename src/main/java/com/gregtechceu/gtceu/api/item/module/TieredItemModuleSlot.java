package com.gregtechceu.gtceu.api.item.module;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.resources.ResourceLocation;

import lombok.Getter;

import java.util.function.BiFunction;

public class TieredItemModuleSlot extends ItemModuleSlot {

    @Getter
    private final int tier;

    public TieredItemModuleSlot(ResourceLocation id, int tier) {
        super(id);
        this.tier = tier;
    }

    @Override
    public boolean acceptsModule(ItemModule module) {
        return !(module instanceof ITieredItemModule tieredModule) || tieredModule.getTier() >= getTier();
    }

    public static TieredItemModuleSlot[] create(ResourceLocation id, int minTier, int maxTier,
                                                BiFunction<ResourceLocation, Integer, TieredItemModuleSlot> constructor) {
        TieredItemModuleSlot[] result = new TieredItemModuleSlot[maxTier - minTier + 1];
        for (int i = 0; i <= maxTier - minTier; i++) {
            ResourceLocation resourceLocation = id.withSuffix("_" + (i + minTier));
            result[i] = constructor.apply(resourceLocation, i + minTier);
        }
        return result;
    }

    public static TieredItemModuleSlot[] create(ResourceLocation id,
                                                BiFunction<ResourceLocation, Integer, TieredItemModuleSlot> constructor) {
        return create(id, GTValues.LV, GTValues.MAX, constructor);
    }
}
