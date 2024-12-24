package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.*;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.GridLayout;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.util.SlotGenerator;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author h3tR
 * @date 2023/3/27
 * @implNote CrateMachine
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CrateMachine extends MetaMachine implements IUIMachine, IMachineLife,
                          IDropSaveMachine, IInteractedMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(CrateMachine.class,
            MetaMachine.MANAGED_FIELD_HOLDER);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Getter
    private final Material material;
    @Getter
    private final int inventorySize;
    @Getter
    @RequireRerender
    @Persisted
    @DescSynced
    private boolean isTaped;

    @Persisted
    public final NotifiableItemStackHandler inventory;

    public CrateMachine(IMachineBlockEntity holder, Material material, int inventorySize) {
        super(holder);
        this.material = material;
        this.inventorySize = inventorySize;
        this.inventory = new NotifiableItemStackHandler(this, inventorySize, IO.BOTH);
    }

    @Override
    public void loadServerUI(Player player, UIContainerMenu<MetaMachine> menu, MetaMachine holder) {
        var generator = SlotGenerator.begin(menu::addSlot, 0, 0);
        for (int slot = 0; slot < inventorySize; slot++) {
            generator.slot(inventory, slot, 0, 0);
        }

        generator.playerInventory(player.getInventory());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void loadClientUI(Player player, UIAdapter<StackLayout> adapter, MetaMachine holder) {
        StackLayout rootComponent = adapter.rootComponent;

        int xOffset = inventorySize >= 90 ? 162 : 0;
        int yOverflow = xOffset > 0 ? 18 : 9;
        int yOffset = inventorySize > 3 * yOverflow ?
                (inventorySize - 3 * yOverflow - (inventorySize - 3 * yOverflow) % yOverflow) / yOverflow * 18 : 0;

        FlowLayout parent;
        rootComponent.child(parent = (FlowLayout) UIContainers
                .horizontalFlow(Sizing.fixed(176 + xOffset), Sizing.fixed(166 + yOffset))
                .child(UIComponents.label(getBlockState().getBlock().getName())
                        .positioning(Positioning.absolute(5, 5)))
                .child(UIComponents.playerInventory(player.getInventory(), GuiTextures.SLOT)
                        .positioning(Positioning.absolute(8 + xOffset / 2, 83 + yOffset)))
                .positioning(Positioning.relative(50, 50))
                .surface(Surface.UI_BACKGROUND));

        int x = 0;
        int y = 0;
        GridLayout grid = UIContainers.grid(Sizing.content(),
                Sizing.content(),
                inventorySize / yOverflow,
                yOverflow);
        grid.positioning(Positioning.absolute(8, 18));
        for (int slot = 0; slot < inventorySize; slot++) {
            grid.child(UIComponents.slot(inventory, slot).id("inventory." + slot),
                    y, x);
            x++;
            if (x == yOverflow) {
                x = 0;
                y++;
            }
        }
        parent.child(grid);

        // parent.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isCrouching() && !isTaped) {
            if (stack.is(GTItems.DUCT_TAPE.asItem()) || stack.is(GTItems.BASIC_TAPE.asItem())) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                isTaped = true;
                return InteractionResult.SUCCESS;
            }
        }
        return IInteractedMachine.super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public void onMachinePlaced(@Nullable LivingEntity player, ItemStack stack) {
        IMachineLife.super.onMachinePlaced(player, stack);
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            this.isTaped = tag.contains("taped") && tag.getBoolean("taped");
            if (isTaped) {
                this.inventory.storage.deserializeNBT(tag.getCompound("inventory"));
            }

            tag.remove("taped");
            this.isTaped = false;
        }
        stack.setTag(null);
    }

    @Override
    public void saveToItem(CompoundTag tag) {
        if (isTaped) {
            IDropSaveMachine.super.saveToItem(tag);
            tag.putBoolean("taped", isTaped);
            tag.put("inventory", inventory.storage.serializeNBT());
        }
    }

    @Override
    public boolean saveBreak() {
        return isTaped;
    }

    @Override
    public void onMachineRemoved() {
        if (!isTaped) clearInventory(inventory.storage);
    }
}
