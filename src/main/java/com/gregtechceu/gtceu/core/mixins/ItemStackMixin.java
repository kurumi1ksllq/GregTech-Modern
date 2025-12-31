package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.item.ISpoilableItemStackExtension;
import com.gregtechceu.gtceu.api.item.component.*;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

import org.jetbrains.annotations.NotNull;
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
public abstract class ItemStackMixin implements ISpoilableItemStackExtension {

    // ***************************//
    // Shadow fields and methods //
    // ***************************//

    @Shadow
    public abstract CompoundTag getOrCreateTagElement(String key);

    @Shadow
    public abstract Item getItem();

    @Shadow
    @Mutable
    @Final
    @Nullable
    private Item item;

    @Shadow(remap = false)
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

    @Shadow(remap = false)
    protected abstract void forgeInit();

    // ***************************//
    // Unique fields //
    // ***************************//

    /**
     * Whether {@link ItemStackMixin#gtceu$updateFreshness(SpoilContext, boolean)}
     * was called and did not return yet.
     * <br>
     * Used to prevent stack overflows.
     */
    @Unique
    private boolean gtceu$isUpdating = false;

    /**
     * The value of the last non-empty (determined by {@link SpoilContext#isEmpty()})
     * context passed into {@link ItemStackMixin#gtceu$updateFreshness(SpoilContext, boolean)}.
     * <br>
     * Used to as an argument for {@link ISpoilableItem#spoilResult(SpoilContext, boolean)}
     * when this stack spoils.
     */
    @Unique
    private @Nullable SpoilContext gtceu$lastSpoilContext = new SpoilContext();

    /**
     * Whether to display a fake "spoils into" tooltip for an item if it's not spoilable.
     * <br>
     * Has a 10% chance of being {@code true} for any stack on april fools.
     * The chance is rolled once in the constructor (injected by {@link ItemStackMixin#gtceu$injectFakeTooltipInit}).
     */
    @Unique
    private boolean gtceu$fakeTooltip;

    @Unique
    private ItemStack gtceu$self() {
        return (ItemStack) (Object) this;
    }

    // ***************************//
    // Interface implementations //
    // ***************************//

    /**
     * Primarily used as debug info when advanced tooltips are turned on.
     * 
     * @return the last known context of this stack
     */
    @Unique
    @Override
    public SpoilContext gtceu$getSpoilContext() {
        return gtceu$lastSpoilContext;
    }

