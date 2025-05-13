package com.gregtechceu.gtceu.api.mui.base;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

public class MCHelper {

    public static boolean hasMc() {
        return getMc() != null;
    }

    public static Minecraft getMc() {
        return Minecraft.getInstance();
    }

    public static LocalPlayer getPlayer() {
        if (hasMc()) {
            return getMc().player;
        }
        return null;
    }

    public static boolean closeScreen() {
        if (!hasMc()) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.closeContainer();
            return true;
        }
        Minecraft.getInstance().setScreen(null);
        return false;
    }

    public static boolean setScreen(Screen screen) {
        Minecraft mc = getMc();
        if (mc != null) {
            mc.setScreen(screen);
            return true;
        }
        return false;
    }

    public static Screen getCurrentScreen() {
        Minecraft mc = getMc();
        return mc != null ? mc.screen : null;
    }

    public static Font getFont() {
        if (hasMc()) return getMc().font;
        return null;
    }

    public static List<Component> getItemToolTip(ItemStack item) {
        if (!hasMc()) return Collections.emptyList();
        return Screen.getTooltipFromItem(getMc(), item);
    }
}
