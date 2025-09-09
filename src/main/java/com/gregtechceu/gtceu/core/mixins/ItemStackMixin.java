package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.ISpoilableItemStack;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IDurabilityBar;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import com.gregtechceu.gtceu.common.item.SpoilableBehaviour;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ISpoilableItemStack {

    @Unique
    private boolean gtceu$isUpdating = false;

    @Unique
    private boolean gtceu$fakeTooltip;

    @Inject(at = @At("RETURN"),
            method = "<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/nbt/CompoundTag;)V")
    private void injectFakeTooltipInit(ItemLike item, int count, CompoundTag p_41606_, CallbackInfo ci) {
        gtceu$fakeTooltip = GTValues.FOOLS.getAsBoolean() && Math.random() < .1;
    }

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

    @Shadow
    @Nullable
    private Entity entityRepresentation;

    @Unique
    @Override
    public void gtceu$updateFreshness(Level level, boolean createTag) {
        if (gtceu$isUpdating) return;
        gtceu$isUpdating = true;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (level == null && server != null) level = server.overworld();
        ISpoilableItem spoilable = SpoilableBehaviour.getSpoilable((ItemStack) (Object) this);
        if (spoilable != null && spoilable.shouldSpoil((ItemStack) (Object) this)) {
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
        gtceu$updateFreshness(server == null ? null : server.overworld(),
                GTValues.BREAK_EVERYTHING_LOL || entityRepresentation != null);
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
    public void gtceu$setCreationTick(@Nullable Level level, long value) {
        CompoundTag tag = getTagElement("GTCEu_spoilable");
        if (tag == null) return;
        tag.putLong("creation_tick", value);
    }

    @Override
    @Unique
    public long gtceu$getRemainingTicks(@Nullable Level level) {
        gtceu$updateFreshness(level, false);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (level == null && server != null) level = server.overworld();
        ISpoilableItem spoilable = SpoilableBehaviour.getSpoilable((ItemStack) (Object) this);
        if (level != null && getTagElement("GTCEu_spoilable") != null && spoilable != null)
            return spoilable.getSpoilTicks((ItemStack) (Object) this) - level.getGameTime() +
                    gtceu$getCreationTick(level);
        if (spoilable != null) return spoilable.getSpoilTicks((ItemStack) (Object) this);
        return 0;
    }

    @Unique
    @Override
    public void gtceu$setRemainingTicks(@Nullable Level level, long value) {
        gtceu$updateFreshness(level, false);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (level == null && server != null) level = server.overworld();
        ISpoilableItem spoilable = SpoilableBehaviour.getSpoilable((ItemStack) (Object) this);
        if (level != null && getTagElement("GTCEu_spoilable") != null && spoilable != null)
            gtceu$setCreationTick(level,
                    level.getGameTime() - spoilable.getSpoilTicks((ItemStack) (Object) this) + value);
    }

    @Inject(at = @At("HEAD"), method = "isSameItemSameTags", cancellable = true)
    private static void mergeSpoilables(ItemStack stack, ItemStack other, CallbackInfoReturnable<Boolean> cir) {
        CompoundTag tag1 = stack.getTagElement("GTCEu_spoilable");
        CompoundTag tag2 = other.getTagElement("GTCEu_spoilable");
        boolean isSameItem = ItemStack.isSameItem(stack, other) && stack.areCapsCompatible(other);
        CompoundTag modifiedTag1 = stack.getTag() == null ? null : stack.getTag().copy();
        CompoundTag modifiedTag2 = other.getTag() == null ? null : other.getTag().copy();
        if (modifiedTag1 != null) modifiedTag1.remove("GTCEu_spoilable");
        if (modifiedTag2 != null) modifiedTag2.remove("GTCEu_spoilable");
        isSameItem = isSameItem && Objects.equals(modifiedTag1, modifiedTag2);
        if (isSameItem && tag1 != null && tag2 != null) {
            long tick1 = tag1.getLong("creation_tick");
            long tick2 = tag2.getLong("creation_tick");
            if (tick1 != tick2) {
                long avg;
                if (stack.getCount() + other.getCount() > 0)
                    avg = (tick1 * stack.getCount() + tick2 * other.getCount()) /
                            (stack.getCount() + other.getCount());
                else avg = tick1;
                tag1.putLong("creation_tick", avg);
                tag2.putLong("creation_tick", avg);
            }
        }
        cir.setReturnValue(isSameItem && Objects.equals(stack.getTag(), other.getTag()));
        cir.cancel();
    }

    @Inject(at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V"),
            method = "getTooltipLines",
            locals = LocalCapture.CAPTURE_FAILSOFT)
    private void aprilFoolsTooltip(Player player, TooltipFlag isAdvanced, CallbackInfoReturnable<List<Component>> cir,
                                   List<Component> list) {
        ISpoilableItem spoilable = SpoilableBehaviour.getSpoilable((ItemStack) (Object) this);
        if (!(getItem() instanceof ISpoilableItem) && spoilable instanceof IAddInformation addInformation) {
            addInformation.appendHoverText((ItemStack) (Object) this, player == null ? null : player.level(), list,
                    isAdvanced);
        } else if (gtceu$fakeTooltip) {
            list.add(Component.translatable(
                    "gtceu.tooltip.spoil_time_remaining",
                    Component.literal(FormattingUtil.formatTime(100)).withStyle(ChatFormatting.DARK_AQUA)));
            list.add(Component.translatable(
                    "gtceu.tooltip.spoils_into",
                    Items.DIRT.getDefaultInstance().getDisplayName()));
        }
    }

    @Inject(at = @At("HEAD"), method = "isBarVisible", cancellable = true)
    private void aprilFoolsBarVisible(CallbackInfoReturnable<Boolean> cir) {
        ISpoilableItem spoilable = SpoilableBehaviour.getSpoilable((ItemStack) (Object) this);
        if (!(getItem() instanceof ISpoilableItem) && spoilable instanceof IDurabilityBar durabilityBar) {
            cir.setReturnValue(durabilityBar.isBarVisible((ItemStack) (Object) this));
            cir.cancel();
        } else if (gtceu$fakeTooltip) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "getBarColor", cancellable = true)
    private void aprilFoolsBarColor(CallbackInfoReturnable<Integer> cir) {
        ISpoilableItem spoilable = SpoilableBehaviour.getSpoilable((ItemStack) (Object) this);
        if (!(getItem() instanceof ISpoilableItem) && spoilable instanceof IDurabilityBar durabilityBar) {
            cir.setReturnValue(durabilityBar.getBarColor((ItemStack) (Object) this));
            cir.cancel();
        } else if (gtceu$fakeTooltip) {
            cir.setReturnValue(FastColor.ARGB32.color(255, 255, 255, 255));
            cir.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "getBarWidth", cancellable = true)
    private void aprilFoolsBarWidth(CallbackInfoReturnable<Integer> cir) {
        ISpoilableItem spoilable = SpoilableBehaviour.getSpoilable((ItemStack) (Object) this);
        if (!(getItem() instanceof ISpoilableItem) && spoilable instanceof IDurabilityBar durabilityBar) {
            cir.setReturnValue(durabilityBar.getBarWidth((ItemStack) (Object) this));
            cir.cancel();
        } else if (gtceu$fakeTooltip) {
            cir.setReturnValue(13);
            cir.cancel();
        }
    }
}
