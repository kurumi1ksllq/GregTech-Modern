package com.gregtechceu.gtceu.api.mui.theme;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class ThemeReloadCommand extends CommandBase {

    @Override
    public @NotNull String getName() {
        return "reloadThemes";
    }

    @Override
    public @NotNull String getUsage(@NotNull ICommandSender sender) {
        return "/reloadThemes";
    }

    @Override
    public void execute(@NotNull MinecraftServer server, @NotNull ICommandSender sender, String @NotNull [] args) throws CommandException {
        try {
            sender.sendMessage(new TextComponentString("Reloading ModularUI themes..."));
            ThemeManager.reload();
            sender.sendMessage(new TextComponentString(ChatFormatting.GREEN + "Successfully reloaded themes"));
        } catch (Exception e) {
            sender.sendMessage(new TextComponentString(ChatFormatting.RED + "Error reloaded themes:"));
            sender.sendMessage(new TextComponentString(ChatFormatting.RED + e.getMessage()));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
