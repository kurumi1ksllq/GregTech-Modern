package com.gregtechceu.gtceu.integration.ae2.gui.widget;

import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.base.BaseUIComponent;
import com.gregtechceu.gtceu.api.ui.component.TextBoxComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot;

import net.minecraft.network.FriendlyByteBuf;

import appeng.api.stacks.GenericStack;
import lombok.Getter;

import static com.lowdragmc.lowdraglib.gui.util.DrawerHelper.drawStringSized;

/**
 * @author GlodBlock
 * @implNote The amount set widget for config slot
 * @date 2023/4/21-21:20
 */
public class AmountSetComponent extends BaseUIComponent {

    private int index = -1;
    @Getter
    private final TextBoxComponent amountText;
    private final ConfigComponent parentWidget;

    public AmountSetComponent(int x, int y, ConfigComponent widget) {
        positioning(Positioning.absolute(x, y));
        sizing(Sizing.fixed(80), Sizing.fixed(30));
        this.parentWidget = widget;
        this.amountText = UIComponents.textBox(Sizing.fixed(65))
                .textSupplier(this::getAmountStr)
                .numbersOnly(0, Integer.MAX_VALUE)
                .configure(c -> {
                    c.onChanged().subscribe(this::setNewAmount);
                    c.verticalSizing(Sizing.fixed(13))
                            .positioning(Positioning.absolute(x + 3, y + 12));
                    c.setMaxLength(10);
                });
    }

    public void setSlotIndex(int slotIndex) {
        this.index = slotIndex;
        //sendMessage(0, buf -> buf.writeVarInt(this.index));
    }

    public String getAmountStr() {
        if (this.index < 0) {
            return "0";
        }
        IConfigurableSlot slot = this.parentWidget.getConfig(this.index);
        if (slot.getConfig() != null) {
            return String.valueOf(slot.getConfig().amount());
        }
        return "0";
    }

    public void setNewAmount(String amount) {
        try {
            long newAmount = Long.parseLong(amount);
            if (this.index < 0) {
                return;
            }
            IConfigurableSlot slot = this.parentWidget.getConfig(this.index);
            if (newAmount > 0 && slot.getConfig() != null) {
                slot.setConfig(new GenericStack(slot.getConfig().what(), newAmount));
            }
        } catch (NumberFormatException ignore) {}
    }

    /*
    @Override
    public void receiveMessage(int id, FriendlyByteBuf buf) {
        super.receiveMessage(id, buf);
        if (id == 0) {
            this.index = buf.readVarInt();
        }
    }
    */

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        GuiTextures.BACKGROUND.draw(graphics, mouseX, mouseY, x(), y(), 80, 30);
        // FIXME MAKE TRANSLATABLE
        drawStringSized(graphics, "Amount", x() + 3, y() + 3, 0x404040, false, 1f, false);
        GuiTextures.DISPLAY.draw(graphics, mouseX, mouseY, x() + 3, y() + 11, 65, 14);
    }
}
