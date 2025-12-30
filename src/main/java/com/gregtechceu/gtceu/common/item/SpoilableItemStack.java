package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.item.ISpoilableItemStackExtension;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IDurabilityBar;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import com.gregtechceu.gtceu.api.item.component.SpoilContext;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    @Getter
    private final ItemStack stack;

    public SpoilableItemStack(ItemStack stack) {
        this.stack = stack;
    }

    public void updateFreshness(SpoilContext spoilContext, boolean createTag) {
        ((ISpoilableItemStackExtension) (Object) stack).gtceu$updateFreshness(spoilContext, createTag);
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
        updateFreshness(new SpoilContext(), false);
        Level level = SpoilContext.getDefaultLevel();
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
        updateFreshness(new SpoilContext(), false);
        Level level = SpoilContext.getDefaultLevel();
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(stack);
        if (level != null && stack.getTagElement("GTCEu_spoilable") != null && spoilable != null)
            setCreationTick(level.getGameTime() - spoilable.getSpoilTicks() + value);
    }

    private void setFreezeSpoiling(boolean freeze) {
        if (freeze) {
            updateFreshness(new SpoilContext(), true);
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

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(stack);
        if (spoilable == null) return;
        tooltipComponents.add(Component.translatable(
                "gtceu.tooltip.spoil_time_remaining",
                Component.literal(FormattingUtil.formatTime(spoilable.getTicksUntilSpoiled()))
                        .withStyle(ChatFormatting.DARK_AQUA)));
        tooltipComponents.add(Component.translatable(
                "gtceu.tooltip.spoils_into", getSpoilResultTooltip()));
        if (isAdvanced.isAdvanced()) {
            tooltipComponents.add(Component.translatable(
                    "gtceu.tooltip.spoil_time_total",
                    Component.literal(FormattingUtil.formatTime(spoilable.getSpoilTicks()))
                            .withStyle(ChatFormatting.GREEN)));
            tooltipComponents.add(Component.translatable(
                    "gtceu.tooltip.creation_tick",
                    spoilable.getCreationTick()));
            SpoilContext ctx = ((ISpoilableItemStackExtension) (Object) stack).gtceu$getSpoilContext();
            if (ctx.level() != null && ctx.pos() != null)
                tooltipComponents.add(Component.translatable("gtceu.tooltip.location",
                        ctx.level().dimensionTypeId().location().toString(),
                        ctx.pos().getX(), ctx.pos().getY(), ctx.pos().getZ()));
            if (ctx.entity() != null) tooltipComponents.add(Component.translatable("gtceu.tooltip.location_entity",
                    ctx.entity().getType().getDescription()));
            if (ctx.itemHandler() != null)
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
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(stack);
        if (spoilable == null) return 0;
        return (float) spoilable.getTicksUntilSpoiled() / spoilable.getSpoilTicks();
    }
}
