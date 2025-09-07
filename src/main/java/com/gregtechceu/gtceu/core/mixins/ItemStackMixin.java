package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.item.ISpoilableItemStack;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ISpoilableItemStack {

    @Unique
    private boolean gtceu$isUpdating = false;

    @Shadow
    public abstract CompoundTag getOrCreateTagElement(String key);

    @Shadow
    public abstract Item getItem();

    @Shadow
    @Mutable
    @Final
    @Deprecated
    @Nullable
    private Item item;

    @Shadow
    @Mutable
    @Final
    @Nullable
    private Holder.Reference<Item> delegate;

    @Shadow
    private int count;

    @Shadow
    @Nullable
    private CompoundTag tag;

    @Shadow
    public abstract boolean isEmpty();

    @Shadow
    @Nullable
    public abstract CompoundTag getTagElement(String key);

    @Unique
    @Override
    public void gtceu$updateFreshness(Level level, boolean createTag) {
        if (gtceu$isUpdating) return;
        gtceu$isUpdating = true;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (level == null && server != null) level = server.overworld();
        if (getItem() instanceof ISpoilableItem spoilable) {
            if (spoilable.getSpoilTicks((ItemStack) (Object) this) < 0) {
                gtceu$isUpdating = false;
                return;
            }
            CompoundTag tag = createTag ? getOrCreateTagElement("GTCEu_spoilable") : getTagElement("GTCEu_spoilable");
            if (tag == null) {
                gtceu$isUpdating = false;
                return;
            }
            if (level == null) {
                gtceu$isUpdating = false;
                return;
            }
            if (!tag.contains("creation_tick")) {
                tag.putLong("creation_tick", level.getGameTime());
            }
            long spoilTicks = spoilable.getSpoilTicks((ItemStack) (Object) this);
            if (spoilTicks <= level.getGameTime() - tag.getLong("creation_tick")) {
                @SuppressWarnings("DataFlowIssue")
                ItemStack newStack = spoilable.spoilResult((ItemStack) (Object) this);
                item = newStack.getItem();
                delegate = ForgeRegistries.ITEMS.getDelegateOrThrow(item);
                count = newStack.getCount();
                this.tag = newStack.getTag();
            }
        }
        gtceu$isUpdating = false;
    }

    @Inject(at = @At("HEAD"),
            method = { "getItem", "getCount", "getTag", "getOrCreateTag", "getTagElement", "getOrCreateTagElement",
                    "getItemHolder" })
    private void injectedFreshnessUpdate(CallbackInfoReturnable<Item> cir) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        gtceu$updateFreshness(server == null ? null : server.overworld(), false);
    }

    @Inject(at = @At("HEAD"), method = "inventoryTick")
    private void tickFreshness(Level level, Entity entity, int inventorySlot, boolean isCurrentItem, CallbackInfo ci) {
        gtceu$updateFreshness(null, true);
    }

    @Override
    @Unique
    public long gtceu$getCreationTick(@Nullable Level level) {
        if (getTagElement("GTCEu_spoilable") == null) return 0;
        return getTagElement("GTCEu_spoilable").getLong("creation_tick");
    }
}
