package com.gregtechceu.gtceu.integration.ae2.gui.widget.list;

import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.ScrollContainer;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;

import net.minecraft.network.FriendlyByteBuf;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.util.ArrayList;
import java.util.List;

/**
 * @author GlodBlock
 * @apiNote A display only widget for {@link KeyStorage}
 * @date 2023/4/19-0:18
 */
public abstract class AEListGridComponent extends ScrollContainer<UIComponent> {

    protected final FlowLayout container;
    protected final KeyStorage list;
    private final int slotAmountY;
    private int slotRowsAmount;
    protected final static int ROW_CHANGE_ID = 2;
    protected final static int CONTENT_CHANGE_ID = 3;

    protected final Object2LongMap<AEKey> changeMap = new Object2LongOpenHashMap<>();
    protected final KeyStorage cached = new KeyStorage();
    protected final List<GenericStack> displayList = new ArrayList<>();

    public AEListGridComponent(int slotsY, KeyStorage internalList) {
        super(ScrollDirection.VERTICAL, Sizing.fixed(18 + 140), Sizing.content(), null);
        container = UIContainers.verticalFlow(Sizing.fill(), Sizing.fill());
        this.child(container);
        this.list = internalList;
        this.slotAmountY = slotsY;
    }

    public GenericStack getAt(int index) {
        return index >= 0 && index < displayList.size() ? displayList.get(index) : null;
    }

    private void addSlotRows(int amount) {
        for (int i = 0; i < amount; i++) {
            int childCount = container.children().size();
            UIComponent component = createDisplayComponent(childCount);
            container.child(component);
        }
    }

    private void removeSlotRows(int amount) {
        for (int i = 0; i < amount; i++) {
            UIComponent slotComponent = container.children().get(container.children().size() - 1);
            container.removeChild(slotComponent);
        }
    }

    private void modifySlotRows(int delta) {
        if (delta > 0) {
            addSlotRows(delta);
        } else {
            removeSlotRows(delta);
        }
    }

    protected void writeListChange(FriendlyByteBuf buffer) {
        this.changeMap.clear();

        // Remove
        var cachedIt = cached.storage.object2LongEntrySet().iterator();
        while (cachedIt.hasNext()) {
            var entry = cachedIt.next();
            var cachedKey = entry.getKey();
            if (!list.storage.containsKey(cachedKey)) {
                this.changeMap.put(cachedKey, -entry.getLongValue());
                cachedIt.remove();
            }
        }

        // Change/Add
        for (var entry : list.storage.object2LongEntrySet()) {
            var key = entry.getKey();
            long value = entry.getLongValue();
            long cacheValue = cached.storage.getOrDefault(key, 0);
            if (cacheValue == 0) {
                // Add
                this.changeMap.put(key, value);
                this.cached.storage.put(key, value);
            } else {
                // Change
                if (cacheValue != value) {
                    this.changeMap.put(key, value - cacheValue);
                    this.cached.storage.put(key, value);
                }
            }
        }

        buffer.writeVarInt(this.changeMap.size());
        for (var entry : this.changeMap.object2LongEntrySet()) {
            entry.getKey().writeToPacket(buffer);
            buffer.writeVarLong(entry.getLongValue());
        }
    }

    protected void readListChange(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            var key = fromPacket(buffer);
            long delta = buffer.readVarLong();

            boolean found = false;
            var li = displayList.listIterator();
            while (li.hasNext()) {
                var stack = li.next();
                if (stack.what().equals(key)) {
                    long newAmount = stack.amount() + delta;
                    if (newAmount > 0) {
                        li.set(new GenericStack(key, newAmount));
                    } else {
                        li.remove();
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                displayList.add(new GenericStack(key, delta));
            }
        }
    }

    protected abstract void toPacket(FriendlyByteBuf buffer, AEKey key);

    protected abstract AEKey fromPacket(FriendlyByteBuf buffer);

    protected abstract UIComponent createDisplayComponent(int index);

    @Override
    protected void parentUpdate(float delta, int mouseX, int mouseY) {
        super.parentUpdate(delta, mouseX, mouseY);
        if (this.list == null) return;
        int slotRowsRequired = Math.max(this.slotAmountY, list.storage.size());
        if (this.slotRowsAmount != slotRowsRequired) {
            int slotsToAdd = slotRowsRequired - this.slotRowsAmount;
            this.slotRowsAmount = slotRowsRequired;
            // this.sendMessage(ROW_CHANGE_ID, buf -> buf.writeVarInt(slotsToAdd));
            this.modifySlotRows(slotsToAdd);
        }
        // this.sendMessage(CONTENT_CHANGE_ID, this::writeListChange);
    }

    // TODO implement
    /*
     * @Override
     * public void receiveMessage(int id, FriendlyByteBuf buffer) {
     * super.receiveMessage(id, buffer);
     * if (id == ROW_CHANGE_ID) {
     * int slotsToAdd = buffer.readVarInt();
     * this.modifySlotRows(slotsToAdd);
     * }
     * if (id == CONTENT_CHANGE_ID) {
     * this.readListChange(buffer);
     * }
     * }
     * 
     * @Override
     * public void writeInitialData(FriendlyByteBuf buffer) {
     * super.writeInitialData(buffer);
     * if (this.list == null) return;
     * int slotRowsRequired = Math.max(this.slotAmountY, list.storage.size());
     * int slotsToAdd = slotRowsRequired - this.slotRowsAmount;
     * this.slotRowsAmount = slotRowsRequired;
     * this.modifySlotRows(slotsToAdd);
     * buffer.writeVarInt(slotsToAdd);
     * this.writeListChange(buffer);
     * }
     * 
     * @Override
     * public void readInitialData(FriendlyByteBuf buffer) {
     * super.readInitialData(buffer);
     * if (this.list == null) return;
     * this.modifySlotRows(buffer.readVarInt());
     * this.readListChange(buffer);
     * }
     */

    public static class Item extends AEListGridComponent {

        public Item(int slotsY, KeyStorage internalList) {
            super(slotsY, internalList);
        }

        @Override
        protected void toPacket(FriendlyByteBuf buffer, AEKey key) {
            key.writeToPacket(buffer);
        }

        @Override
        protected AEKey fromPacket(FriendlyByteBuf buffer) {
            return AEItemKey.fromPacket(buffer);
        }

        @Override
        protected UIComponent createDisplayComponent(int index) {
            return new AEItemDisplayComponent(this, index);
        }
    }

    public static class Fluid extends AEListGridComponent {

        public Fluid(int slotsY, KeyStorage internalList) {
            super(slotsY, internalList);
        }

        @Override
        protected void toPacket(FriendlyByteBuf buffer, AEKey key) {
            key.writeToPacket(buffer);
        }

        @Override
        protected AEKey fromPacket(FriendlyByteBuf buffer) {
            return AEFluidKey.fromPacket(buffer);
        }

        @Override
        protected UIComponent createDisplayComponent(int index) {
            return new AEFluidDisplayComponent(this, index);
        }
    }
}
