package com.gregtechceu.gtceu.client.mui.screen;

import com.gregtechceu.gtceu.api.mui.base.drawable.IRichTextBuilder;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RichTooltipEvent {

    private RichTooltipEvent() {}

    // TODO: this can probably be removed?
    @Cancelable
    public static class Pre extends RenderTooltipEvent.Pre {

        public Pre(@NotNull ItemStack stack, GuiGraphics graphics, int x, int y, int screenWidth, int screenHeight, @NotNull Font font, @NotNull List<ClientTooltipComponent> components, @NotNull ClientTooltipPositioner positioner) {
            super(stack, graphics, x, y, screenWidth, screenHeight, font, components, positioner);
        }

    }

    public static class Color extends RenderTooltipEvent.Color {

        private final IRichTextBuilder<?> tooltip;

        public Color(@NotNull ItemStack stack, GuiGraphics graphics, int x, int y, @NotNull Font fr, int background, int borderStart, int borderEnd, @NotNull List<ClientTooltipComponent> components, IRichTextBuilder<?> tooltip) {
            super(stack, graphics, x, y, fr, background, borderStart, borderEnd, components);
            this.tooltip = tooltip;

        }
        public IRichTextBuilder<?> getTooltip() {
            return tooltip;
        }
    }

    @Cancelable
    public static class GatherComponents extends RenderTooltipEvent.GatherComponents {

        private final IRichTextBuilder<?> tooltip;

        public GatherComponents(ItemStack itemStack, int screenWidth, int screenHeight, List<Either<FormattedText, TooltipComponent>> tooltipElements, int maxWidth, IRichTextBuilder<?> tooltip) {
            super(itemStack, screenWidth, screenHeight, tooltipElements, maxWidth);
            this.tooltip = tooltip;
        }

        public IRichTextBuilder<?> getTooltip() {
            return tooltip;
        }
    }
}
