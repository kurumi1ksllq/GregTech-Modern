package com.gregtechceu.gtceu.api.item.module;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AppliedItemModule {

    private static final String MODULES_TAG = "Modules";

    private final CompoundTag moduleTag;
    @Getter
    private final int slot;

    @Getter
    private ItemModule module;

    @Getter
    private CompoundTag tag;

    @Getter
    private ItemStack moduleItem;

    /**
     * The stack that this module is applied to.
     * If this module is not applied to anything, this field is {@code null}.
     */
    @Getter
    private ItemStack appliedTo;

    /**
     * Create an applied module and fill the provided tag with module data
     */
    private AppliedItemModule(CompoundTag moduleTag, ItemModule module, int slot) {
        this.moduleTag = moduleTag;
        this.moduleTag.put("module", module.serializeNBT());
        this.moduleTag.put("tag", new CompoundTag());
        this.module = module;
        this.tag = this.moduleTag.getCompound("tag");
        this.slot = slot;
    }

    /**
     * Load an applied module from an existing module tag
     */
    private AppliedItemModule(CompoundTag moduleTag, @Nullable ItemStack appliedTo, int slot) {
        this.moduleTag = moduleTag;
        if (!this.moduleTag.contains("module")) {
            GTCEu.LOGGER.warn("Created an AppliedItemModule from an invalid tag, module will be null!");
        }
        this.module = ItemModule.fromNBT(this.moduleTag.getCompound("module"));
        if (!this.moduleTag.contains("tag")) this.moduleTag.put("tag", new CompoundTag());
        this.tag = this.moduleTag.getCompound("tag");
        this.slot = slot;
        if (this.moduleTag.contains("item")) {
            this.moduleItem = ItemStack.of(this.moduleTag.getCompound("item"));
        }
        this.appliedTo = appliedTo;
    }

    public void setModule(ItemModule module) {
        this.moduleTag.put("module", module.serializeNBT());
        this.module = module;
    }

    public void setModuleItem(ItemStack stack) {
        this.moduleItem = stack;
        this.moduleTag.put("item", stack.serializeNBT());
    }

    public void setTag(CompoundTag tag) {
        this.moduleTag.put("tag", tag);
        this.tag = tag;
    }

    public void detach() {
        if (this.appliedTo == null || !this.module.canRemove(this)) return;
        this.module.onRemove(this);
        this.appliedTo.getOrCreateTagElement(MODULES_TAG).remove(String.valueOf(slot));
        this.appliedTo = null;
    }

    public void inventoryTick(Player player) {
        if (this.isEnabled()) this.module.onInventoryTick(player, this);
        this.module.onTickRaw(this, player, player.level(), null);
    }

    public void armorTick(@NotNull LivingEntity entity) {
        if (this.isEnabled()) this.module.onArmorTick(entity, this);
    }

    public void tick(@NotNull Level level, @Nullable BlockPos pos) {
        this.module.onTickRaw(this, null, level, pos);
    }

    public void equip(@NotNull LivingEntity entity) {
        this.module.onEquip(entity, this);
    }

    public void unequip(@NotNull LivingEntity entity) {
        this.module.onUnequip(entity, this);
    }

    public float changeDamage(LivingEntity entity, float damage, DamageSource source) {
        return this.isEnabled() ? this.module.changeDamage(entity, this, damage, source) : damage;
    }

    public void appendHoverText(Level level, TooltipFlag isAdvanced, List<Component> tooltips) {
        this.module.appendHoverText(level, isAdvanced, tooltips, this);
    }

    public boolean isEnabled() {
        return this.module.isEnabled(this);
    }

    public void setEnabled(boolean enabled) {
        this.module.setEnabled(this, enabled);
    }

    public static AppliedItemModule attach(ItemStack stack, ItemModule module, int slot, boolean simulate) {
        CompoundTag modulesTag = stack.getOrCreateTagElement(MODULES_TAG);
        ItemModuleSlot moduleSlot = ItemModuleSlot.getSlots(stack).get(slot);
        if (moduleSlot == null || !moduleSlot.acceptsModule(module) || !module.canApplyTo(stack)) return null;
        if (!modulesTag.contains(String.valueOf(slot)) && !simulate)
            modulesTag.put(String.valueOf(slot), new CompoundTag());
        AppliedItemModule appliedModule = new AppliedItemModule(
                simulate ? new CompoundTag() : modulesTag.getCompound(String.valueOf(slot)),
                module,
                slot);
        appliedModule.appliedTo = stack;
        if (!simulate) {
            module.onAttach(appliedModule);
        }
        return appliedModule;
    }

    public static @Nullable AppliedItemModule attach(ItemStack stack, ItemModule module, boolean simulate) {
        for (int i = 0; i < ItemModuleSlot.getSlots(stack).size(); i++) {
            if (getModuleInSlot(stack, i) == null) return attach(stack, module, i, simulate);
        }
        return null;
    }

    public static void clearModules(ItemStack stack) {
        stack.removeTagKey(MODULES_TAG);
    }

    public static @Nullable AppliedItemModule getModuleInSlot(ItemStack stack, int slot) {
        CompoundTag modulesTag = stack.getOrCreateTagElement(MODULES_TAG);
        if (!modulesTag.contains(String.valueOf(slot))) return null;
        return new AppliedItemModule(modulesTag.getCompound(String.valueOf(slot)), stack, slot);
    }

    public static @NotNull List<AppliedItemModule> getAppliedModules(ItemStack stack) {
        CompoundTag modulesTag = stack.getOrCreateTagElement(MODULES_TAG);
        List<AppliedItemModule> modules = new ArrayList<>();
        for (String key : modulesTag.getAllKeys()) {
            modules.add(new AppliedItemModule(modulesTag.getCompound(key), stack, Integer.parseInt(key)));
        }
        return modules;
    }

    public static @Nullable AppliedItemModule getModule(ItemStack stack, ItemModule module) {
        return getAppliedModules(stack).stream().filter(appliedModule -> appliedModule.getModule() == module).findAny()
                .orElse(null);
    }

    public boolean isPPE() {
        return this.module.isPPE(this) && this.isEnabled();
    }

    public boolean canRemove() {
        return this.module.canRemove(this);
    }
}
