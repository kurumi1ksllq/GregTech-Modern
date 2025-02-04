package com.gregtechceu.gtceu.integration.ae2.gui.widget;

import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEConfigSlotComponent;
import com.gregtechceu.gtceu.integration.ae2.slot.IConfigurableSlot;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.api.stacks.GenericStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

public abstract class ConfigComponent extends FlowLayout {

    protected final IConfigurableSlot[] config;
    protected IConfigurableSlot[] cached;
    protected Int2ObjectMap<IConfigurableSlot> changeMap = new Int2ObjectOpenHashMap<>();
    protected IConfigurableSlot[] displayList;
    protected AmountSetComponent amountSetComponent;
    protected final static int UPDATE_ID = 1000;

    @Getter
    protected final boolean isStocking;

    public ConfigComponent(IConfigurableSlot[] config, boolean isStocking) {
        super(Sizing.fixed(config.length / 2 * 18), Sizing.fixed(18 * 4 + 2), Algorithm.HORIZONTAL);
        this.isStocking = isStocking;
        this.config = config;
        this.init();
        this.amountSetComponent = new AmountSetComponent(31, -50, this);
        this.child(this.amountSetComponent);
        this.child(this.amountSetComponent.getAmountText());
        this.amountSetComponent.enabled(false);
        this.amountSetComponent.getAmountText().setVisible(false);
    }

    public void enableAmount(int slotIndex) {
        this.amountSetComponent.setSlotIndex(slotIndex);
        this.amountSetComponent.enabled(true);
        this.amountSetComponent.getAmountText().setVisible(true);
    }

    public void disableAmount() {
        this.amountSetComponent.setSlotIndex(-1);
        this.amountSetComponent.enabled(false);
        this.amountSetComponent.getAmountText().setVisible(false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (this.amountSetComponent.enabled()) {
            if (this.amountSetComponent.getAmountText().mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        for (UIComponent w : this.children()) {
            if (w instanceof AEConfigSlotComponent slot) {
                slot.setSelect(false);
            }
        }
        this.disableAmount();
        return super.onMouseDown(mouseX, mouseY, button);
    }

    @Override
    public abstract void init();

    public abstract boolean hasStackInConfig(GenericStack stack);

    public abstract boolean isAutoPull();

    @Override
    protected void parentUpdate(float delta, int mouseX, int mouseY) {
        super.parentUpdate(delta, mouseX, mouseY);
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
                // lmao??
                this.containerAccess().screen().getMenu().broadcastChanges();
            }
        }
        // FIXME
        /*
         * if (!this.changeMap.isEmpty()) {
         * this.sendMessage(UPDATE_ID, buf -> {
         * buf.writeVarInt(this.changeMap.size());
         * for (int index : this.changeMap.keySet()) {
         * GenericStack sConfig = this.changeMap.get(index).getConfig();
         * GenericStack sStock = this.changeMap.get(index).getStock();
         * buf.writeVarInt(index);
         * if (sConfig != null) {
         * buf.writeBoolean(true);
         * GenericStack.writeBuffer(sConfig, buf);
         * } else {
         * buf.writeBoolean(false);
         * }
         * if (sStock != null) {
         * buf.writeBoolean(true);
         * GenericStack.writeBuffer(sStock, buf);
         * } else {
         * buf.writeBoolean(false);
         * }
         * }
         * });
         * }
         */
    }

    // FIXME
    /*
     * @OnlyIn(Dist.CLIENT)
     * 
     * @Override
     * public void receiveMessage(int id, FriendlyByteBuf buffer) {
     * super.receiveMessage(id, buffer);
     * if (id == UPDATE_ID) {
     * int size = buffer.readVarInt();
     * for (int i = 0; i < size; i++) {
     * int index = buffer.readVarInt();
     * IConfigurableSlot slot = this.displayList[index];
     * if (buffer.readBoolean()) {
     * slot.setConfig(GenericStack.readBuffer(buffer));
     * } else {
     * slot.setConfig(null);
     * }
     * if (buffer.readBoolean()) {
     * slot.setStock(GenericStack.readBuffer(buffer));
     * } else {
     * slot.setStock(null);
     * }
     * }
     * }
     * }
     */

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
