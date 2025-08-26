package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.item.CoverPlaceBehavior;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import lombok.Getter;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiCover extends CoverBehavior implements IUICover {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MultiCover.class,
            CoverBehavior.MANAGED_FIELD_HOLDER);

    private static final int SLOTS_ROOT = 2;

    @Persisted
    @Getter
    private final IItemHandlerModifiable itemHandler = new CustomItemStackHandler(SLOTS_ROOT * SLOTS_ROOT);
    @Persisted
    @Getter
    private final CoverBehavior[] covers = new CoverBehavior[SLOTS_ROOT * SLOTS_ROOT];

    public MultiCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public void updateCovers() {
        for (int i = 0; i < SLOTS_ROOT; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.getItem() instanceof IComponentItem componentItem) {
                for (IItemComponent component : componentItem.getComponents()) {
                    if (component instanceof CoverPlaceBehavior coverPlaceBehavior) {
                        if (covers[i] != null) {
                            if (covers[i].coverDefinition == coverPlaceBehavior.coverDefinition()) continue;
                            covers[i].onRemoved();
                        }
                        covers[i] = coverPlaceBehavior.coverDefinition().createCoverBehavior(coverHolder, attachedSide);
                        covers[i].onAttached(stack, null);
                    }
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        for (CoverBehavior cover : covers) if (cover != null) cover.onLoad();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        for (CoverBehavior cover : covers) if (cover != null) cover.onUnload();
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        for (CoverBehavior cover : covers) if (cover != null) cover.onRemoved();
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup slotGroup = new WidgetGroup();
        for (int i = 0; i < SLOTS_ROOT; i++) {
            for (int j = 0; j < SLOTS_ROOT; j++) {
                SlotWidget slot = new SlotWidget(itemHandler, 4 * i + j, j * 20, i * 20);
                slot.setChangeListener(this::updateCovers);
                slotGroup.addWidget(slot);
            }
        }
        return slotGroup;
    }
}
