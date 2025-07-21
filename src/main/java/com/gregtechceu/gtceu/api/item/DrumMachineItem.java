package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.misc.forge.ThermalFluidHandlerItemStack;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.common.pipelike.handlers.properties.MaterialFluidProperties;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DrumMachineItem extends MetaMachineItem {

    @NotNull
    private Material mat = GTMaterials.NULL;

    protected DrumMachineItem(IMachineBlock block, Properties properties, @NotNull Material mat) {
        super(block, properties);
        this.mat = mat;
    }

    public static DrumMachineItem create(IMachineBlock block, Properties properties, @NotNull Material mat) {
        return new DrumMachineItem(block, properties, mat);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        if (!mat.hasProperty(PropertyKey.PIPENET_PROPERTIES)) {
            return null;
        }
        MaterialFluidProperties property = mat.getProperty(PropertyKey.PIPENET_PROPERTIES)
                .getProperty(MaterialFluidProperties.KEY);
        if (property == null) {
            return null;
        }

        return new ThermalFluidHandlerItemStack(stack,
                GTMachineUtils.DRUM_CAPACITY.get(getDefinition()),
                property.getMaxFluidTemperature(), property.getMinFluidTemperature(),
                property.isGasProof(), property.isPlasmaProof());
    }
}
