package com.gregtechceu.gtceu.api.item.module;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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

    /**
     * The stack that this module is applied to.
     * If this module is not applied to anything, this field is {@code null}.
     */
    @Getter
    private ItemStack appliedTo;

    private AppliedItemModule(CompoundTag moduleTag, ItemModule module, int slot) {
        this.moduleTag = moduleTag;
        this.moduleTag.put("module", module.serializeNBT());
        this.moduleTag.put("tag", new CompoundTag());
        this.module = module;
        this.tag = this.moduleTag.getCompound("tag");
        this.slot = slot;
    }

    private AppliedItemModule(CompoundTag moduleTag, int slot) {
        this.moduleTag = moduleTag;
        if (!this.moduleTag.contains("module")) {
            GTCEu.LOGGER.warn("Created an AppliedItemModule from an invalid tag, module will be null!");
        }
        this.module = ItemModule.fromNBT(this.moduleTag.getCompound("module"));
        if (!this.moduleTag.contains("tag")) this.moduleTag.put("tag", new CompoundTag());
        this.tag = this.moduleTag.getCompound("tag");
        this.slot = slot;
    }

    public void setModule(ItemModule module) {
        this.moduleTag.put("module", module.serializeNBT());
        this.module = module;
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
        this.module.onInventoryTick(player, this);
    }

    public void armorTick(@NotNull LivingEntity entity) {
        this.module.onArmorTick(entity, this);
    }

    public void equip(@NotNull LivingEntity entity) {
        this.module.onEquip(entity, this);
    }

    public void unequip(@NotNull LivingEntity entity) {
        this.module.onUnequip(entity, this);
    }

    public static AppliedItemModule attach(ItemStack stack, ItemModule module, int slot) {
        CompoundTag modulesTag = stack.getOrCreateTagElement(MODULES_TAG);
        if (!modulesTag.contains(String.valueOf(slot))) modulesTag.put(String.valueOf(slot), new CompoundTag());
        AppliedItemModule appliedModule = new AppliedItemModule(modulesTag.getCompound(String.valueOf(slot)), module,
                slot);
        appliedModule.appliedTo = stack;
        module.onAttach(appliedModule);
        return appliedModule;
    }

    public static @Nullable AppliedItemModule getModuleInSlot(ItemStack stack, int slot) {
        CompoundTag modulesTag = stack.getOrCreateTagElement(MODULES_TAG);
        if (!modulesTag.contains(String.valueOf(slot))) return null;
        return new AppliedItemModule(modulesTag.getCompound(String.valueOf(slot)), slot);
    }

    public static @NotNull List<AppliedItemModule> getAppliedModules(ItemStack stack) {
        CompoundTag modulesTag = stack.getOrCreateTagElement(MODULES_TAG);
        List<AppliedItemModule> modules = new ArrayList<>();
        for (String key : modulesTag.getAllKeys()) {
            modules.add(new AppliedItemModule(modulesTag.getCompound(key), Integer.parseInt(key)));
        }
        return modules;
    }
}
