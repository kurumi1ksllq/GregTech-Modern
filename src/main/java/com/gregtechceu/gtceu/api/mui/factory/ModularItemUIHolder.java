package com.gregtechceu.gtceu.api.mui.factory;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.item.module.AppliedItemModule;
import com.gregtechceu.gtceu.api.item.module.IModularItem;
import com.gregtechceu.gtceu.api.item.module.ItemModuleSlot;
import com.gregtechceu.gtceu.api.mui.GTGuiScreen;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.common.mui.GTMuiWidgets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import brachy.modularui.api.IUIHolder;
import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.api.drawable.IKey;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.GuiDraw;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.factory.GuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;
import brachy.modularui.screen.UISettings;
import brachy.modularui.screen.viewport.ModularGuiContext;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.value.sync.DynamicSyncHandler;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widgets.*;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.ItemSlot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Setter(AccessLevel.PRIVATE)
@Getter(AccessLevel.PRIVATE)
public class ModularItemUIHolder implements IUIHolder<GuiData> {

    private final Player player;
    private boolean inventoryUnlocked = false;
    private int selectedSlot = -1;
    private DynamicSyncHandler dynamicSyncHandler = null;

    public ModularItemUIHolder(Player player) {
        this.player = player;
    }

    private void registerSyncValues(PanelSyncManager syncManager) {
        dynamicSyncHandler = new DynamicSyncHandler();
        dynamicSyncHandler.widgetProvider(this::getStackInfoWidget);
        syncManager.syncValue("inventoryUnlocked",
                SyncHandlers.bool(this::isInventoryUnlocked, this::setInventoryUnlocked));
        syncManager.syncValue("selectedSlot", SyncHandlers.intNumber(this::getSelectedSlot, this::setSelectedSlot));
    }

    @Override
    public ModularPanel<?> buildUI(GuiData data, PanelSyncManager syncManager, UISettings settings) {
        registerSyncValues(syncManager);
        return new ModularPanel<>("modularItem")
                .leftRel(.2f)
                .child(GTMuiWidgets.createTitleBar(IDrawable.EMPTY.asIcon(), "Modules", 176, GTGuiTextures.BACKGROUND))
                .child(playerInventory())
                .child(new ToggleButton()
                        .syncHandler("inventoryUnlocked")
                        .overlay(true, GTGuiTextures.BUTTON_LOCK)
                        .overlay(false, GTGuiTextures.BUTTON_LOCK)
                        .invertSelected(true)
                        .left(7).bottom(86))
                .child(new DynamicSyncedWidget<>()
                        .syncHandler(dynamicSyncHandler)
                        .initialChild(new TextWidget<>("Select an item"))
                        .widthRel(1)
                        .height(85)
                        .top(5)
                        .padding(5));
    }

    private IWidget getStackInfoWidget(PanelSyncManager psm, FriendlyByteBuf buf) {
        ItemStack stack = getSelectedItem();
        IModularItem modularItem = GTCapabilityHelper.getModularItem(stack);
        List<ItemModuleSlot> slots = modularItem == null ? List.of() : modularItem.getSlots();
        return Flow.col()
                .coverChildren()
                .child(Flow.row()
                        .coverChildren()
                        .childPadding(5)
                        .child(new ItemDisplayWidget().item(stack))
                        .child(new TextWidget<>(IKey.dynamic(stack::getHoverName))))
                .childIf(modularItem != null, () -> Flow.row()
                        .child(Flow.col()
                                .coverChildren()
                                .children(slots.size() / 2 + slots.size() % 2, index -> {
                                    AppliedItemModule module = modularItem.getModuleInSlot(index);
                                    return Flow.row()
                                            .coverChildren()
                                            .childIf(module == null,
                                                    () -> new TextWidget<>(
                                                            IKey.lang("metaarmor.tooltip.modifier.empty")))
                                            .childIf(module != null,
                                                    () -> new ItemDrawable(module.getModuleItem()).asWidget())
                                            .childIf(module != null, () -> new TextWidget<>(
                                                    IKey.dynamic(() -> module.getModuleItem().getHoverName())));
                                }))
                        .child(Flow.col()
                                .coverChildren()
                                .children(slots.size() / 2, index -> {
                                    AppliedItemModule module = modularItem
                                            .getModuleInSlot(slots.size() / 2 + slots.size() % 2 + index);
                                    return Flow.row()
                                            .coverChildren()
                                            .childIf(module == null,
                                                    () -> new TextWidget<>(
                                                            IKey.lang("metaarmor.tooltip.modifier.empty")))
                                            .childIf(module != null,
                                                    () -> new ItemDrawable(module.getModuleItem()).asWidget())
                                            .childIf(module != null, () -> new TextWidget<>(
                                                    IKey.dynamic(() -> module.getModuleItem().getHoverName())));
                                })));
    }

    private ItemStack getSelectedItem() {
        return player.getInventory().getItem(selectedSlot);
    }

    @Override
    public ModularScreen createScreen(GuiData data, ModularPanel<?> mainPanel) {
        return new GTGuiScreen(mainPanel);
    }

    private class InventorySlot extends ItemSlot {

        @Override
        public @NotNull Result onMousePressed(double mouseX, double mouseY, int button) {
            if (inventoryUnlocked)
                return super.onMousePressed(mouseX, mouseY, button);
            selectedSlot = this.getSlot().getSlotIndex();
            dynamicSyncHandler.notifyUpdate(buf -> {});
            return Result.SUCCESS;
        }

        @Override
        public boolean onMouseReleased(double mouseX, double mouseY, int button) {
            if (inventoryUnlocked)
                return super.onMouseReleased(mouseX, mouseY, button);
            return false;
        }

        @Override
        protected void drawOverlay(ModularGuiContext context) {
            if (inventoryUnlocked)
                super.drawOverlay(context);
            if (this.getSlot().getSlotIndex() == selectedSlot) {
                GuiDraw.drawBorder(context.getGraphics(), 0, 0, 17, 17, 0xFFFFFF00, 1);
            }
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            if (!inventoryUnlocked && !isHovering() && this.getSlot().getSlotIndex() != selectedSlot) {
                GuiDraw.drawRect(context.getGraphics(), 1, 1, 15, 15, 0x88444444);
            }
            super.draw(context, widgetTheme);
        }
    }

    private SlotGroupWidget playerInventory() {
        SlotGroupWidget slotGroupWidget = new SlotGroupWidget();
        slotGroupWidget.coverChildren();
        slotGroupWidget.name("player_inventory");
        String key = "player";
        for (int i = 0; i < 9; i++) {
            slotGroupWidget.child(new InventorySlot()
                    .syncHandler(key, i)
                    .pos(i * 18, 3 * 18 + 4)
                    .name("slot_" + i));
        }
        for (int i = 0; i < 27; i++) {
            slotGroupWidget.child(new InventorySlot()
                    .syncHandler(key, i + 9)
                    .pos(i % 9 * 18, i / 9 * 18)
                    .name("slot_" + (i + 9)));
        }
        return slotGroupWidget.bottom(7).leftRel(.5f);
    }
}
