package com.gregtechceu.gtceu.integration.ae2.gui.widget.slot;

import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.ingredient.GhostIngredientSlot;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.ConfigComponent;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.util.TextFormattingUtil;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import org.jetbrains.annotations.NotNull;

import static com.lowdragmc.lowdraglib.gui.util.DrawerHelper.drawItemStack;
import static com.lowdragmc.lowdraglib.gui.util.DrawerHelper.drawStringFixedCorner;

/**
 * @author GlodBlock
 * @apiNote A configurable slot for {@link ItemStack}
 * @date 2023/4/22-0:48
 */
public class AEItemConfigSlotComponent extends AEConfigSlotComponent implements GhostIngredientSlot<ItemStack> {

    public AEItemConfigSlotComponent(ConfigComponent widget, int index) {
        super(widget, index);
        this.sizing(Sizing.fixed(18), Sizing.fixed(18 * 2));
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        super.draw(graphics, mouseX, mouseY, partialTicks, delta);
        IConfigurableSlot slot = this.parentWidget.getDisplay(this.index);
        GenericStack config = slot.getConfig();
        GenericStack stock = slot.getStock();
        drawSlots(graphics, mouseX, mouseY, x(), y(), parentWidget.isAutoPull());
        if (this.select) {
            GuiTextures.SELECT_BOX.draw(graphics, mouseX, mouseY, x(), y(), 18, 18);
        }
        int stackX = x() + 1;
        int stackY = y() + 1;
        if (config != null) {
            ItemStack stack = config.what() instanceof AEItemKey key ? new ItemStack(key.getItem()) : ItemStack.EMPTY;
            drawItemStack(graphics, stack, stackX, stackY, 0xFFFFFFFF, null);

            if (!parentWidget.isStocking()) {
                String amountStr = TextFormattingUtil.formatLongToCompactString(config.amount(), 4);
                drawStringFixedCorner(graphics, amountStr, stackX + 17, stackY + 17, 16777215, true, 0.5f);
            }
        }
        if (stock != null) {
            ItemStack stack = stock.what() instanceof AEItemKey key ? new ItemStack(key.getItem()) : ItemStack.EMPTY;
            drawItemStack(graphics, stack, stackX, stackY + 18, 0xFFFFFFFF, null);
            String amountStr = TextFormattingUtil.formatLongToCompactString(stock.amount(), 4);
            drawStringFixedCorner(graphics, amountStr, stackX + 17, stackY + 18 + 17, 16777215, true, 0.5f);
        }
        if (mouseOverConfig(mouseX, mouseY)) {
            drawSelectionOverlay(graphics, stackX, stackY, 16, 16);
        } else if (mouseOverStock(mouseX, mouseY)) {
            drawSelectionOverlay(graphics, stackX, stackY + 18, 16, 16);
        }
    }

    private void drawSlots(UIGuiGraphics graphics, int mouseX, int mouseY, int x, int y, boolean autoPull) {
        if (autoPull) {
            GuiTextures.SLOT_DARK.draw(graphics, mouseX, mouseY, x, y, 18, 18);
            GuiTextures.CONFIG_ARROW.draw(graphics, mouseX, mouseY, x, y, 18, 18);
        } else {
            GuiTextures.SLOT.draw(graphics, mouseX, mouseY, x, y, 18, 18);
            GuiTextures.CONFIG_ARROW_DARK.draw(graphics, mouseX, mouseY, x, y, 18, 18);
        }
        GuiTextures.SLOT_DARK.draw(graphics, mouseX, mouseY, x, y + 18, 18, 18);
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (mouseOverConfig(mouseX, mouseY)) {
            // don't allow manual interaction with config slots when auto pull is enabled
            if (parentWidget.isAutoPull()) {
                return false;
            }

            if (button == 1) {
                // Right click to clear
                // sendMessage(REMOVE_ID, buf -> {});

                if (!parentWidget.isStocking()) {
                    this.parentWidget.disableAmount();
                }
            } else if (button == 0) {
                // Left click to set/select
                ItemStack item = getCarried();

                if (!item.isEmpty()) {
                    /// sendMessage(UPDATE_ID, buf -> buf.writeItem(item));
                }

                if (!parentWidget.isStocking()) {
                    this.parentWidget.enableAmount(this.index);
                    this.select = true;
                }
            }
            return true;
        } else if (mouseOverStock(mouseX, mouseY)) {
            // Left click to pick up
            if (button == 0) {
                if (parentWidget.isStocking()) {
                    return false;
                }
                GenericStack stack = this.parentWidget.getDisplay(this.index).getStock();
                if (stack != null) {
                    // sendMessage(PICK_UP_ID, buf -> {});
                }
                return true;
            }
        }
        return false;
    }

