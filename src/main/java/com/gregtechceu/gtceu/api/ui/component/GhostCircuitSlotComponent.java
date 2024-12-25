package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.container.GridLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.Surface;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Used for setting a "ghost" IC for a machine
 */
public class GhostCircuitSlotComponent extends SlotComponent {

    private static final int SET_TO_ZERO = 1;
    private static final int SET_TO_EMPTY = 2;
    private static final int SET_TO_N = 3;

    private static final int NO_CONFIG = -1;

    @Getter
    private IItemHandlerModifiable circuitInventory;
    @Nullable
    private UIComponent configurator;

    public GhostCircuitSlotComponent() {
        super(0);
    }

    public void setCircuitInventory(IItemHandlerModifiable circuitInventory) {
        this.circuitInventory = circuitInventory;
        setSlot(circuitInventory, 0);
    }

    public boolean isConfiguratorOpen() {
        return configurator != null;
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY) && containerAccess().adapter() != null) {
            if (button == 0 && Screen.hasShiftDown()) {
                // open popup on shift-left-click
                if (!isConfiguratorOpen()) {
                    // FIXME add the widget somehow
                    // this.containerAccess().adapter().rootComponent.child(configurator = createConfigurator());
                } else {
                    // FIXME add the widget somehow
                    // this.containerAccess().adapter().rootComponent.removeWidget(configurator);
                    configurator = null;
                }
            } else if (button == 0) {
                // increment on left-click
                int newValue = getNextValue(true);
                setCircuitValue(newValue);
            } else if (button == 1 && Screen.hasShiftDown()) {
                // clear on shift-right-click
                this.circuitInventory.setStackInSlot(0, ItemStack.EMPTY);
                //sendMessage(SET_TO_EMPTY, buf -> {});
            } else if (button == 1) {
                // decrement on right-click
                int newValue = getNextValue(false);
                setCircuitValue(newValue);
            }
            return true;
        }
        return false;
    }

    private int getNextValue(boolean increment) {
        int currentValue = IntCircuitBehaviour.getCircuitConfiguration(this.circuitInventory.getStackInSlot(0));
        if (increment) {
            // if at max, loop around to no circuit
            if (currentValue == IntCircuitBehaviour.CIRCUIT_MAX) {
                return 0;
            }
            // if at no circuit, skip 0 and return 1
            if (this.circuitInventory.getStackInSlot(0).isEmpty()) {
                return 1;
            }
            // normal case: increment by 1
            return currentValue + 1;
        } else {
            // if at no circuit, loop around to max
            if (this.circuitInventory.getStackInSlot(0).isEmpty()) {
                return IntCircuitBehaviour.CIRCUIT_MAX;
            }
            // if at 1, skip 0 and return no circuit
            if (currentValue == 1) {
                return NO_CONFIG;
            }
            // normal case: decrement by 1
            return currentValue - 1;
        }
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        if (isConfiguratorOpen()) return true;
        if (isMouseOverElement(mouseX, mouseY)) {
            int newValue = getNextValue(amount >= 0);
            setCircuitValue(newValue);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        return false;
    }

    // @Override
    // public boolean canMergeSlot(ItemStack stack) {
    // return false;
    // }

    public void setCircuitValue(int newValue) {
        if (newValue == NO_CONFIG) {
            this.circuitInventory.setStackInSlot(0, ItemStack.EMPTY);
            //sendMessage(SET_TO_EMPTY, buf -> {});
        } else {
            this.circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(newValue));
            //sendMessage(SET_TO_N, buf -> buf.writeVarInt(newValue));
        }
    }

    /*
    @Override
    public void receiveMessage(int id, FriendlyByteBuf buf) {
        switch (id) {
            case SET_TO_ZERO -> this.circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(0));
            case SET_TO_EMPTY -> this.circuitInventory.setStackInSlot(0, ItemStack.EMPTY);
            case SET_TO_N -> this.circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(buf.readVarInt()));
        }
    }
    */

    public UIComponent createConfigurator() {
        var group = UIContainers.horizontalFlow(Sizing.fixed(174), Sizing.fixed(132));
        // FIXME WHY IS THIS NOT TRANSLATABLE WHAT
        group.child(UIComponents.label(Component.translatable("Programmed Circuit Configuration"))
                .positioning(Positioning.absolute(9, 8)));
        group.child(UIComponents.slot(this.circuitInventory, 0)
                .canInsert(!ConfigHolder.INSTANCE.machines.ghostCircuit)
                .canExtract(!ConfigHolder.INSTANCE.machines.ghostCircuit)
                .backgroundTexture(UITextures.group(GuiTextures.SLOT, GuiTextures.INT_CIRCUIT_OVERLAY)));
        if (ConfigHolder.INSTANCE.machines.ghostCircuit) {
            group.child(UIComponents.button(Component.empty(),
                    clickData -> {
                        if (!clickData.isClientSide) {
                            circuitInventory.setStackInSlot(0, ItemStack.EMPTY);
                        }
                    })
                    .renderer(ButtonComponent.Renderer.EMPTY)
                    .positioning(Positioning.relative(50, 15))
                    .sizing(Sizing.fixed(18)));
        }

        GridLayout grid = UIContainers.grid(Sizing.content(), Sizing.content(), 9, 4)
                .configure(layout -> layout.positioning(Positioning.absolute(5, 48)));
        int idx = 0;
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 8; y++) {
                int finalIdx = idx;
                grid.child(UIComponents.button(Component.empty(),
                        clickData -> {
                            if (!clickData.isClientSide) {
                                ItemStack stack = circuitInventory.getStackInSlot(0).copy();
                                if (IntCircuitBehaviour.isIntegratedCircuit(stack)) {
                                    IntCircuitBehaviour.setCircuitConfiguration(stack, finalIdx);
                                    circuitInventory.setStackInSlot(0, stack);
                                } else if (ConfigHolder.INSTANCE.machines.ghostCircuit) {
                                    circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(finalIdx));
                                }
                            }
                        })
                        .renderer(ButtonComponent.Renderer.texture(UITextures.group(GuiTextures.SLOT,
                                UITextures.item(IntCircuitBehaviour.stack(finalIdx)).scale(16f / 18))))
                        .sizing(Sizing.fixed(18)),
                        y, x);
                idx++;
            }
        }
        for (int x = 0; x <= 5; x++) {
            int finalIdx = x + 27;
            grid.child(UIComponents.button(Component.empty(),
                    clickData -> {
                        if (!clickData.isClientSide) {
                            ItemStack stack = circuitInventory.getStackInSlot(0).copy();
                            if (IntCircuitBehaviour.isIntegratedCircuit(stack)) {
                                IntCircuitBehaviour.setCircuitConfiguration(stack, finalIdx);
                                circuitInventory.setStackInSlot(0, stack);
                            } else if (ConfigHolder.INSTANCE.machines.ghostCircuit) {
                                circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(finalIdx));
                            }
                        }
                    })
                    .renderer(ButtonComponent.Renderer.texture(UITextures.group(GuiTextures.SLOT,
                            UITextures.item(IntCircuitBehaviour.stack(finalIdx)).scale(16f / 18))))
                    .sizing(Sizing.fixed(18)), 9, x);
        }
        group.child(grid);
        group.surface(Surface.UI_BACKGROUND);
        return group;
    }
}
