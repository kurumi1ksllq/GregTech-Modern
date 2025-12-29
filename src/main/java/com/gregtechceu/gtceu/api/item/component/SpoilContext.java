package com.gregtechceu.gtceu.api.item.component;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.server.ServerLifecycleHooks;

import lombok.With;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@With
public record SpoilContext(@Nullable Level level, @Nullable BlockPos pos, @Nullable Entity entity,
                           @Nullable IItemHandler itemHandler, int slot) {

    public static @Nullable Level getDefaultLevel() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return server.overworld();
    }

    public SpoilContext() {
        this((Level) null);
    }

    public SpoilContext(@Nullable Level level) {
        this(level, null);
    }

    public SpoilContext(@Nullable Level level, @Nullable BlockPos pos) {
        this(level, pos, null, null, -1);
    }

    public SpoilContext(@NotNull Entity entity) {
        this(entity.level(), null, entity, null, -1);
    }

    public SpoilContext(@NotNull Player player, int slot) {
        this(player.level(), null, player, new CustomItemStackHandler(player.getInventory().items), slot);
    }

    public SpoilContext(@NotNull MetaMachine machine, @Nullable IItemHandler itemHandler, int slot) {
        this(machine.getLevel(), machine.getPos(), null, itemHandler, slot);
    }

    public SpoilContext(IItemHandler itemHandler) {
        this(null, null, null, itemHandler, -1);
    }

    public boolean isEmpty() {
        return level == null;
    }
}
