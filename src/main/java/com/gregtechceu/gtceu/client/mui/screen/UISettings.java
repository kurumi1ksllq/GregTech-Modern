package com.gregtechceu.gtceu.client.mui.screen;

import com.gregtechceu.gtceu.api.mui.base.JeiSettings;
import com.gregtechceu.gtceu.api.mui.base.UIFactory;
import com.gregtechceu.gtceu.api.mui.factory.GuiData;
import com.gregtechceu.gtceu.api.mui.factory.PosGuiData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.IntFunction;
import java.util.function.Predicate;

public class UISettings {

    public static final double DEFAULT_INTERACT_RANGE = 8.0;

    private IntFunction<ModularContainerMenu> containerSupplier;
    private Predicate<Player> canInteractWith;
    private final JeiSettings jeiSettings;

    public UISettings() {
        this(new JeiSettingsImpl());
    }

    public UISettings(JeiSettings jeiSettings) {
        this.jeiSettings = jeiSettings;
    }

    /**
     * A function for a custom {@link ModularContainerMenu} implementation. This overrides {@link UIFactory#createContainer(int)}.
     *
     * @param containerSupplier container creator function. Must return a new instance.
     */
    public void customContainer(IntFunction<ModularContainerMenu> containerSupplier) {
        this.containerSupplier = containerSupplier;
    }

    /**
     * Overrides the default can interact check of {@link UIFactory#canInteractWith(Player, GuiData)}.
     *
     * @param canInteractWith function to test if a player can interact with the ui. This is called every tick while UI is open. Once this
     *                        function returns false, the UI is immediately closed.
     */
    public void canInteractWith(Predicate<Player> canInteractWith) {
        this.canInteractWith = canInteractWith;
    }

    @ApiStatus.Internal
    public <D extends GuiData> void defaultCanInteractWith(UIFactory<D> factory, D guiData) {
        canInteractWith(player -> factory.canInteractWith(player, guiData));
    }

    public void canInteractWithinRange(double x, double y, double z, double range) {
        canInteractWith(player -> player.distanceToSqr(x, y, z) <= range * range);
    }

    public void canInteractWithinRange(BlockPos pos, double range) {
        canInteractWith(player -> player.distanceToSqr(pos.getCenter()) <= range * range);
    }

    public void canInteractWithinRange(PosGuiData guiData, double range) {
        canInteractWithinRange(guiData.getX() + 0.5, guiData.getY() + 0.5, guiData.getZ() + 0.5, range);
    }

    public void canInteractWithinDefaultRange(double x, double y, double z) {
        canInteractWithinRange(x, y, z, DEFAULT_INTERACT_RANGE);
    }

    public void canInteractWithinDefaultRange(BlockPos pos) {
        canInteractWithinRange(pos, DEFAULT_INTERACT_RANGE);
    }

    public void canInteractWithinDefaultRange(PosGuiData guiData) {
        canInteractWithinRange(guiData, DEFAULT_INTERACT_RANGE);
    }

    public JeiSettings getJeiSettings() {
        return jeiSettings;
    }

    @ApiStatus.Internal
    public ModularContainerMenu createContainer(int containerId) {
        return containerSupplier.apply(containerId);
    }

    public boolean hasContainer() {
        return containerSupplier != null;
    }

    public boolean canPlayerInteractWithUI(Player player) {
        return canInteractWith == null || canInteractWith.test(player);
    }
}
