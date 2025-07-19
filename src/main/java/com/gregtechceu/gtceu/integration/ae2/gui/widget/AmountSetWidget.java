package com.gregtechceu.gtceu.integration.ae2.gui.widget;

import com.gregtechceu.gtceu.api.gui.widget.LongInputWidget;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.api.stacks.GenericStack;
import lombok.Getter;

public class AmountSetWidget extends Widget {

    private int index = -1;
    @Getter
    private final LongInputWidget amountText;
    private final ConfigWidget parentWidget;

    public AmountSetWidget(int x, int y, ConfigWidget widget) {
        super(x, y, 80, 30);
        this.parentWidget = widget;
        this.amountText = new LongInputWidget(x, y, 105, 11, this::getAmount, this::setAmount);
        amountText.setMin(0L);
    }

    @OnlyIn(Dist.CLIENT)
    public void setSlotIndexClient(int slotIndex) {
        this.index = slotIndex;
        writeClientAction(0, buf -> buf.writeVarInt(this.index));
    }

    public void setSlotIndex(int slotIndex) {
        this.index = slotIndex;
    }

    public long getAmount() {
        if (index < 0) {
            return 0;
        }
        IConfigurableSlot slot = this.parentWidget.getConfig(this.index);
        if (slot.getConfig() != null) {
            return slot.getConfig().amount();
        }
        return 0;
    }

    public void setAmount(long amount) {
        if (this.index < 0) {
            return;
        }
        IConfigurableSlot slot = this.parentWidget.getConfig(this.index);
        if (amount > 0 && slot.getConfig() != null) {
            slot.setConfig(new GenericStack(slot.getConfig().what(), amount));
        }
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        super.handleClientAction(id, buffer);
        if (id == 0) {
            this.index = buffer.readVarInt();
        }
    }
}
