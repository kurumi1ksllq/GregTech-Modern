package com.gregtechceu.gtceu.api.item.armor.modifier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AppliedArmorModifier {

    @Getter
    private final @NotNull ArmorModifier modifier;

    private final Consumer<ItemStack> modifierItemSetter;
    private final Consumer<CompoundTag> tagSetter;

    private final Supplier<ItemStack> modifierItemGetter;
    private final Supplier<CompoundTag> tagGetter;

    public AppliedArmorModifier(
                                @NotNull ArmorModifier modifier,
                                @NotNull Consumer<ItemStack> modifierItemSetter,
                                @NotNull Consumer<CompoundTag> tagSetter,
                                @NotNull Supplier<ItemStack> modifierItemGetter,
                                @NotNull Supplier<CompoundTag> tagGetter) {
        this.modifier = modifier;
        this.modifierItemSetter = modifierItemSetter;
        this.modifierItemGetter = modifierItemGetter;
        this.tagGetter = tagGetter;
        this.tagSetter = tagSetter;
    }

    public void setModifierItem(@NotNull ItemStack modifierItem) {
        modifierItemSetter.accept(modifierItem);
    }

    public ItemStack getModifierItem() {
        return modifierItemGetter.get();
    }

    public CompoundTag getTag() {
        return tagGetter.get();
    }

    public void setTag(@NotNull CompoundTag tag) {
        tagSetter.accept(tag);
    }
}
