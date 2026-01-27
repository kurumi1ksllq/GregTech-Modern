package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.mui.base.drawable.IKey;
import com.gregtechceu.gtceu.api.mui.drawable.DynamicDrawable;
import com.gregtechceu.gtceu.api.mui.drawable.ItemDrawable;
import com.gregtechceu.gtceu.api.mui.drawable.text.TextRenderer;
import com.gregtechceu.gtceu.api.mui.factory.PosGuiData;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.value.sync.GenericMapSyncHandler;
import com.gregtechceu.gtceu.api.mui.value.sync.PanelSyncManager;
import com.gregtechceu.gtceu.api.mui.widget.Widget;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Column;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Row;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.client.mui.screen.ModularPanel;
import com.gregtechceu.gtceu.client.mui.screen.UISettings;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.common.mui.GTGuis;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;
import com.gregtechceu.gtceu.syncsystem.annotations.SaveField;
import com.gregtechceu.gtceu.utils.ICopy;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The Output Bus that can directly send its contents to ME storage network.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MEOutputBusPartMachine extends MEBusPartMachine implements IMachineLife, IInteractedMachine {

    @SaveField
    private KeyStorage internalBuffer; // Do not use KeyCounter, use our simple implementation

    public MEOutputBusPartMachine(BlockEntityCreationInfo info) {
        super(info, IO.OUT);
    }

    /////////////////////////////////
    // ***** Machine LifeCycle ****//
    /////////////////////////////////

    @Override
    protected NotifiableItemStackHandler createInventory() {
        this.internalBuffer = new KeyStorage();
        return new InaccessibleInfiniteHandler(this);
    }

    @Override
    public void onMachineRemoved() {
        var grid = getMainNode().getGrid();
        if (grid != null && !internalBuffer.isEmpty()) {
            for (var entry : internalBuffer) {
                grid.getStorageService().getInventory().insert(entry.getKey(), entry.getValue(),
                        Actionable.MODULATE, actionSource);
            }
        }
    }

    /////////////////////////////////
    // ********** Sync ME *********//
    /////////////////////////////////

    @Override
    protected boolean shouldSubscribe() {
        return super.shouldSubscribe() && !internalBuffer.storage.isEmpty();
    }

    @Override
    public void autoIO() {
        if (!this.shouldSyncME()) return;
        if (this.updateMEStatus()) {
            var grid = getMainNode().getGrid();
            if (grid != null && !internalBuffer.isEmpty()) {
                internalBuffer.insertInventory(grid.getStorageService().getInventory(), actionSource);
            }
            this.updateInventorySubscription();
        }
    }

    ///////////////////////////////
    // ********** GUI ***********//
    ///////////////////////////////

    /*
     * @Override
     * public Widget createUIWidget() {
     * WidgetGroup group = new WidgetGroup(0, 0, 170, 65);
     * // ME Network status
     * group.addWidget(new LabelWidget(5, 0, () -> this.isOnline ?
     * "gtceu.gui.me_network.online" :
     * "gtceu.gui.me_network.offline"));
     * group.addWidget(new LabelWidget(5, 10, "gtceu.gui.waiting_list"));
     * // display list
     * group.addWidget(new AEListGridWidget.Item(5, 20, 3, this.internalBuffer));
     *
     * return group;
     * }
     */

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        int panelWidth = 176;
        int panelHeight = 118;

        var panel = GTGuis.createPanel(this, panelWidth, panelHeight);

        var displayItem = this.getDefinition().asStack();
        String hatchName = displayItem.getHoverName().getString();
        hatchName = hatchName.replaceAll("§.", "").trim();

        int borderRadius = 5;
        int iconSize = 16;
        int minPanelWidth = (int) (panelWidth * 0.8f) - (iconSize + (borderRadius * 2));
        int textTitleWidth = TextRenderer.getFont().width(hatchName) + iconSize + (borderRadius * 2);

        int textRows = (int) Math.ceil((double) textTitleWidth / minPanelWidth);
        int textHeightPerRow = (int) (IKey.renderer.getFontHeight());
        int textHeight = textHeightPerRow * textRows + borderRadius;

        var keyStorageSyncHandler = new GenericMapSyncHandler<>(() -> internalBuffer.storage,
                (map) -> internalBuffer.storage = map,
                AEKey::readKey, FriendlyByteBuf::readLong,
                AEKey::writeKey, FriendlyByteBuf::writeLong,
                Objects::equals, ICopy.immutable(), ICopy.immutable());

        syncManager.syncValue("keyStorage", keyStorageSyncHandler);

        panel.child(new Row()
                .coverChildrenHeight()
                .mainAxisAlignment(Alignment.MainAxis.CENTER)
                .widthRel(.8f)
                .top(-(textHeight + borderRadius))
                .rightRel(0.5f)
                .background(GTGuiTextures.BACKGROUND)
                .child(new ItemDrawable(displayItem)
                        .asIcon().size(iconSize)
                        .asWidget()
                        .marginLeft(borderRadius))
                .mainAxisAlignment(Alignment.MainAxis.START)
                .child(IKey.str(hatchName)
                        .asWidget()
                        .paddingTop(1)
                        .margin(borderRadius, borderRadius, borderRadius, 1)
                        .size(textTitleWidth, textHeight)));

        var widget = new Column().name("ae_list");
        for (var entry : keyStorageSyncHandler.getValue().entrySet()) {
            AEKey key = entry.getKey();

            var drawable = new ItemDrawable();
            widget.child(new Row()
                    .child(new Widget<>()
                            .overlay(new DynamicDrawable(() -> drawable.setItem(key.wrapForDisplayOrFilter()))))
                    .child(IKey.dynamic(() -> {
                        ItemStack stack = key.wrapForDisplayOrFilter();
                        return Component.literal(stack.getDisplayName().getString() + " " + entry.getValue());
                    }).asWidget()));
        }

        panel.child(widget);

        return panel;
    }

    private class InaccessibleInfiniteHandler extends NotifiableItemStackHandler {

        public InaccessibleInfiniteHandler(MetaMachine holder) {
            super(holder, 1, IO.OUT, IO.NONE, ItemStackHandlerDelegate::new);
            internalBuffer.setOnContentsChanged(this::onContentsChanged);
        }

        @Override
        public @NotNull List<Object> getContents() {
            return Collections.emptyList();
        }

        @Override
        public double getTotalContentAmount() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    }

    @NoArgsConstructor
    private class ItemStackHandlerDelegate extends CustomItemStackHandler {

        // Necessary for InaccessibleInfiniteHandler
        public ItemStackHandlerDelegate(Integer integer) {
            super();
        }

        @Override
        public int getSlots() {
            return Short.MAX_VALUE;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            // NO-OP
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            var key = AEItemKey.of(stack);
            int count = stack.getCount();
            long oldValue = internalBuffer.storage.getOrDefault(key, 0L);
            long changeValue = Math.min(Long.MAX_VALUE - oldValue, count);
            if (changeValue > 0) {
                if (!simulate) {
                    internalBuffer.storage.put(key, oldValue + changeValue);
                    internalBuffer.onChanged();
                }
                return stack.copyWithCount((int) (count - changeValue));
            } else {
                return ItemStack.EMPTY;
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }
}
