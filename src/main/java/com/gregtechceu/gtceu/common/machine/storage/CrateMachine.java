package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.*;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.mui.base.drawable.IKey;
import com.gregtechceu.gtceu.api.mui.base.widget.IWidget;
import com.gregtechceu.gtceu.api.mui.factory.PosGuiData;
import com.gregtechceu.gtceu.api.mui.value.sync.PanelSyncManager;
import com.gregtechceu.gtceu.api.mui.value.sync.SyncHandlers;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Grid;
import com.gregtechceu.gtceu.api.mui.widgets.slot.ItemSlot;
import com.gregtechceu.gtceu.client.mui.screen.ModularPanel;
import com.gregtechceu.gtceu.client.mui.screen.UISettings;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.common.mui.GTGuis;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CrateMachine extends MetaMachine implements IMuiMachine, IMachineLife,
                          IDropSaveMachine, IInteractedMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(CrateMachine.class,
            MetaMachine.MANAGED_FIELD_HOLDER);

    public static final BooleanProperty TAPED_PROPERTY = GTMachineModelProperties.IS_TAPED;

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Getter
    private final Material material;
    @Getter
    private final int inventorySize;
    @Getter
    private final int rowLength;
    @Getter
    @RequireRerender
    @Persisted
    @DescSynced
    private boolean isTaped;

    @Persisted
    public final NotifiableItemStackHandler inventory;

    public CrateMachine(IMachineBlockEntity holder, Material material, int inventorySize, int rowLength) {
        super(holder);
        this.material = material;
        this.inventorySize = inventorySize;
        this.rowLength = rowLength;
        this.inventory = new NotifiableItemStackHandler(this, inventorySize, IO.BOTH);
    }

    /**
     * @deprecated Use the method that accepts a specific row length.
     */
    @Deprecated(since = "7.0.0")
    public CrateMachine(IMachineBlockEntity holder, Material material, int inventorySize) {
        this(holder, material, inventorySize, inventorySize >= 90 ? 18 : 9);
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerSlotGroup("item_inv", inventorySize);

        int rows = inventorySize / rowLength;
        List<List<IWidget>> widgets = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            widgets.add(new ArrayList<>());
            for (int j = 0; j < this.rowLength; j++) {
                int index = i * rowLength + j;
                widgets.get(i).add(new ItemSlot()
                        .slot(SyncHandlers.itemSlot(inventory, index).slotGroup("item_inv"))
                        .background(GTGuiTextures.SLOT));
            }
        }

        return GTGuis.createPanel(this, rowLength * 18 + 14, 18 + 4 * 18 + 5 + 14 + 18 * rows)
                .background(GTGuiTextures.BACKGROUND_STEEL)
                .child(IKey.lang(getBlockState().getBlock().getName()).asWidget().pos(5, 5))
                .child(new Grid()
                        .top(18).left(7).right(7).height(rows * 18)
                        .minElementMargin(0, 0)
                        .minColWidth(18).minRowHeight(18)
                        .matrix(widgets))
                .bindPlayerInventory();
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
                setRenderState(getRenderState().setValue(GTMachineModelProperties.IS_TAPED, isTaped));
                return InteractionResult.sidedSuccess(world.isClientSide);
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
            setRenderState(getRenderState().setValue(GTMachineModelProperties.IS_TAPED, isTaped));
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
