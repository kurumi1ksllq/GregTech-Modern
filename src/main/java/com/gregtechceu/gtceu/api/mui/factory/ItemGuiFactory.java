package com.gregtechceu.gtceu.api.mui.factory;

import com.gregtechceu.gtceu.api.mui.base.IGuiHolder;
import net.minecraft.entity.player.Player;
import net.minecraft.entity.player.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.EnumHand;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ItemGuiFactory extends AbstractUIFactory<HandGuiData> {

    public static final ItemGuiFactory INSTANCE = new ItemGuiFactory();

    private ItemGuiFactory() {
        super("mui:item");
    }

    public void open(Player player, EnumHand hand) {
        if (player instanceof ServerPlayer entityServerPlayer) {
            open(entityServerPlayer, hand);
            return;
        }
        throw new IllegalStateException("Synced GUIs must be opened from server side");
    }

    public void open(ServerPlayer player, EnumHand hand) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(hand);
        HandGuiData guiData = new HandGuiData(player, hand);
        GuiManager.open(this, guiData, player);
    }

    @Override
    public @NotNull IGuiHolder<HandGuiData> getGuiHolder(HandGuiData data) {
        return Objects.requireNonNull(castGuiHolder(data.getUsedItemStack().getItem()), "Item was not a gui holder!");
    }

    @Override
    public void writeGuiData(HandGuiData guiData, FriendlyByteBuf buffer) {
        buffer.writeByte(guiData.getHand().ordinal());
    }

    @Override
    public @NotNull HandGuiData readGuiData(Player player, FriendlyByteBuf buffer) {
        return new HandGuiData(player, EnumHand.values()[buffer.readByte()]);
    }
}
