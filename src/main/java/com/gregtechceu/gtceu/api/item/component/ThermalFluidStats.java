package com.gregtechceu.gtceu.api.item.component;

import com.gregtechceu.gtceu.api.fluids.attribute.FluidAttribute;
import com.gregtechceu.gtceu.api.fluids.attribute.FluidAttributes;
import com.gregtechceu.gtceu.api.item.component.forge.IComponentCapability;
import com.gregtechceu.gtceu.api.misc.forge.SimpleThermalFluidHandlerItemStack;
import com.gregtechceu.gtceu.api.misc.forge.ThermalFluidHandlerItemStack;
import com.gregtechceu.gtceu.client.TooltipsHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ThermalFluidStats implements IItemComponent, IComponentCapability, IAddInformation {

    public final int capacity;
    public final int maxFluidTemperature;
    public final int minFluidTemperature;
    public final boolean gasProof;
    public final boolean plasmaProof;
    public final boolean allowPartialFill;

    private final Object2BooleanMap<FluidAttribute> containmentPredicate = new Object2BooleanOpenHashMap<>();

    protected ThermalFluidStats(int capacity, int maxFluidTemperature, int minFluidTemperature,
                                boolean gasProof, boolean plasmaProof, boolean acidProof,
                                boolean allowPartialFill) {
        this.capacity = capacity;
        this.maxFluidTemperature = maxFluidTemperature;
        this.minFluidTemperature = minFluidTemperature;
        this.gasProof = gasProof;
        if (acidProof) setCanContain(FluidAttributes.ACID, true);
        this.plasmaProof = plasmaProof;
        this.allowPartialFill = allowPartialFill;
    }

    public static ThermalFluidStats create(int capacity, int maxFluidTemperature, int minFluidTemperature,
                                           boolean gasProof, boolean acidProof, boolean plasmaProof,
                                           boolean allowPartialFill) {
        return new ThermalFluidStats(capacity, maxFluidTemperature, minFluidTemperature,
                gasProof, acidProof, plasmaProof,
                allowPartialFill);
    }

    public static ThermalFluidStats create(int capacity, @NotNull FluidPipeProperties properties,
                                           boolean allowPartialFill) {
        return new ThermalFluidStats(capacity, properties.getMaxFluidTemperature(), properties.isGasProof(),
                properties.isAcidProof(), properties.isCryoProof(), properties.isPlasmaProof(), allowPartialFill);
    }

    public ThermalFluidStats setCanContain(@NotNull FluidAttribute attribute, boolean canContain) {
        containmentPredicate.put(attribute, canContain);
        return this;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(ItemStack itemStack, @NotNull Capability<T> cap) {
        if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
            return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(cap, LazyOptional.of(() -> {
                if (allowPartialFill) {
                    return new ThermalFluidHandlerItemStack(itemStack, capacity,
                            maxFluidTemperature, minFluidTemperature,
                            gasProof, plasmaProof);
                }
                return new SimpleThermalFluidHandlerItemStack(itemStack, capacity,
                        maxFluidTemperature, minFluidTemperature,
                        gasProof, plasmaProof);
            }));
        }
        return LazyOptional.empty();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        if (stack.hasTag()) {
            FluidUtil.getFluidContained(stack).ifPresent(tank -> {
                tooltipComponents
                        .add(Component.translatable("gtceu.universal.tooltip.fluid_stored", tank.getDisplayName(),
                                tank.getAmount()));
                TooltipsHandler.appendFluidTooltips(tank, tooltipComponents::add, null);
            });
        } else {
            tooltipComponents.add(Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity",
                    FormattingUtil.formatNumbers(capacity)));
        }
        if (GTUtil.isShiftDown()) {
            tooltipComponents.add(Component.translatable("gtceu.fluid_pipe.max_temperature", maxFluidTemperature));
            tooltipComponents.add(Component.translatable("gtceu.fluid_pipe.min_temperature", minFluidTemperature));
            if (gasProof) tooltipComponents.add(Component.translatable("gtceu.fluid_pipe.gas_proof"));
            else tooltipComponents.add(Component.translatable("gtceu.fluid_pipe.not_gas_proof"));
            if (plasmaProof) tooltipComponents.add(Component.translatable("gtceu.fluid_pipe.plasma_proof"));
            containmentPredicate.keySet().forEach(a -> a.appendContainerTooltips(tooltipComponents::add));
        } else if (gasProof || plasmaProof || !containmentPredicate.isEmpty()) {
            tooltipComponents.add(Component.translatable("gtceu.tooltip.fluid_pipe_hold_shift"));
        }
    }
}
