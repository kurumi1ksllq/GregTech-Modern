package com.gregtechceu.gtceu.common.blockentity;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.widget.BlockableSlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.item.armor.ArmorUtils;
import com.gregtechceu.gtceu.api.item.armor.modifier.AppliedArmorModifier;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.recipe.type.EquipmentFoundryRecipe;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
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

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.RecipeWrapper;

import lombok.Getter;

import java.util.List;

public class EquipmentFoundryBlockEntity extends BlockEntity implements IAsyncAutoSyncBlockEntity, IRPCBlockEntity,
                                         IAutoPersistBlockEntity, IManaged, IManagedBlockEntity, IUIHolder {

    public static final int MAX_MODIFIER_SLOTS = 10;

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            EquipmentFoundryBlockEntity.class);
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    private final CustomItemStackHandler equipmentSlot;
    @Persisted
    @DescSynced
    private final CustomItemStackHandler modifierSlots;

    public EquipmentFoundryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.equipmentSlot = new CustomItemStackHandler(1);
        // TODO remove instanceof once datagen can run
        this.equipmentSlot.setFilter(stack -> stack.is(CustomTags.MODIFIABLE_EQUIPMENT));

        this.modifierSlots = new CustomItemStackHandler(MAX_MODIFIER_SLOTS) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };

        this.modifierSlots.setFilter(stack -> {
            if (this.getLevel() == null) {
                return false;
            }
            NonNullList<ItemStack> stacks = NonNullList.create();
            stacks.add(this.equipmentSlot.getStackInSlot(0));
            stacks.add(stack);
            RecipeWrapper newWrapper = new RecipeWrapper(new CustomItemStackHandler(stacks));

            return getLevel().getRecipeManager()
                    .getRecipeFor(GTRecipeTypes.EQUIPMENT_FOUNDRY_RECIPES.get(), newWrapper, this.getLevel())
                    .isPresent();
        });
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        ModularUI modularUI = new ModularUI(176, 166, this, entityPlayer);
        modularUI.background(GuiTextures.BACKGROUND.copy().setColor(0xff69645f));

        IGuiTexture slotTexture = GuiTextures.SLOT.copy().setColor(0xff69645f);

        TextTexture titleText = new TextTexture(getBlockState().getBlock().getDescriptionId())
                .setColor(0xffffff)
                .setDropShadow(false)
                .setType(TextTexture.TextType.ROLL)
                .setWidth(105);
        titleText.setRollSpeed(0.7f);
        modularUI.widget(new WidgetGroup(9, -16, 160, 16)
                .addWidget(new ImageWidget(
                        16, 2, 105, 16, titleText))
                .setBackground(GuiTextures.TITLE_BAR_BACKGROUND.copy().setColor(0xff69645f)));
        modularUI.widget(new ImageWidget(4, 4, 168, 75, GuiTextures.EQUIPMENT_FOUNDRY_BACKGROUND));

        modularUI.widget(new SlotWidget(equipmentSlot, 0, 14, 32)
                .setChangeListener(() -> this.onEquipmentSlotChanged(entityPlayer))
                .setBackgroundTexture(null));

        int x = 42;
        int y = 13;
        for (int i = 0; i < MAX_MODIFIER_SLOTS; i++) {
            final int finalI = i;
            modularUI.widget(new BlockableSlotWidget(modifierSlots, i, x, y)
                    .setIsBlocked(() -> isModifierSlotBlocked(finalI))
                    .setChangeListener(this::onModifierSlotChanged)
                    .setBackgroundTexture(null));
            x += 26;
            if (i == 4) {
                x = 42;
                y = 52;
            }
        }
        modularUI.widget(UITemplate.bindPlayerInventory(entityPlayer.getInventory(), slotTexture, 7, 84, true));
        return modularUI;
    }

    public boolean isModifierSlotBlocked(int slot) {
        ItemStack equipment = equipmentSlot.getStackInSlot(0);
        List<AppliedArmorModifier> modifiers = ArmorUtils.getModifiers(equipment);
        if (modifiers.size() > slot) return !modifiers.get(slot).getModifier().canRemove();
        return ArmorUtils.getMaxModifiers(equipment) <= slot;
    }

    public void onEquipmentSlotChanged(Player player) {
        ItemStack stack = equipmentSlot.getStackInSlot(0);
        if (stack.isEmpty()) {
            for (int i = 0; i < modifierSlots.getSlots(); i++) {
                ItemStack out = modifierSlots.extractItem(i, Integer.MAX_VALUE, true);
                if (out.isEmpty()) {
                    continue;
                }
                out = modifierSlots.extractItem(i, Integer.MAX_VALUE, false);
                out.shrink(1);
                if (!player.getInventory().add(out)) {
                    player.drop(out, true);
                }
            }
        } else {
            List<AppliedArmorModifier> modifiers = ArmorUtils.getModifiers(stack);
            for (int i = 0; i < modifiers.size() && i < modifierSlots.getSlots(); i++) {
                if (modifiers.get(i) != null) modifierSlots.insertItem(i, modifiers.get(i).getModifierItem(), false);
            }
        }
    }

    public void onModifierSlotChanged() {
        if (getLevel() == null || getLevel().isClientSide) {
            return;
        }

        ItemStack stack = equipmentSlot.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }

        ArmorUtils.clearModifiers(stack);
        for (int i = 0; i < modifierSlots.getSlots(); i++) {
            ItemStack modifier = modifierSlots.getStackInSlot(i);
            if (modifier.isEmpty()) {
                continue;
            }

            CustomItemStackHandler handler = new CustomItemStackHandler(modifier);
            RecipeWrapper recipeWrapper = new RecipeWrapper(
                    new CombinedInvWrapper(this.equipmentSlot, handler));

            var maybeRecipe = getLevel().getRecipeManager()
                    .getRecipeFor(GTRecipeTypes.EQUIPMENT_FOUNDRY_RECIPES.get(), recipeWrapper, this.getLevel());
            if (maybeRecipe.isPresent()) {
                EquipmentFoundryRecipe recipe = maybeRecipe.get();
                ItemStack newStack = recipe.assemble(recipeWrapper, getLevel().registryAccess());
                if (newStack.isEmpty()) {
                    continue;
                }
                equipmentSlot.setStackInSlot(0, newStack);
            }
        }
    }

    @Override
    public boolean isInvalid() {
        return isRemoved();
    }

    @Override
    public boolean isRemote() {
        return getLevel() != null && getLevel().isClientSide;
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