    /*
     * @Override
     * public void receiveMessage(int id, FriendlyByteBuf buffer) {
     * super.receiveMessage(id, buffer);
     * IConfigurableSlot slot = this.parentWidget.getConfig(this.index);
     * if (id == REMOVE_ID) {
     * slot.setConfig(null);
     * this.parentWidget.disableAmount();
     * // sendMessage(REMOVE_ID, buf -> {});
     * }
     * if (id == UPDATE_ID) {
     * ItemStack item = buffer.readItem();
     * var stack = GenericStack.fromItemStack(item);
     * if (!isStackValidForSlot(stack)) return;
     * slot.setConfig(stack);
     * this.parentWidget.enableAmount(this.index);
     * if (!item.isEmpty()) {
     * // sendMessage(UPDATE_ID, buf -> buf.writeItem(item));
     * }
     * }
     * if (id == AMOUNT_CHANGE_ID) {
     * if (slot.getConfig() != null) {
     * long amt = buffer.readVarLong();
     * slot.setConfig(new GenericStack(slot.getConfig().what(), amt));
     * // sendMessage(AMOUNT_CHANGE_ID, buf -> buf.writeVarLong(amt));
     * }
     * }
     * if (id == PICK_UP_ID) {
     * if (slot.getStock() != null && getCarried() == ItemStack.EMPTY &&
     * slot.getStock().what() instanceof AEItemKey key) {
     * ItemStack stack = new ItemStack(key.getItem());
     * stack.setCount(Math.min((int) slot.getStock().amount(), stack.getMaxStackSize()));
     * if (key.hasTag()) {
     * stack.setTag(key.getTag().copy());
     * }
     * setCarried(stack);
     * GenericStack stack1 = ExportOnlyAESlot.copy(slot.getStock(),
     * Math.max(0, (slot.getStock().amount() - stack.getCount())));
     * slot.setStock(stack1.amount() == 0 ? null : stack1);
     * // sendMessage(PICK_UP_ID, buf -> {});
     * }
     * }
     * }
     */

    @Override
    public Rect2i area() {
        Rect2i rectangle = super.area();
        rectangle.setHeight(rectangle.getHeight() / 2);
        return rectangle;
    }

    @Override
    public void setGhostIngredient(@NotNull ItemStack ingredient) {
        // sendMessage(UPDATE_ID, buf -> buf.writeItem(ingredient));
    }

    @Override
    public Class<ItemStack> ghostIngredientClass() {
        return ItemStack.class;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double wheelDelta) {
        // Only allow the amount scrolling if not stocking, as amount is useless for stocking
        if (parentWidget.isStocking()) return false;
        IConfigurableSlot slot = this.parentWidget.getDisplay(this.index);
        Rect2i rectangle = area();
        rectangle.setHeight(rectangle.getHeight() / 2);
        if (slot.getConfig() == null || wheelDelta == 0 || !rectangle.contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        GenericStack stack = slot.getConfig();
        long amt;
        if (GTUtil.isCtrlDown()) {
            amt = wheelDelta > 0 ? stack.amount() * 2L : stack.amount() / 2L;
        } else {
            amt = wheelDelta > 0 ? stack.amount() + 1L : stack.amount() - 1L;
        }
        if (amt > 0 && amt < Integer.MAX_VALUE + 1L) {
            // sendMessage(AMOUNT_CHANGE_ID, buf -> buf.writeVarLong(amt));
            return true;
        }
        return false;
    }
}
