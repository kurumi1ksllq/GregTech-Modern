package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CommonClassTransformers {

    public static class StringTransformer implements IValueTransformer<String> {

        @Override
        public void writeToBuffer(String value, FriendlyByteBuf buf) {
            buf.writeUtf(value);
        }

        @Override
        public String readFromBuffer(FriendlyByteBuf buf, String currentValue) {
            return buf.readUtf();
        }

        @Override
        public Tag serializeNBT(String value) {
            return StringTag.valueOf(value);
        }

        @Override
        public String deserializeNBT(Tag tag, @Nullable String currentVal) {
            return (tag instanceof StringTag stringTag) ? stringTag.getAsString() : "";
        }
    }

    public static class ItemStackTransformer implements IValueTransformer<ItemStack> {

        @Override
        public void writeToBuffer(ItemStack value, FriendlyByteBuf buf) {
            buf.writeItemStack(value, false);
        }

        @Override
        public ItemStack readFromBuffer(FriendlyByteBuf buf, ItemStack currentValue) {
            return buf.readItem();
        }

        @Override
        public Tag serializeNBT(ItemStack value) {
            return value.serializeNBT();
        }

        @Override
        public ItemStack deserializeNBT(Tag tag, @Nullable ItemStack currentVal) {
            return (tag instanceof CompoundTag compoundTag) ? ItemStack.of(compoundTag) : ItemStack.EMPTY;
        }
    }

    public static class FluidStackTransformer implements IValueTransformer<FluidStack> {

        @Override
        public void writeToBuffer(FluidStack value, FriendlyByteBuf buf) {
            value.writeToPacket(buf);
        }

        @Override
        public FluidStack readFromBuffer(FriendlyByteBuf buf, FluidStack currentValue) {
            return FluidStack.readFromPacket(buf);
        }

        @Override
        public Tag serializeNBT(FluidStack value) {
            return value.writeToNBT(new CompoundTag());
        }

        @Override
        public FluidStack deserializeNBT(Tag tag, @Nullable FluidStack currentVal) {
            return (tag instanceof CompoundTag compoundTag) ? FluidStack.loadFluidStackFromNBT(compoundTag) :
                    FluidStack.EMPTY;
        }
    }

    public static class UUIDTransformer implements IValueTransformer<UUID> {

        @Override
        public void writeToBuffer(UUID value, FriendlyByteBuf buf) {
            buf.writeUUID(value);
        }

        @Override
        public UUID readFromBuffer(FriendlyByteBuf buf, UUID currentValue) {
            return buf.readUUID();
        }

        @Override
        public Tag serializeNBT(UUID value) {
            return NbtUtils.createUUID(value);
        }

        @Override
        public UUID deserializeNBT(Tag tag, @Nullable UUID currentVal) {
            return NbtUtils.loadUUID(tag);
        }
    }

    public static class BlockPosTransformer implements IValueTransformer<BlockPos> {

        @Override
        public void writeToBuffer(BlockPos value, FriendlyByteBuf buf) {
            buf.writeBlockPos(value);
        }

        @Override
        public BlockPos readFromBuffer(FriendlyByteBuf buf, BlockPos currentValue) {
            return buf.readBlockPos();
        }

        @Override
        public Tag serializeNBT(BlockPos value) {
            return NbtUtils.writeBlockPos(value);
        }

        @Override
        public BlockPos deserializeNBT(Tag tag, @Nullable BlockPos currentVal) {
            return (tag instanceof CompoundTag compoundTag) ? NbtUtils.readBlockPos(compoundTag) : BlockPos.ZERO;
        }
    }

    public static class CompoundTagTransformer implements IValueTransformer<CompoundTag> {

        @Override
        public void writeToBuffer(CompoundTag value, FriendlyByteBuf buf) {
            buf.writeNbt(value);
        }

        @Override
        public CompoundTag readFromBuffer(FriendlyByteBuf buf, CompoundTag currentValue) {
            return buf.readNbt();
        }

        @Override
        public Tag serializeNBT(CompoundTag value) {
            return value;
        }

        @Override
        public CompoundTag deserializeNBT(Tag tag, @Nullable CompoundTag currentVal) {
            return (tag instanceof CompoundTag compoundTag) ? compoundTag : new CompoundTag();
        }
    }

    public static class ComponentTransformer implements IValueTransformer<Component> {

        @Override
        public void writeToBuffer(Component value, FriendlyByteBuf buf) {
            buf.writeComponent(value);
        }

        @Override
        public Component readFromBuffer(FriendlyByteBuf buf, Component currentValue) {
            return buf.readComponent();
        }

        @Override
        public Tag serializeNBT(Component value) {
            return StringTag.valueOf(Component.Serializer.toJson(value));
        }

        @Override
        public Component deserializeNBT(Tag tag, @Nullable Component currentVal) {
            return (tag instanceof StringTag stringTag) ? Component.Serializer.fromJson(stringTag.getAsString()) :
                    Component.empty();
        }
    }
}
