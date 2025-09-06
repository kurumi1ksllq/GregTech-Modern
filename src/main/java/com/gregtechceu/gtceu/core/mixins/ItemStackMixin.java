package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.item.ISpoilableItemStack;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ISpoilableItemStack {

    @Unique
    private boolean isUpdating = false;

    @Shadow
    public abstract CompoundTag getOrCreateTagElement(String key);

    @Shadow
    public abstract Item getItem();

    @Shadow
    @Deprecated
    @Nullable
    private Item item;

    @Shadow
    @Nullable
    private Holder.Reference<Item> delegate;

    @Shadow
    private int count;

    @Shadow
    @Nullable
    private CompoundTag tag;

    @Shadow
    public abstract boolean isEmpty();

    @Unique
    @Override
    public void gtceu$updateFreshness(Level level) {
        if (isUpdating) return;
        isUpdating = true;
        if (getItem() instanceof ISpoilableItem spoilable) {
            if (spoilable.getSpoilTicks((ItemStack) (Object) this) < 0) {
                isUpdating = false;
                return;
            }
            CompoundTag tag = getOrCreateTagElement("GTCEu_spoilable");
            if (!tag.contains("creation_tick")) {
                if (level == null) {
                    isUpdating = false;
                    return;
                }
                tag.putLong("creation_tick", level.getGameTime());
            }
            long spoilTicks = spoilable.getSpoilTicks((ItemStack) (Object) this);
            if (spoilTicks >= tag.getLong("creation_tick")) {
                @SuppressWarnings("DataFlowIssue")
                ItemStack newStack = spoilable.spoilResult((ItemStack) (Object) this);
                item = newStack.getItem();
                delegate = ForgeRegistries.ITEMS.getDelegateOrThrow(item);
                count = newStack.getCount();
                this.tag = newStack.getTag();
            }
        }
        isUpdating = false;
    }

    @Inject(at = @At("HEAD"),
            method = { "getItem", "getCount", "getTag", "getOrCreateTag", "getTagElement", "getOrCreateTagElement",
                    "getItemHolder" })
    private void injectedFreshnessUpdate(CallbackInfoReturnable<Item> cir) {
        gtceu$updateFreshness(null);
    }

    @Inject(at = @At("HEAD"), method = "use")
    private void useFreshnessUpdate(Level level, Player player, InteractionHand usedHand,
                                    CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        gtceu$updateFreshness(level);
    }

    @Override
    @Unique
    public long gtceu$getCreationTick(@Nullable Level level) {
        gtceu$updateFreshness(level);
        return getOrCreateTagElement("GTCEu_spoilable").getLong("creation_tick");
    }
}
