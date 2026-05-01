package com.gregtechceu.gtceu.api.data.chemical.material.registry;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.registry.GTRegistry;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.resources.ResourceLocation;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MaterialRegistry extends GTRegistry.RL<Material> {

    @Getter
    private final Set<java.lang.String> usedNamespaces = new ObjectOpenHashSet<>();

    private Phase registrationPhase = Phase.PRE;

    @NotNull
    private Map<java.lang.String, Material> fallbackMaterial = new Object2ObjectOpenHashMap<>();

    public MaterialRegistry() {
        super(GTCEu.id("material"));
    }

    public Material get(java.lang.String name) {
        if (!name.isEmpty()) {
            if (name.contains(":")) {
                ResourceLocation resLoc = ResourceLocation.tryParse(name);
                if (resLoc == null) return GTMaterials.NULL;
                return get(resLoc);
            } else {
                return get(GTCEu.id(name));
            }
        }
        return GTMaterials.NULL;
    }

    public void register(Material material) {
        this.register(material.getResourceLocation(), material);
    }

    @Override
    public <T extends Material> T register(@NotNull ResourceLocation key, @NotNull T value) {
        if (registrationPhase == Phase.CLOSED || registrationPhase == Phase.FROZEN) {
            GTCEu.LOGGER.error(
                    "Materials cannot be registered in the PostMaterialEvent (or after)! Must be added in the MaterialEvent. Skipping material {}...",
                    key);
            return null;
        }
        super.register(key, value);
        usedNamespaces.add(key.getNamespace());
        return value;
    }

    /**
     * Accessible when in phases:
     * <ul>
     * <li>{@link Phase#CLOSED}</li>
     * <li>{@link Phase#FROZEN}</li>
     * </ul>
     *
     * @return all registered materials.
     */
    @NotNull
    public Collection<Material> getAllMaterials() {
        if (registrationPhase == Phase.PRE || registrationPhase == Phase.OPEN)
            throw new IllegalStateException("Cannot retrieve all materials before registration");
        return values();
    }

    public void setFallbackMaterial(java.lang.String modId, @NotNull Material material) {
        this.fallbackMaterial.put(modId, material);
    }

    @NotNull
    public Material getFallbackMaterial(java.lang.String modId) {
        return fallbackMaterial.get(modId);
    }

    public void closeRegistry() {
        registrationPhase = Phase.CLOSED;
    }

    @Override
    public void freeze() {
        super.freeze();
        registrationPhase = Phase.FROZEN;
    }

    @Override
    public void unfreeze() {
        super.unfreeze();
        registrationPhase = Phase.OPEN;
    }

    @NotNull
    public Phase getPhase() {
        return registrationPhase;
    }

    public boolean canModifyMaterials() {
        return getPhase() != Phase.FROZEN && getPhase() != Phase.PRE;
    }

    public enum Phase {
        /** Material Registration and Modification is not started */
        PRE,
        /** Material Registration and Modification is available */
        OPEN,
        /** Material Registration is unavailable and only Modification is available */
        CLOSED,
        /** Material Registration and Modification is unavailable */
        FROZEN
    }
}
