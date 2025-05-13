package com.gregtechceu.gtceu.common.mui.widgets.slot;

import com.gregtechceu.gtceu.api.mui.ModularUI;
import com.gregtechceu.gtceu.api.mui.integration.jei.JeiGhostIngredientSlot;
import com.gregtechceu.gtceu.api.mui.integration.jei.ModularUIJeiPlugin;
import com.gregtechceu.gtceu.api.mui.utils.MouseData;
import com.gregtechceu.gtceu.api.mui.value.sync.PhantomItemSlotSH;
import com.gregtechceu.gtceu.api.mui.value.sync.SyncHandler;
import net.minecraft.client.renderer.RenderSystem;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PhantomItemSlot extends ItemSlot implements JeiGhostIngredientSlot<ItemStack> {

    private PhantomItemSlotSH syncHandler;

    @Override
    public void onInit() {
        super.onInit();
        getContext().getJeiSettings().addJeiGhostIngredientSlot(this);
    }

    @Override
    public boolean isValidSyncHandler(SyncHandler syncHandler) {
        this.syncHandler = castIfTypeElseNull(syncHandler, PhantomItemSlotSH.class);
        return this.syncHandler != null && super.isValidSyncHandler(syncHandler);
    }

    @Override
    protected void drawOverlay() {
        if (ModularUI.isJeiLoaded() && (ModularUIJeiPlugin.hasDraggingGhostIngredient() || ModularUIJeiPlugin.hoveringOverIngredient(this))) {
            RenderSystem.colorMask(true, true, true, false);
            drawHighlight(getArea(), isHovering());
            RenderSystem.colorMask(true, true, true, true);
        } else {
            super.drawOverlay();
        }
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        MouseData mouseData = MouseData.create(mouseButton);
        this.syncHandler.syncToServer(PhantomItemSlotSH.SYNC_CLICK, mouseData::writeToPacket);
        return Result.SUCCESS;
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        return true;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double delta) {
        MouseData mouseData = MouseData.create(mouseX.modifier);
        this.syncHandler.syncToServer(PhantomItemSlotSH.SYNC_SCROLL, mouseData::writeToPacket);
        return true;
    }

    @Override
    public void onMouseDrag(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // TODO custom drag impl
    }

    @Override
    public void setGhostIngredient(@NotNull ItemStack ingredient) {
        this.syncHandler.updateFromClient(ingredient);
    }

    @Override
    public @Nullable ItemStack castGhostIngredientIfValid(@NotNull Object ingredient) {
        return areAncestorsEnabled() &&
                this.syncHandler.isPhantom() &&
                ingredient instanceof ItemStack itemStack &&
                this.syncHandler.isItemValid(itemStack) ? itemStack : null;
    }

    @Override
    @NotNull
    public PhantomItemSlotSH getSyncHandler() {
        if (this.syncHandler == null) {
            throw new IllegalStateException("Widget is not initialised!");
        }
        return syncHandler;
    }

    @Override
    public PhantomItemSlot slot(ModularSlot slot) {
        slot.slotNumber = -1;
        this.syncHandler = new PhantomItemSlotSH(slot);
        super.isValidSyncHandler(this.syncHandler);
        setSyncHandler(this.syncHandler);
        return this;
    }

    @Override
    public boolean handleAsVanillaSlot() {
        return false;
    }
}
