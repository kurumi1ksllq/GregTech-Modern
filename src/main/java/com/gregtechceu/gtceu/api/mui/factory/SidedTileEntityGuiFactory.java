package com.gregtechceu.gtceu.api.mui.factory;

import com.gregtechceu.gtceu.api.mui.base.IGuiHolder;
import net.minecraft.entity.player.Player;
import net.minecraft.entity.player.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SidedBlockEntityGuiFactory extends AbstractUIFactory<SidedPosGuiData> {

    public static final SidedBlockEntityGuiFactory INSTANCE = new SidedBlockEntityGuiFactory();

    public <T extends BlockEntity & IGuiHolder<SidedPosGuiData>> void open(Player player, T tile, EnumFacing facing) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(tile);
        Objects.requireNonNull(facing);
        if (tile.isInvalid()) {
            throw new IllegalArgumentException("Can't open invalid BlockEntity GUI!");
        }
        if (player.world != tile.getWorld()) {
            throw new IllegalArgumentException("BlockEntity must be in same dimension as the player!");
        }
        BlockPos pos = tile.getPos();
        SidedPosGuiData data = new SidedPosGuiData(player, pos.getX(), pos.getY(), pos.getZ(), facing);
        GuiManager.open(this, data, (ServerPlayer) player);
    }

    public void open(Player player, BlockPos pos, EnumFacing facing) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(pos);
        Objects.requireNonNull(facing);
        SidedPosGuiData data = new SidedPosGuiData(player, pos.getX(), pos.getY(), pos.getZ(), facing);
        GuiManager.open(this, data, (ServerPlayer) player);
    }

    private SidedBlockEntityGuiFactory() {
        super("mui:sided_tile");
    }

    @Override
    public @NotNull IGuiHolder<SidedPosGuiData> getGuiHolder(SidedPosGuiData data) {
        return Objects.requireNonNull(castGuiHolder(data.getBlockEntity()), "Found BlockEntity is not a gui holder!");
    }

    @Override
    public boolean canInteractWith(Player player, SidedPosGuiData guiData) {
        return player == guiData.getPlayer() && guiData.getBlockEntity() != null && guiData.getSquaredDistance(player) <= 64;
    }

    @Override
    public void writeGuiData(SidedPosGuiData guiData, FriendlyByteBuf buffer) {
        buffer.writeVarInt(guiData.getX());
        buffer.writeVarInt(guiData.getY());
        buffer.writeVarInt(guiData.getZ());
        buffer.writeByte(guiData.getSide().getIndex());
    }

    @Override
    public @NotNull SidedPosGuiData readGuiData(Player player, FriendlyByteBuf buffer) {
        return new SidedPosGuiData(player, buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), EnumFacing.VALUES[buffer.readByte()]);
    }
}
