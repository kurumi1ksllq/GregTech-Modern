package com.gregtechceu.gtceu.integration.ae2.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEConfigSlotWidget;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot;

import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.api.stacks.GenericStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

import java.awt.*;

public abstract class ConfigWidget extends WidgetGroup {

    protected final IConfigurableSlot[] config;
    protected IConfigurableSlot[] cached;
    protected Int2ObjectMap<IConfigurableSlot> changeMap = new Int2ObjectOpenHashMap<>();
    protected IConfigurableSlot[] displayList;
    protected AmountSetWidget amountSetWidget;
    protected LabelWidget titleWidget;
    protected ImageWidget backgroundImage;
    protected final static int UPDATE_ID = 1000;

    @Getter
    protected final boolean isStocking;

    public ConfigWidget(int x, int y, IConfigurableSlot[] config, boolean isStocking) {
        super(new Position(x, y), new Size(config.length / 2 * 18, 18 * 4 + 2));
        this.isStocking = isStocking;
        this.config = config;
        init();
        var asw = new AmountSetWidget(15, -53, this);
        titleWidget = new LabelWidget(15, -65, "gui.gtceu.ae.amount").setTextColor(0x404040).setDropShadow(false);
        backgroundImage = new ImageWidget(12, -68, 112, 40, GuiTextures.BACKGROUND);
        addWidget(backgroundImage);
        addWidget(titleWidget);
        addWidget(asw);
        addWidget(asw.getAmountText());
        asw.setVisible(false);
        asw.getAmountText().setVisible(false);
        titleWidget.setVisible(false);
        backgroundImage.setVisible(false);
        amountSetWidget = asw;
    }

    @OnlyIn(Dist.CLIENT)
    public void enableAmountClient(int slotIndex) {
        amountSetWidget.setSlotIndexClient(slotIndex);
        amountSetWidget.setVisible(true);
        amountSetWidget.getAmountText().setVisible(true);
        titleWidget.setVisible(true);
        backgroundImage.setVisible(true);
    }

    @OnlyIn(Dist.CLIENT)
    public void disableAmountClient() {
        amountSetWidget.setSlotIndexClient(-1);
        amountSetWidget.setVisible(false);
        amountSetWidget.getAmountText().setVisible(false);
        titleWidget.setVisible(false);
        backgroundImage.setVisible(false);
    }

    public void enableAmount(int slotIndex) {
        amountSetWidget.setSlotIndex(slotIndex);
        amountSetWidget.setVisible(true);
        amountSetWidget.getAmountText().setVisible(true);
        titleWidget.setVisible(true);
        backgroundImage.setVisible(true);
    }

    public void disableAmount() {
        amountSetWidget.setSlotIndex(-1);
        amountSetWidget.setVisible(false);
        amountSetWidget.getAmountText().setVisible(false);
        titleWidget.setVisible(false);
        backgroundImage.setVisible(false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (amountSetWidget.isVisible()) {
            if (amountSetWidget.getAmountText().mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        for (Widget w : this.widgets) {
            if (w instanceof AEConfigSlotWidget slot) {
                slot.setSelect(false);
            }
        }
        this.disableAmountClient();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    abstract void init();

    public abstract boolean hasStackInConfig(GenericStack stack);

    public abstract boolean isAutoPull();

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        this.changeMap.clear();
        for (int index = 0; index < this.config.length; index++) {
            IConfigurableSlot newSlot = this.config[index];
            IConfigurableSlot oldSlot = this.cached[index];
            GenericStack nConfig = newSlot.getConfig();
            GenericStack nStock = newSlot.getStock();
            GenericStack oConfig = oldSlot.getConfig();
            GenericStack oStock = oldSlot.getStock();
            if (!areAEStackCountsEqual(nConfig, oConfig) || !areAEStackCountsEqual(nStock, oStock)) {
                this.changeMap.put(index, newSlot.copy());
                this.cached[index] = this.config[index].copy();
                this.gui.holder.markAsDirty();
            }
        }
        if (!this.changeMap.isEmpty()) {
            this.writeUpdateInfo(UPDATE_ID, buf -> {
                buf.writeVarInt(this.changeMap.size());
                for (int index : this.changeMap.keySet()) {
                    GenericStack sConfig = this.changeMap.get(index).getConfig();
                    GenericStack sStock = this.changeMap.get(index).getStock();
                    buf.writeVarInt(index);
                    if (sConfig != null) {
                        buf.writeBoolean(true);
                        GenericStack.writeBuffer(sConfig, buf);
                    } else {
                        buf.writeBoolean(false);
                    }
                    if (sStock != null) {
                        buf.writeBoolean(true);
                        GenericStack.writeBuffer(sStock, buf);
                    } else {
                        buf.writeBoolean(false);
                    }
                }
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        super.readUpdateInfo(id, buffer);
        if (id == UPDATE_ID) {
            int size = buffer.readVarInt();
            for (int i = 0; i < size; i++) {
                int index = buffer.readVarInt();
                IConfigurableSlot slot = this.displayList[index];
                if (buffer.readBoolean()) {
                    slot.setConfig(GenericStack.readBuffer(buffer));
                } else {
                    slot.setConfig(null);
                }
                if (buffer.readBoolean()) {
                    slot.setStock(GenericStack.readBuffer(buffer));
                } else {
                    slot.setStock(null);
                }
            }
        }
    }

    public final IConfigurableSlot getConfig(int index) {
        return this.config[index];
    }

    public final IConfigurableSlot getDisplay(int index) {
        return this.displayList[index];
    }

    protected final boolean areAEStackCountsEqual(GenericStack s1, GenericStack s2) {
        if (s2 == s1) {
            return true;
        }
        if (s1 != null && s2 != null) {
            return s1.amount() == s2.amount() && s1.what().matches(s2);
        }
        return false;
    }
}
