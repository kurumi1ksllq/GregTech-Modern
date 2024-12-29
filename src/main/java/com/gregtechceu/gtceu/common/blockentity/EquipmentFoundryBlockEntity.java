package com.gregtechceu.gtceu.common.blockentity;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.gui.widget.EquipmentFoundryBaseWidget;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.custom.PlayerInventoryWidget;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoPersistBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IManagedBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IRPCBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class EquipmentFoundryBlockEntity extends BlockEntity implements IAsyncAutoSyncBlockEntity, IRPCBlockEntity,
        IAutoPersistBlockEntity, IManaged, IManagedBlockEntity, IUIHolder {

    public static final int MAX_MODIFIER_SLOTS = 10;

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(EquipmentFoundryBlockEntity.class);
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted @DescSynced
    private final CustomItemStackHandler equipmentSlot;
    @Persisted @DescSynced
    private final CustomItemStackHandler modifierSlots;

    public EquipmentFoundryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.equipmentSlot = new CustomItemStackHandler(1);
        // TODO remove instanceof once datagen can run
        this.equipmentSlot.setFilter(stack -> stack.is(CustomTags.MODIFIABLE_EQUIPMENT) || stack.getItem() instanceof ArmorComponentItem);
        this.modifierSlots = new CustomItemStackHandler(MAX_MODIFIER_SLOTS);
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        ModularUI modularUI = new ModularUI(176, 166, this, entityPlayer);
        modularUI.background(GuiTextures.BACKGROUND);
        modularUI.widget(new LabelWidget(5, 5, getBlockState().getBlock().getDescriptionId()));
        modularUI.widget(new EquipmentFoundryBaseWidget(5, 10, 176, 166,
                equipmentSlot, modifierSlots));
        modularUI.widget(UITemplate.bindPlayerInventory(entityPlayer.getInventory(), GuiTextures.SLOT, 7, 84, true));
        return modularUI;
    }

    @Override
    public boolean isInvalid() {
        return isRemoved();
    }

    @Override
    public boolean isRemote() {
        return getLevel().isClientSide;
    }

    @Override
    public void markAsDirty() {
        setChanged();
    }

    @Override
    public IManagedStorage getRootStorage() {
        return syncStorage;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onChanged() {
        this.setChanged();
    }

}
