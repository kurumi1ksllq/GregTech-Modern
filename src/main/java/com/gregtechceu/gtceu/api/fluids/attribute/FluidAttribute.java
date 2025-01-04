package com.gregtechceu.gtceu.api.fluids.attribute;

import com.gregtechceu.gtceu.api.fluids.ContainmentFailureHandler;
import com.gregtechceu.gtceu.api.fluids.GTFluid;
import com.gregtechceu.gtceu.utils.TriConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class FluidAttribute implements ContainmentFailureHandler {

    @Getter
    @NotNull
    private final ResourceLocation resourceLocation;
    private final Consumer<Consumer<Component>> fluidTooltip;
    private final Consumer<Consumer<Component>> containerTooltip;
    private final TriConsumer<Level, BlockPos, FluidStack> blockContainmentFailure;
    private final BiConsumer<Player, FluidStack> playerContainmentFailure;
    private final int hashCode;

    public FluidAttribute(@NotNull ResourceLocation resourceLocation,
                          @NotNull Consumer<Consumer<@NotNull Component>> fluidTooltip,
                          @NotNull Consumer<Consumer<@NotNull Component>> containerTooltip,
                          @NotNull TriConsumer<Level, BlockPos, FluidStack> blockContainmentFailure,
                          @NotNull BiConsumer<Player, FluidStack> playerContainmentFailure) {
        this.resourceLocation = resourceLocation;
        this.fluidTooltip = fluidTooltip;
        this.containerTooltip = containerTooltip;
        this.hashCode = resourceLocation.hashCode();
        this.blockContainmentFailure = blockContainmentFailure;
        this.playerContainmentFailure = playerContainmentFailure;
    }

    public static Collection<FluidAttribute> inferAttributes(FluidStack stack) {
        if (stack.getFluid() instanceof GTFluid fluid) return fluid.getAttributes();
        else return Collections.emptyList();
    }

    public void appendFluidTooltips(@NotNull Consumer<@NotNull Component> tooltip) {
        fluidTooltip.accept(tooltip);
    }

    public void appendContainerTooltips(@NotNull Consumer<@NotNull Component> tooltip) {
        containerTooltip.accept(tooltip);
    }

    @Override
    public void handleFailure(Level world, BlockPos failingBlock, FluidStack failingStack) {
        blockContainmentFailure.accept(world, failingBlock, failingStack);
    }

    @Override
    public void handleFailure(Player failingPlayer, FluidStack failingStack) {
        playerContainmentFailure.accept(failingPlayer, failingStack);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FluidAttribute that = (FluidAttribute) o;

        return resourceLocation.equals(that.getResourceLocation());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public @NotNull String toString() {
        return "FluidAttribute{" + resourceLocation + '}';
    }
}
