package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.item.ISpoilableItemStackExtension;
import com.gregtechceu.gtceu.api.item.component.*;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class is a basic implementation of the {@link ISpoilableItem} capability,
 * to be attached to an item in an {@link AttachCapabilitiesEvent<ItemStack>} listener.
 * It leaves some methods unimplemented, such as {@link ISpoilableItem#getSpoilTicks()} and
 * {@link ISpoilableItem#spoilResult(SpoilContext, boolean)}.
 *
 * @implNote this class uses a mixin in its {@link ISpoilableItem#updateFreshness} implementation
 *
 * @see SpoilableBehaviour
 * @see SpoilableBehaviour#toCapProvider(ItemStack)
 */
public abstract class SpoilableItemStack implements ISpoilableItem, IAddInformation, IDurabilityBar {

    public static final String SPOILABLE_KEY = "GTCEu_spoilable";
    public static final String FROZEN_TICKS_KEY = "frozenRemainingTicks";
    public static final String CREATION_TICK_KEY = "creationTick";
    /**
     * Consider frozen and non-frozen spoilables equal. This is done to allow filtering by ticks remaining until
     * spoiled.<br>
     * If you want the player to have frozen stacks in their inventory, set this to {@code false} to prevent players
     * from
     * entirely bypassing the spoilage system.
     */
    public static boolean FROZEN_EQUALITY = true;
    @Getter
    private final ItemStack stack;

    private final Item originalItem;

    public SpoilableItemStack(ItemStack stack) {
        this.stack = stack;
        this.originalItem = stack.getItem();
    }

    public SpoilContext getSpoilContext() {
        CompoundTag tag = stack.getTagElement(SPOILABLE_KEY);
        if (tag == null || !tag.contains("spoilContext")) return new SpoilContext();
        return SpoilContext.deserializeNBT(tag.getCompound("spoilContext"));
    }

    private void setSpoilContext(SpoilContext ctx) {
        CompoundTag tag = stack.getTagElement(SPOILABLE_KEY);
        if (tag != null) tag.put("spoilContext", ctx.serializeNBT());
    }

    /**
     * Checks if this item should've already spoiled, and calls
     * {@link ISpoilableItem#spoilResult(SpoilContext, boolean)}
     * with {@link SpoilableItemStack#getSpoilContext()}
     * and replaces this item with its return value if so.<br>
     * Also sets the {@link SpoilContext} stored in the mixin to the provided
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
    public void updateFreshness(SpoilContext spoilContext, boolean createTag) {
        if (!stack.is(originalItem)) return;
        CompoundTag tag = createTag ? stack.getOrCreateTagElement(SPOILABLE_KEY) :
                stack.getTagElement(SPOILABLE_KEY);
        if (!spoilContext.isEmpty()) setSpoilContext(spoilContext);
        Level level = SpoilContext.getDefaultLevel();
        if (level != null && this.shouldSpoil()) {
            if (tag == null || tag.contains(FROZEN_TICKS_KEY)) {
                return;
            }
            if (!tag.contains(CREATION_TICK_KEY)) {
                tag.putLong(CREATION_TICK_KEY, level.getGameTime());
            }
            long spoilTicks = this.getSpoilTicks();
            long timeDifference = level.getGameTime() - tag.getLong(CREATION_TICK_KEY) - spoilTicks;
            if (timeDifference >= 0) {
                ItemStack newStack = this.spoilResult(getSpoilContext(), GTCEu.isClientThread());
                ((ISpoilableItemStackExtension) (Object) stack).gtceu$setStack(newStack);
                onItemChanged();
                ISpoilableItem newSpoilable = GTCapabilityHelper.getSpoilable(stack);
                if (newSpoilable != null) {
                    newSpoilable.setCreationTick(level.getGameTime() - timeDifference);
                    try {
                        newSpoilable.updateFreshness(spoilContext, false);
                    } catch (StackOverflowError ignored) {
                        // if items spoil in a giant chain or a loop
                    }
                }
            }
        }
    }

    @Override
    public long getCreationTick() {
        CompoundTag tag = stack.getTagElement(SPOILABLE_KEY);
        if (tag == null) return 0;
        return tag.getLong(CREATION_TICK_KEY);
    }

    @Override
    public void setCreationTick(long tick) {
        CompoundTag tag = stack.getOrCreateTagElement(SPOILABLE_KEY);
        tag.putLong(CREATION_TICK_KEY, tick);
    }

    @Override
    public long getTicksUntilSpoiled() {
        updateFreshness(new SpoilContext(), false);
        Level level = SpoilContext.getDefaultLevel();
        CompoundTag spoilTag = stack.getTagElement(SPOILABLE_KEY);
        if (level != null && spoilTag != null) {
            if (spoilTag.contains(FROZEN_TICKS_KEY)) return spoilTag.getLong(FROZEN_TICKS_KEY);
            return this.getSpoilTicks() - level.getGameTime() +
                    this.getCreationTick();
        }
        return this.getSpoilTicks();
    }

    @Override
    public void setTicksUntilSpoiled(long value) {
        updateFreshness(new SpoilContext(), false);
        Level level = SpoilContext.getDefaultLevel();
        if (level != null && stack.getTagElement(SPOILABLE_KEY) != null)
            setCreationTick(level.getGameTime() - this.getSpoilTicks() + value);
    }

    private void setFreezeSpoiling(boolean freeze) {
        if (freeze) {
            updateFreshness(new SpoilContext(), true);
            stack.getOrCreateTagElement(SPOILABLE_KEY).putLong(FROZEN_TICKS_KEY, getTicksUntilSpoiled());
        } else {
            CompoundTag spoilTag = stack.getTagElement(SPOILABLE_KEY);
            if (spoilTag != null && spoilTag.contains(FROZEN_TICKS_KEY)) {
                setTicksUntilSpoiled(spoilTag.getLong(FROZEN_TICKS_KEY));
                spoilTag.remove(FROZEN_TICKS_KEY);
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
        CompoundTag tag = stack.getTagElement(SPOILABLE_KEY);
        return tag != null && tag.contains(FROZEN_TICKS_KEY);
    }

    @Override
    public boolean shouldSpoil() {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.translatable(
                "gtceu.tooltip.spoil_time_remaining",
                Component.literal(FormattingUtil.formatTime(getTicksUntilSpoiled()))
                        .withStyle(ChatFormatting.DARK_AQUA)));
        tooltipComponents.add(Component.translatable(
                "gtceu.tooltip.spoils_into", getSpoilResultTooltip()));
        if (isAdvanced.isAdvanced()) {
            tooltipComponents.add(Component.translatable(
                    "gtceu.tooltip.spoil_time_total",
                    Component.literal(FormattingUtil.formatTime(getSpoilTicks()))
                            .withStyle(ChatFormatting.GREEN)));
            tooltipComponents.add(Component.translatable(
                    "gtceu.tooltip.creation_tick",
                    getCreationTick()));
            SpoilContext ctx = getSpoilContext();
            if (ctx.level() != null && ctx.pos() != null)
                tooltipComponents.add(Component.translatable("gtceu.tooltip.location",
                        ctx.level().dimensionTypeId().location().toString(),
                        ctx.pos().getX(), ctx.pos().getY(), ctx.pos().getZ()));
            if (ctx.entity() != null) tooltipComponents.add(Component.translatable("gtceu.tooltip.location_entity",
                    ctx.entity().getType().getDescription()));
            if (ctx.itemHandlerSource() != null) tooltipComponents
                    .add(Component.translatable("gtceu.tooltip.item_handler_source", ctx.itemHandlerSource()));
            if (ctx.itemHandlerData() != null)
                tooltipComponents.add(Component.translatable("gtceu.tooltip.item_handler_data", ctx.itemHandlerData()));
            if (ctx.slot() != -1)
                tooltipComponents.add(Component.translatable("gtceu.tooltip.location_slot", ctx.slot()));
        }
    }

    protected Component getSpoilResultTooltip() {
        return spoilResult(new SpoilContext(), false).getDisplayName();
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return FastColor.ARGB32.color(255, 255, 255, 255);
    }

    @Override
    public boolean doDamagedStateColors(ItemStack itemStack) {
        return false;
    }

    @Override
    public @Nullable IntIntPair getDurabilityColorsForDisplay(ItemStack itemStack) {
        return IntIntPair.of(getBarColor(itemStack), getBarColor(itemStack));
    }

    @Override
    public float getDurabilityForDisplay(ItemStack stack) {
        return (float) getTicksUntilSpoiled() / getSpoilTicks();
    }

    /**
     * Since {@link ItemStack#isSameItemSameTags(ItemStack, ItemStack)} is commonly called
     * right before merging two stacks, this method averages their spoil progress (or, more
     * accurately, their {@link ISpoilableItem#getCreationTick()}). If {@link SpoilableItemStack#FROZEN_EQUALITY}
     * is {@code true}, this method will ignore the frozen/not frozen status of stacks when determining its
     * return value. Other than that, the return value is equal to the normal {@link ItemStack#isSameItemSameTags}.
     *
     * @implNote This implementation may lead to spoil progress averaging in situations other
     *           than stack merging, though I don't think this will lead to any big user-facing bugs.
     */
    @Override
    public Optional<Boolean> isEqualTo(ItemStack other) {
        ISpoilableItem spoilable1 = this;
        ISpoilableItem spoilable2 = GTCapabilityHelper.getSpoilable(other);
        if (spoilable2 != null && !(spoilable2 instanceof SpoilableItemStack)) return spoilable2.isEqualTo(stack);
        boolean isSameItem = ItemStack.isSameItem(stack, other) && stack.areCapsCompatible(other);
        CompoundTag modifiedTag1 = stack.getTag() == null ? null : stack.getTag().copy();
        CompoundTag modifiedTag2 = other.getTag() == null ? null : other.getTag().copy();
        if (modifiedTag1 != null) modifiedTag1.remove(SPOILABLE_KEY);
        if (modifiedTag2 != null) modifiedTag2.remove(SPOILABLE_KEY);
        isSameItem = isSameItem && Objects.equals(modifiedTag1, modifiedTag2);
        if (isSameItem && spoilable2 != null) {
            if (spoilable1.isFrozen() || spoilable2.isFrozen()) {
                if (!FROZEN_EQUALITY && (spoilable1.isFrozen() ^ spoilable2.isFrozen())) {
                    return Optional.of(false);
                }
                return Optional.of(spoilable1.getTicksUntilSpoiled() == spoilable2.getTicksUntilSpoiled());
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
        return Optional.empty();
    }

    /**
     * Called when the stack has spoiled and transformed into a different item.
     */
    abstract protected void onItemChanged();
}
