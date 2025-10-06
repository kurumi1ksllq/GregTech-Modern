package com.gregtechceu.gtceu.api.mui.base;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MCHelper {

    public static Minecraft getMc() {
        return Minecraft.getInstance();
    }

    public static LocalPlayer getPlayer() {
        return getMc().player;
    }

    public static boolean closeScreen() {
        LocalPlayer player = getMc().player;
        if (player != null) {
            player.closeContainer();
            return true;
        }
        getMc().setScreen(null);
        return false;
    }

    public static void setScreen(Screen screen) {
        getMc().setScreen(screen);
    }

    public static Screen getCurrentScreen() {
        return getMc().screen;
    }

    public static Font getFont() {
        return getMc().font;
    }

    public static List<Component> getItemToolTip(ItemStack item) {
        if (getMc().screen != null) return Screen.getTooltipFromItem(getMc(), item);
        List<Component> list = item.getTooltipLines(getPlayer(),
                getMc().options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                list.set(i,
                        Component.empty().append(item.getHoverName()).withStyle(item.getRarity().getStyleModifier()));
            } else {
                list.set(i, list.get(i).copy().withStyle(ChatFormatting.GRAY));
            }
        }
        return list;
    }
}
