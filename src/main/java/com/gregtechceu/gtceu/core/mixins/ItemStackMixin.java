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

import java.util.Objects;

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
        // noinspection ConstantValue
        if (getItem() instanceof ISpoilableItem spoilable && spoilable.shouldSpoil((ItemStack) (Object) this)) {
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
            @SuppressWarnings("DataFlowIssue")
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
        CompoundTag tag = getTagElement("GTCEu_spoilable");
        if (tag == null) return 0;
        return tag.getLong("creation_tick");
    }

    @Override
    @Unique
    public long gtceu$getRemainingTicks(@Nullable Level level) {
        gtceu$updateFreshness(level, false);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (level == null && server != null) level = server.overworld();
        if (level != null && getTagElement("GTCEu_spoilable") != null && getItem() instanceof ISpoilableItem spoilable)
            return spoilable.getSpoilTicks((ItemStack) (Object) this) - level.getGameTime() +
                    gtceu$getCreationTick(level);
        if (getItem() instanceof ISpoilableItem spoilable) return spoilable.getSpoilTicks((ItemStack) (Object) this);
        return 0;
    }

    @Inject(at = @At("HEAD"), method = "isSameItemSameTags", cancellable = true)
    private static void mergeSpoilables(ItemStack stack, ItemStack other, CallbackInfoReturnable<Boolean> cir) {
        CompoundTag tag1 = stack.getTagElement("GTCEu_spoilable");
        CompoundTag tag2 = other.getTagElement("GTCEu_spoilable");
        boolean isSameItem = ItemStack.isSameItem(stack, other) && stack.areCapsCompatible(other);
        if (tag1 != null && tag2 != null) {
            long tick1 = tag1.getLong("creation_tick");
            long tick2 = tag2.getLong("creation_tick");
            if (tick1 != tick2) {
                long avg = (tick1 * stack.getCount() + tick2 * other.getCount()) /
                        (stack.getCount() + other.getCount());
                tag1.putLong("creation_tick", avg);
                tag2.putLong("creation_tick", avg);
            }
        }
        cir.setReturnValue(isSameItem && Objects.equals(tag1, tag2));
        cir.cancel();
    }
}
