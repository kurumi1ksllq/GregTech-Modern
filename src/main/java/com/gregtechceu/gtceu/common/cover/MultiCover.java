package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.item.CoverPlaceBehavior;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.*;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiCover extends CoverBehavior implements IUICover {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MultiCover.class,
            CoverBehavior.MANAGED_FIELD_HOLDER);

    public static final int SLOTS_ROOT = 2;

    @Persisted
    @DescSynced
    @UpdateListener(methodName = "updateCovers")
    private final CustomItemStackHandler itemHandler = new CustomItemStackHandler(SLOTS_ROOT * SLOTS_ROOT);

    @Getter
    @Persisted
    @DescSynced
    @LazyManaged
    private final List<CoverBehavior> covers = new ArrayList<>();

    public MultiCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        for (int i = 0; i < SLOTS_ROOT * SLOTS_ROOT; i++) covers.add(null);
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
                        if (covers.get(i) != null) {
                            if (covers.get(i).coverDefinition == coverPlaceBehavior.coverDefinition()) continue;
                            covers.get(i).onRemoved();
                        }
                        covers.set(i,
                                coverPlaceBehavior.coverDefinition().createCoverBehavior(coverHolder, attachedSide));
                        /*
                         * if (coverHolder.getLevel().getBlockEntity(coverHolder.getPos()) instanceof
                         * IMachineBlockEntity machineBE) {
                         * if (machineBE.getRootStorage() != null)
                         * machineBE.getRootStorage().attach(covers.get(i).getSyncStorage());
                         * }
                         */
                        covers.get(i).onAttached(stack, null);
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
                SlotWidget slot = new SlotWidget(itemHandler, SLOTS_ROOT * i + j, j * 20, i * 20);
                slot.setChangeListener(this::updateCovers);
                slotGroup.addWidget(slot);
            }
        }
        return slotGroup;
    }

    private boolean onCoverDirty(@Nullable CoverBehavior coverBehavior) {
        if (coverBehavior != null) {
            for (IRef ref : coverBehavior.getSyncStorage().getNonLazyFields()) {
                ref.update();
            }
            return coverBehavior.getSyncStorage().hasDirtySyncFields() ||
                    coverBehavior.getSyncStorage().hasDirtyPersistedFields();
        }
        return false;
    }

    private CompoundTag serializeCoverUid(CoverBehavior coverBehavior) {
        var uid = new CompoundTag();
        uid.putString("id", GTRegistries.COVERS.getKey(coverBehavior.coverDefinition).toString());
        uid.putInt("side", coverBehavior.attachedSide.ordinal());
        return uid;
    }

    private CoverBehavior deserializeCoverUid(CompoundTag uid) {
        var definitionId = new ResourceLocation(uid.getString("id"));
        var side = GTUtil.DIRECTIONS[uid.getInt("side")];
        var definition = GTRegistries.COVERS.get(definitionId);
        if (definition != null) {
            return definition.createCoverBehavior(coverHolder, side);
        }
        GTCEu.LOGGER.error("couldn't find cover definition {}", definitionId);
        throw new RuntimeException();
    }

    @SuppressWarnings("unused")
    private boolean onCoverListDirty(List<CoverBehavior> coverList) {
        return coverList.stream().anyMatch(this::onCoverDirty);
    }

    @SuppressWarnings("unused")
    private CompoundTag serializeCoverList(List<CoverBehavior> coverList) {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();
        for (CoverBehavior cover : coverList) listTag.add(serializeCoverUid(cover));
        tag.put("list", listTag);
        return tag;
    }

    @SuppressWarnings("unused")
    private List<CoverBehavior> deserializeCoverList(CompoundTag tag) {
        ListTag listTag = tag.getList("list", Tag.TAG_COMPOUND);
        List<CoverBehavior> out = new ArrayList<>();
        for (Tag e : listTag) {
            if (e instanceof CompoundTag compoundTag) out.add(deserializeCoverUid(compoundTag));
        }
        return out;
    }
}
