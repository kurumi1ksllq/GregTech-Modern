package com.gregtechceu.gtceu.integration.kjs.recipe.components;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.content.Content;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@NoArgsConstructor
public class CapabilityMap extends Reference2ObjectLinkedOpenHashMap<RecipeCapability<?>, List<Content>> {

    public static final Codec<CapabilityMap> CODEC = RecipeCapability.CODEC
            .xmap(CapabilityMap::new, Function.identity());

    public CapabilityMap(Map<RecipeCapability<?>, List<Content>> m) {
        super(m);
    }

    public void add(RecipeCapability<?> capability, Content value) {
        this.computeIfAbsent(capability, cap -> new ArrayList<>()).add(value);
    }
}
