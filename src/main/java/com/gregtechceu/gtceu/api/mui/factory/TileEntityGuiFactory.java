package com.gregtechceu.gtceu.api.mui.factory;

import com.gregtechceu.gtceu.api.mui.base.IGuiHolder;
import net.minecraft.entity.player.Player;
import net.minecraft.entity.player.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BlockEntityGuiFactory extends AbstractUIFactory<PosGuiData> {

    public static final BlockEntityGuiFactory INSTANCE = new BlockEntityGuiFactory();

    private BlockEntityGuiFactory() {
        super("mui:tile_entity");
    }

    public <T extends BlockEntity & IGuiHolder<PosGuiData>> void open(Player player, T tile) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(tile);
        if (tile.isInvalid()) {
            throw new IllegalArgumentException("Can't open invalid BlockEntity GUI!");
        }
        if (player.world != tile.getWorld()) {
            throw new IllegalArgumentException("BlockEntity must be in same dimension as the player!");
        }
        BlockPos pos = tile.getPos();
        PosGuiData data = new PosGuiData(player, pos.getX(), pos.getY(), pos.getZ());
        GuiManager.open(this, data, (ServerPlayer) player);
    }

    public void open(Player player, BlockPos pos) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(pos);
        PosGuiData data = new PosGuiData(player, pos.getX(), pos.getY(), pos.getZ());
        GuiManager.open(this, data, (ServerPlayer) player);
    }

    @Override
    public @NotNull IGuiHolder<PosGuiData> getGuiHolder(PosGuiData data) {
        return Objects.requireNonNull(castGuiHolder(data.getBlockEntity()), "Found BlockEntity is not a gui holder!");
    }

    @Override
    public boolean canInteractWith(Player player, PosGuiData guiData) {
        return player == guiData.getPlayer() && guiData.getBlockEntity() != null && guiData.getSquaredDistance(player) <= 64;
    }

    @Override
    public void writeGuiData(PosGuiData guiData, FriendlyByteBuf buffer) {
        buffer.writeVarInt(guiData.getX());
        buffer.writeVarInt(guiData.getY());
        buffer.writeVarInt(guiData.getZ());
    }

    @Override
    public @NotNull PosGuiData readGuiData(Player player, FriendlyByteBuf buffer) {
        return new PosGuiData(player, buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }
}
