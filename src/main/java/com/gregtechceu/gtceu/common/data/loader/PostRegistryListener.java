package com.gregtechceu.gtceu.common.data.loader;

import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.worldgen.OreVeinDefinition;
import com.gregtechceu.gtceu.api.worldgen.WorldGeneratorUtils;
import com.gregtechceu.gtceu.api.worldgen.generator.indicators.SurfaceIndicatorGenerator;
import com.gregtechceu.gtceu.data.worldgen.GTOreVeins;
import com.gregtechceu.gtceu.integration.map.cache.server.ServerCache;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@NotNullByDefault
public class PostRegistryListener extends ContextAwareReloadListener implements PreparableReloadListener {

    public static final PostRegistryListener INSTANCE = new PostRegistryListener();

    private PostRegistryListener() {}

    protected void apply() {
        // Apply and validate stuff that requires registry access that isn't finished yet during the Registry event
        var biomeLookup = GTRegistries.builtinRegistry().lookupOrThrow(Registries.BIOME);

        var oreVeinRegistry = GTRegistries.builtinRegistry().registryOrThrow(GTRegistries.ORE_VEIN_REGISTRY);
        oreVeinRegistry.holders().forEach(holder -> {
            var definition = holder.value();
            definition.biomeLookup(biomeLookup);
            definition.indicatorGenerators().stream()
                    .filter(SurfaceIndicatorGenerator.class::isInstance)
                    .map(SurfaceIndicatorGenerator.class::cast)
                    .forEach(SurfaceIndicatorGenerator::validateAfterBlocks);
        });

        buildVeinGenerators(oreVeinRegistry);
        GTOreVeins.updateLargestVeinSize(oreVeinRegistry);
        ServerCache.instance.oreVeinDefinitionsChanged(oreVeinRegistry);
        WorldGeneratorUtils.invalidateOreVeinCache();
    }

    public static void buildVeinGenerators(Registry<OreVeinDefinition> registry) {
        var iterator = registry.holders().iterator();
        while (iterator.hasNext()) {
            var definition = iterator.next();
            var veinGen = definition.value().veinGenerator();
            if (veinGen != null && definition.value().canGenerate()) {
                veinGen.build();
            }
        }
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return stage.wait(null).thenRunAsync(this::apply);
    }
}
