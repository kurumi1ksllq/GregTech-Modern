package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.component.ButtonComponent;
import com.gregtechceu.gtceu.api.ui.component.SlotComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.GridLayout;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.Surface;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;
import com.gregtechceu.gtceu.api.ui.holder.HeldItemUIHolder;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;
import com.gregtechceu.gtceu.api.ui.util.SlotGenerator;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author KilaBash
 * @date 2023/2/23
 * @implNote IntCircuitBehaviour
 */
public class IntCircuitBehaviour implements IItemUIFactory, IAddInformation {

    public static final int CIRCUIT_MAX = 32;

    public static ItemStack stack(int configuration) {
        var stack = GTItems.PROGRAMMED_CIRCUIT.asStack();
        setCircuitConfiguration(stack, configuration);
        return stack;
    }

    public static void setCircuitConfiguration(HeldItemUIHolder holder, int configuration) {
        setCircuitConfiguration(holder.getHeld(), configuration);
        holder.markDirty();
    }

    public static void setCircuitConfiguration(ItemStack itemStack, int configuration) {
        if (configuration < 0 || configuration > CIRCUIT_MAX)
            throw new IllegalArgumentException("Given configuration number is out of range!");
        var tagCompound = itemStack.getOrCreateTag();
        tagCompound.putInt("Configuration", configuration);
    }

    public static int getCircuitConfiguration(ItemStack itemStack) {
        if (!isIntegratedCircuit(itemStack)) return 0;
        var tagCompound = itemStack.getTag();
        if (tagCompound != null) {
            return tagCompound.getInt("Configuration");
        }
        return 0;
    }

    public static boolean isIntegratedCircuit(ItemStack itemStack) {
        boolean isCircuit = GTItems.PROGRAMMED_CIRCUIT.isIn(itemStack);
        if (isCircuit && !itemStack.hasTag()) {
            var compound = new CompoundTag();
            compound.putInt("Configuration", 0);
            itemStack.setTag(compound);
        }
        return isCircuit;
    }

    // deprecated, not needed (for now)
    @Deprecated
    public static void adjustConfiguration(HeldItemUIHolder holder, int amount) {
        adjustConfiguration(holder.getHeld(), amount);
        holder.markDirty();
    }

    // deprecated, not needed (for now)
    @Deprecated
    public static void adjustConfiguration(ItemStack stack, int amount) {
        if (!isIntegratedCircuit(stack)) return;
        int configuration = getCircuitConfiguration(stack);
        configuration += amount;
        configuration = Mth.clamp(configuration, 0, CIRCUIT_MAX);
        setCircuitConfiguration(stack, configuration);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        int configuration = getCircuitConfiguration(stack);
        tooltipComponents.add(Component.translatable("metaitem.int_circuit.configuration", configuration));
    }

    @Override
    public void loadServerUI(Player player, UIContainerMenu<HeldItemUIHolder> menu, HeldItemUIHolder holder) {
        var generator = SlotGenerator.begin(menu::addSlot, 0, 0);
        var handler = new CustomItemStackHandler(stack(getCircuitConfiguration(holder.getHeld())));
        generator.slot(handler, 0, 0, 0);
        var prop = menu.createProperty(int.class, "stack", getCircuitConfiguration(handler.getStackInSlot(0)));
        handler.setOnContentsChanged(() -> {
            prop.set(getCircuitConfiguration(handler.getStackInSlot(0)));
        });
    }

    @Override
    public void loadClientUI(Player entityPlayer, UIAdapter<StackLayout> adapter, HeldItemUIHolder holder) {
        var group = UIContainers.stack(Sizing.fixed(184), Sizing.fixed(132));
        adapter.rootComponent.child(group);

        var handler = new CustomItemStackHandler(stack(getCircuitConfiguration(holder.getHeld())));
        var prop = adapter.menu().<Integer>getProperty("stack");

        var slot = UIComponents.slot(handler, 0)
                .backgroundTexture(GuiTextures.SLOT)
                .<SlotComponent>configure(c -> {
                    c.positioning(Positioning.absolute(82, 20));
                });
        group.child(slot);

        GridLayout grid = UIContainers.grid(Sizing.content(), Sizing.content(), 4, 9)
                .configure(layout -> layout.positioning(Positioning.absolute(5, 48)));
        int idx = 0;
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 8; y++) {
                int finalIdx = idx;
                grid.child(UIComponents.button(Component.empty(),
                        clickData -> {
                            setCircuitConfiguration(holder, finalIdx);
                            prop.set(finalIdx);
                        })
                        .renderer(ButtonComponent.Renderer.texture(UITextures.group(GuiTextures.SLOT,
                                UITextures.item(IntCircuitBehaviour.stack(finalIdx)).scale(16f / 18))))
                        .sizing(Sizing.fixed(18)),
                        x, y);
                idx++;
            }
        }
        for (int x = 0; x <= 5; x++) {
            int finalIdx = x + 27;
            grid.child(UIComponents.button(Component.empty(),
                    clickData -> {
                        setCircuitConfiguration(holder, finalIdx);
                        prop.set(finalIdx);
                    })
                    .renderer(ButtonComponent.Renderer.texture(UITextures.group(GuiTextures.SLOT,
                            UITextures.item(IntCircuitBehaviour.stack(finalIdx)).scale(16f / 18))))
                    .sizing(Sizing.fixed(18)),
                    3, x);
        }
        group.child(grid);
        group.surface(Surface.UI_BACKGROUND);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var stack = context.getItemInHand();
        int circuitSetting = getCircuitConfiguration(stack);
        BlockEntity entity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (entity instanceof MetaMachineBlockEntity machineEntity && context.isSecondaryUseActive()) {
            if (machineEntity.metaMachine instanceof IHasCircuitSlot circuitMachine &&
                    circuitMachine.getCircuitInventory().getSlots() > 0) {
                setCircuitConfig(circuitMachine.getCircuitInventory(), circuitSetting);
            }
            if (!ConfigHolder.INSTANCE.machines.ghostCircuit)
                stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return IItemUIFactory.super.useOn(context);
    }

    void setCircuitConfig(NotifiableItemStackHandler circuit, int value) {
        circuit.setStackInSlot(0, IntCircuitBehaviour.stack(value));
    }
}
