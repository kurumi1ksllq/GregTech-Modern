package com.gregtechceu.gtceu.api.data.chemical.material.registry;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.registry.GTRegistry;

import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.unification.material.MaterialRegistryManager;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class MaterialRegistry extends GTRegistry.String<Material> {

    private static int networkIdCounter;
    @Getter
    private final int networkId = networkIdCounter++;
    private final java.lang.String modid;

    private boolean isRegistryClosed = false;
    @NotNull
    private Material fallbackMaterial = GTMaterials.NULL;

    public MaterialRegistry(java.lang.String modId) {
        super(new ResourceLocation(modId, "material"));
        this.modid = modId;
    }

    public void register(Material material) {
        this.register(material.getName(), material);
    }

    @Override
    public <T extends Material> T register(@NotNull java.lang.String key, @NotNull T value) {
        if (isRegistryClosed) {
            GTCEu.LOGGER.error(
                    "Materials cannot be registered in the PostMaterialEvent (or after)! Must be added in the MaterialEvent. Skipping material {}...",
                    key);
            return null;
        }
        super.register(key, value);
        return value;
    }

    @NotNull
    public Collection<Material> getAllMaterials() {
        return values();
    }

    public void setFallbackMaterial(@NotNull Material material) {
        this.fallbackMaterial = material;
    }

    @NotNull
    public Material getFallbackMaterial() {
        if (this.fallbackMaterial.isNull()) {
            this.fallbackMaterial = MaterialRegistryManager.getInstance().getDefaultFallback();
        }
        return this.fallbackMaterial;
    }

    @NotNull
    public java.lang.String getModid() {
        return modid;
    }

    public void closeRegistry() {
        this.isRegistryClosed = true;
    }
}
