package com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid;

import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FluidVeinWorldEntry {

    @Nullable
    @Getter
    private BedrockFluidDefinition definition;
    @Getter
    private int fluidYield;
    @Getter
    private int operationsRemaining;

    public FluidVeinWorldEntry(@Nullable BedrockFluidDefinition definition, int fluidYield, int operationsRemaining) {
        this.definition = definition;
        this.fluidYield = fluidYield;
        this.operationsRemaining = operationsRemaining;
    }

    private FluidVeinWorldEntry() {}

    @SuppressWarnings("unused")
    public void setOperationsRemaining(int amount) {
        this.operationsRemaining = amount;
    }

    public void decreaseOperations(int amount) {
        operationsRemaining = ConfigHolder.INSTANCE.worldgen.oreVeins.infiniteBedrockOresFluids ? operationsRemaining :
                Math.max(0, operationsRemaining - amount);
    }

    public CompoundTag writeToNBT() {
        var tag = new CompoundTag();
        tag.putInt("fluidYield", fluidYield);
        tag.putInt("operationsRemaining", operationsRemaining);
        if (definition != null && GTRegistries.BEDROCK_FLUID_DEFINITIONS.getKey(definition) != null) {
            tag.putString("vein", GTRegistries.BEDROCK_FLUID_DEFINITIONS.getKey(definition).toString());
        }
        return tag;
    }

    @NotNull
    public static FluidVeinWorldEntry readFromNBT(@NotNull CompoundTag tag) {
        FluidVeinWorldEntry info = new FluidVeinWorldEntry();
        info.fluidYield = tag.getInt("fluidYield");
        info.operationsRemaining = tag.getInt("operationsRemaining");

        if (tag.contains("vein")) {
            ResourceLocation id = new ResourceLocation(tag.getString("vein"));
            if (GTRegistries.BEDROCK_FLUID_DEFINITIONS.containKey(id)) {
                info.definition = GTRegistries.BEDROCK_FLUID_DEFINITIONS.get(id);
            }
        }
        return info;
    }
}