    /**
     * Checks if this item should've already spoiled, and calls
     * {@link ISpoilableItem#spoilResult(SpoilContext, boolean)}
     * with {@link ItemStackMixin#gtceu$lastSpoilContext}
     * and replaces this item with its return value if so.<br>
     * Also sets {@link ItemStackMixin#gtceu$lastSpoilContext} to the provided
     * context if it is non-empty (determined by {@link SpoilContext#isEmpty()}).<br>
     * <br>
     * If {@code createTag = true} and the spoilage tag did not exist, creates
     * the tag and sets the creation tick to this tick.<br>
     * If {@code createTag = false} and the spoilage tag isn't present, does nothing.
     *
     * @param createTag Whether to create a spoilage tag if it wasn't present.
     *                  Usually {@code true} for stacks that are present in-world,
     *                  and {@code false} for stacks in XEI, icons, quests, etc.
     *
     * @implNote This method is injected into all of {@link ItemStack}'s getters to
     *           be called with an empty {@link SpoilContext} and {@code createTag = false}.
     */
    @Unique
    @Override
    public void gtceu$updateFreshness(@NotNull SpoilContext spoilContext, boolean createTag) {
        if (gtceu$isUpdating) return;
        gtceu$isUpdating = true;
        if (!spoilContext.isEmpty()) gtceu$lastSpoilContext = spoilContext;
        Level level = SpoilContext.getDefaultLevel();
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(gtceu$self());
        if (spoilable != null && spoilable.shouldSpoil()) {
            if (spoilable.getSpoilTicks() < 0) {
                gtceu$isUpdating = false;
                return;
            }
            CompoundTag tag = createTag ? getOrCreateTagElement("GTCEu_spoilable") : getTagElement("GTCEu_spoilable");
            if (tag == null || tag.contains("frozenRemainingTicks")) {
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
            long spoilTicks = spoilable.getSpoilTicks();
            long timeDifference = level.getGameTime() - tag.getLong("creation_tick") - spoilTicks;
            if (timeDifference >= 0) {
                ItemStack newStack = spoilable.spoilResult(gtceu$lastSpoilContext, false);
                item = newStack.getItem();
                delegate = ForgeRegistries.ITEMS.getDelegateOrThrow(item);
                count = newStack.getCount();
                this.tag = newStack.getTag();
                forgeInit();
                ISpoilableItem newSpoilable = GTCapabilityHelper.getSpoilable(gtceu$self());
                if (newSpoilable != null && (this.tag == null || !this.tag.contains("GTCEu_spoilable"))) {
                    getOrCreateTagElement("GTCEu_spoilable").putLong("creation_tick",
                            level.getGameTime() - timeDifference);
                    try {
                        gtceu$isUpdating = false;
                        gtceu$updateFreshness(spoilContext, false);
                    } catch (StackOverflowError ignored) {
                        // if items spoil in a giant chain or a loop
                    }
                }
            }
        }
        gtceu$isUpdating = false;
    }

    // ***************************//
    // Injectors //
    // ***************************//

    @Inject(at = @At("HEAD"),
            method = { "getItem", "getCount", "getTag", "getOrCreateTag", "getTagElement", "getOrCreateTagElement",
                    "getItemHolder" })
    private void gtceu$injectedFreshnessUpdate(CallbackInfoReturnable<Item> cir) {
        if (entityRepresentation != null)
            gtceu$updateFreshness(new SpoilContext(entityRepresentation), true);
        else gtceu$updateFreshness(new SpoilContext(), false);
    }

    @Inject(at = @At("HEAD"), method = "inventoryTick")
    private void gtceu$tickFreshness(Level level, Entity entity, int inventorySlot, boolean isCurrentItem,
                                     CallbackInfo ci) {
        if (entity instanceof Player player) gtceu$updateFreshness(new SpoilContext(player, inventorySlot), true);
        else gtceu$updateFreshness(new SpoilContext(entity), true);
    }

    @Inject(at = @At("HEAD"), method = "onCraftedBy")
    private void gtceu$updateFreshnessOnCraft(Level level, Player player, int amount, CallbackInfo ci) {
        gtceu$updateFreshness(new SpoilContext(player, -1), true);
    }

    /**
     * Since {@link ItemStack#isSameItemSameTags(ItemStack, ItemStack)} is commonly called
     * right before merging two stacks, this method averages their spoil progress (or, more
     * accurately, their {@link ISpoilableItem#getCreationTick()}). If {@link SpoilUtils#FROZEN_EQUALITY}
     * is {@code true}, this method will ignore the frozen/not frozen status of stacks when determining its
     * return value. Other than that, the return value is equal to the normal {@link ItemStack#isSameItemSameTags}.
     * <br>
     * This method replaces {@link ItemStack#isSameItemSameTags(ItemStack, ItemStack)}.
     *
     * @implNote This implementation may lead to spoil progress averaging in situations other
     *           than stack merging, though I don't think this will lead to any big user-facing bugs.
     */
    @Inject(at = @At("HEAD"), method = "isSameItemSameTags", cancellable = true)
    private static void gtceu$mergeSpoilables(ItemStack stack, ItemStack other, CallbackInfoReturnable<Boolean> cir) {
        ISpoilableItem spoilable1 = GTCapabilityHelper.getSpoilable(stack);
        ISpoilableItem spoilable2 = GTCapabilityHelper.getSpoilable(stack);
        boolean isSameItem = ItemStack.isSameItem(stack, other) && stack.areCapsCompatible(other);
        CompoundTag modifiedTag1 = stack.getTag() == null ? null : stack.getTag().copy();
        CompoundTag modifiedTag2 = other.getTag() == null ? null : other.getTag().copy();
        if (modifiedTag1 != null) modifiedTag1.remove("GTCEu_spoilable");
        if (modifiedTag2 != null) modifiedTag2.remove("GTCEu_spoilable");
        isSameItem = isSameItem && Objects.equals(modifiedTag1, modifiedTag2);
        if (isSameItem && spoilable1 != null && spoilable2 != null) {
            if (spoilable1.isFrozen() || spoilable2.isFrozen()) {
                if (!SpoilUtils.FROZEN_EQUALITY &&
                        (spoilable1.isFrozen() ^ spoilable2.isFrozen())) {
                    cir.setReturnValue(false);
                    return;
                }
                cir.setReturnValue(spoilable1.getTicksUntilSpoiled() == spoilable2.getTicksUntilSpoiled());
                return;
            } else {
                long tick1 = spoilable1.getCreationTick();
                long tick2 = spoilable2.getCreationTick();
                if (tick1 != tick2) {
                    long avg;
                    if (stack.getCount() + other.getCount() > 0)
                        avg = (tick1 * stack.getCount() + tick2 * other.getCount()) /
                                (stack.getCount() + other.getCount());
                    else avg = tick1;
                    spoilable1.setCreationTick(avg);
                    spoilable2.setCreationTick(avg);
                }
            }
        }
        cir.setReturnValue(isSameItem && Objects.equals(stack.getTag(), other.getTag()));
    }

    @Inject(at = @At("RETURN"),
            method = "<init>(Lnet/minecraft/world/level/ItemLike;ILnet/minecraft/nbt/CompoundTag;)V")
    private void gtceu$injectFakeTooltipInit(ItemLike item, int count, CompoundTag tag, CallbackInfo ci) {
        gtceu$fakeTooltip = GTValues.FOOLS.getAsBoolean() && GTValues.RNG.nextFloat() < .1f;
    }

    /**
     * Allows {@link ISpoilableItem} subclasses that implement {@link IAddInformation} to
     * actually display the added tooltip.
     */
    @Inject(at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V"),
            method = "getTooltipLines",
            locals = LocalCapture.CAPTURE_FAILSOFT)
    private void gtceu$spoilageTooltip(Player player, TooltipFlag isAdvanced,
                                       CallbackInfoReturnable<List<Component>> cir,
                                       List<Component> list) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(gtceu$self());
        if (!(getItem() instanceof ISpoilableItem) && spoilable instanceof IAddInformation addInformation) {
            addInformation.appendHoverText(gtceu$self(), player == null ? null : player.level(), list,
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

    /**
     * Allows {@link ISpoilableItem} subclasses that implement {@link IDurabilityBar} to
     * actually display the bar.
     */
    @Inject(at = @At("HEAD"), method = "isBarVisible", cancellable = true)
    private void gtceu$spoilageBarVisible(CallbackInfoReturnable<Boolean> cir) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(gtceu$self());
        if (!(getItem() instanceof ISpoilableItem) && spoilable instanceof IDurabilityBar durabilityBar) {
            cir.setReturnValue(durabilityBar.isBarVisible(gtceu$self()));
        } else if (gtceu$fakeTooltip) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Allows {@link ISpoilableItem} subclasses that implement {@link IDurabilityBar} to
     * actually display the bar.
     */
    @Inject(at = @At("HEAD"), method = "getBarColor", cancellable = true)
    private void gtceu$spoilageBarColor(CallbackInfoReturnable<Integer> cir) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(gtceu$self());
        if (!(getItem() instanceof ISpoilableItem) && spoilable instanceof IDurabilityBar durabilityBar) {
            cir.setReturnValue(durabilityBar.getBarColor(gtceu$self()));
        } else if (gtceu$fakeTooltip) {
            cir.setReturnValue(FastColor.ARGB32.color(255, 255, 255, 255));
        }
    }

    /**
     * Allows {@link ISpoilableItem} subclasses that implement {@link IDurabilityBar} to
     * actually display the bar.
     */
    @Inject(at = @At("HEAD"), method = "getBarWidth", cancellable = true)
    private void gtceu$spoilageBarWidth(CallbackInfoReturnable<Integer> cir) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(gtceu$self());
        if (!(getItem() instanceof ISpoilableItem) && spoilable instanceof IDurabilityBar durabilityBar) {
            cir.setReturnValue(durabilityBar.getBarWidth(gtceu$self()));
        } else if (gtceu$fakeTooltip) {
            cir.setReturnValue(13);
        }
    }
}
