package com.gregtechceu.gtceu.common.unification.material;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.IMaterialRegistryManager;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.registry.MaterialRegistry;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.resources.ResourceLocation;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class MaterialRegistryManager implements IMaterialRegistryManager {

    private static MaterialRegistryManager INSTANCE;

    private final Object2ObjectMap<String, MaterialRegistry> registries = new Object2ObjectOpenHashMap<>();
    private final Int2ObjectMap<MaterialRegistry> networkIds = new Int2ObjectOpenHashMap<>();

    @Nullable
    private Collection<Material> registeredMaterials;

    private final MaterialRegistry gregtechRegistry = createInternalRegistry();

    private Phase registrationPhase = Phase.PRE;

    private MaterialRegistryManager() {}

    public static MaterialRegistryManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MaterialRegistryManager();
        }
        return INSTANCE;
    }

    @NotNull
    @Override
    public MaterialRegistry createRegistry(@NotNull String modid) {
        if (getPhase() != Phase.PRE) {
            throw new IllegalStateException("Cannot create registries in phase " + getPhase());
        }

        Preconditions.checkArgument(!registries.containsKey(modid),
                "Material registry already exists for modid %s", modid);
        MaterialRegistry registry = new MaterialRegistry(modid);
        registries.put(modid, registry);
        networkIds.put(registry.getNetworkId(), registry);
        return registry;
    }

    @NotNull
    @Override
    public MaterialRegistry getRegistry(@NotNull String modid) {
        MaterialRegistry registry = registries.get(modid);
        return registry != null ? registry : gregtechRegistry;
    }

    @NotNull
    @Override
    public MaterialRegistry getRegistry(int networkId) {
        MaterialRegistry registry = networkIds.get(networkId);
        return registry != null ? registry : gregtechRegistry;
    }

    @NotNull
    @Override
    public Collection<MaterialRegistry> getRegistries() {
        if (getPhase() == Phase.PRE) {
            throw new IllegalStateException("Cannot get all material registries during phase " + getPhase());
        }
        return Collections.unmodifiableCollection(registries.values());
    }

    @NotNull
    @Override
    public Collection<Material> getRegisteredMaterials() {
        if (registeredMaterials == null ||
                (getPhase() != Phase.CLOSED && getPhase() != Phase.FROZEN)) {
            throw new IllegalStateException("Cannot retrieve all materials before registration");
        }
        return registeredMaterials;
    }

    @Override
    public Material getMaterial(@NotNull String name) {
        if (!name.isEmpty()) {
            if (name.contains(":")) {
                ResourceLocation resLoc = ResourceLocation.tryParse(name);
                if (resLoc == null) return GTMaterials.NULL;
                return getRegistry(resLoc.getNamespace()).get(resLoc);
            } else {
                return getRegistry(GTCEu.MOD_ID).get(GTCEu.id(name));
            }
        }
        return GTMaterials.NULL;
    }

    @Override
    public Material getMaterial(ResourceLocation resourceLocation) {
        return getRegistry(resourceLocation.getNamespace()).get(resourceLocation);
    }

    @Override
    public ResourceLocation getKey(Material material) {
        return material.getResourceLocation();
    }

    @NotNull
    @Override
    public Phase getPhase() {
        return registrationPhase;
    }

    public void unfreezeRegistries() {
        registries.values().forEach(MaterialRegistry::unfreeze);
        registrationPhase = Phase.OPEN;
    }

    public void closeRegistries() {
        registries.values().forEach(MaterialRegistry::closeRegistry);
        Collection<Material> collection = new ArrayList<>();
        for (MaterialRegistry registry : registries.values()) {
            collection.addAll(registry.getAllMaterials());
        }
        registeredMaterials = Collections.unmodifiableCollection(collection);
        registrationPhase = Phase.CLOSED;
    }

    public void freezeRegistries() {
        registries.values().forEach(MaterialRegistry::freeze);
        registrationPhase = Phase.FROZEN;
    }

    @NotNull
    private MaterialRegistry createInternalRegistry() {
        MaterialRegistry registry = new MaterialRegistry(GTCEu.MOD_ID);
        this.registries.put(GTCEu.MOD_ID, registry);
        return registry;
    }

    @NotNull
    public Material getDefaultFallback() {
        return gregtechRegistry.getFallbackMaterial();
    }
}
