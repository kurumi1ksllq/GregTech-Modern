package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.item.ISpoilableItemStackMixin;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;

public abstract class SpoilableItemStack implements ISpoilableItem {
    @Getter
    private final ItemStack stack;

    public SpoilableItemStack(ItemStack stack) {
        this.stack = stack;
    }

    private ISpoilableItemStackMixin mixin() {
        return (ISpoilableItemStackMixin) (Object) stack;
    }

    public void updateFreshness(boolean createTag) {
        mixin().gtceu$updateFreshness(null, createTag);
    }

    @Override
    public long getCreationTick() {
        CompoundTag tag = stack.getTagElement("GTCEu_spoilable");
        if (tag == null) return 0;
        return tag.getLong("creation_tick");
    }

    @Override
    public void setCreationTick(long tick) {
        CompoundTag tag = stack.getTagElement("GTCEu_spoilable");
        if (tag == null) return;
        tag.putLong("creation_tick", tick);
    }

    @Override
    public long getTicksUntilSpoiled() {
        updateFreshness(false);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Level level = null;
        if (server != null) level = server.overworld();
        CompoundTag spoilTag = stack.getTagElement("GTCEu_spoilable");
        if (level != null && spoilTag != null) {
            if (spoilTag.contains("frozenRemainingTicks")) return spoilTag.getLong("frozenRemainingTicks");
            return this.getSpoilTicks() - level.getGameTime() +
                    this.getCreationTick();
        }
        return this.getSpoilTicks();
    }

    @Override
    public void setTicksUntilSpoiled(long value) {
        updateFreshness(false);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Level level = null;
        if (server != null) level = server.overworld();
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable((ItemStack) (Object) this);
        if (level != null && stack.getTagElement("GTCEu_spoilable") != null && spoilable != null)
            setCreationTick(level.getGameTime() - spoilable.getSpoilTicks() + value);
    }

    private void setFreezeSpoiling(boolean freeze) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Level level = server == null ? null : server.overworld();
        if (level == null) return;
        if (freeze) {
            updateFreshness(true);
            stack.getOrCreateTagElement("GTCEu_spoilable").putLong("frozenRemainingTicks", getTicksUntilSpoiled());
        } else {
            CompoundTag spoilTag = stack.getTagElement("GTCEu_spoilable");
            if (spoilTag != null && spoilTag.contains("frozenRemainingTicks")) {
                setTicksUntilSpoiled(spoilTag.getLong("frozenRemainingTicks"));
                spoilTag.remove("frozenRemainingTicks");
            }
        }
    }

    @Override
    public void freezeSpoiling() {
        setFreezeSpoiling(true);
    }

    @Override
    public void unfreezeSpoiling() {
        setFreezeSpoiling(false);
    }

    @Override
    public boolean isFrozen() {
        CompoundTag tag = stack.getTagElement("GTCEu_spoilable");
        return tag != null && tag.contains("frozenRemainingTicks");
    }

    @Override
    public boolean shouldSpoil() {
        return true;
    }
}
