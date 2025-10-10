package com.gregtechceu.gtceu.client.mui.screen;

import com.gregtechceu.gtceu.api.mui.base.GuiAxis;
import com.gregtechceu.gtceu.api.mui.base.MCHelper;
import com.gregtechceu.gtceu.api.mui.base.drawable.IRichTextBuilder;
import com.gregtechceu.gtceu.api.mui.base.drawable.ITextLine;
import com.gregtechceu.gtceu.api.mui.base.widget.IWidget;
import com.gregtechceu.gtceu.api.mui.drawable.GuiDraw;
import com.gregtechceu.gtceu.api.mui.drawable.text.RichText;
import com.gregtechceu.gtceu.api.mui.drawable.text.TextRenderer;
import com.gregtechceu.gtceu.api.mui.utils.Color;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Area;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Accessors(chain = true)
public class RichTooltip implements IRichTextBuilder<RichTooltip> {

    private static final Area HOLDER = new Area();

    private final RichText text = new RichText();
    private Consumer<Area> parent;
    private Pos pos = null;
    private Consumer<RichTooltip> tooltipBuilder;
    @Getter
    @Setter
    private int showUpTimer = 0;
    @Getter
    @Setter
    private boolean autoUpdate = false;
    private int titleMargin = 0;
    private boolean appliedMargin = true;

    private int x = 0, y = 0;
    private int maxWidth = Integer.MAX_VALUE;

    private boolean dirty;

    public RichTooltip() {
        parent(Area.ZERO);
    }

    public RichTooltip parent(Consumer<Area> parent) {
        this.parent = parent;
        return this;
    }

    public RichTooltip parent(Supplier<Area> parent) {
        return parent(area -> area.set(parent.get()));
    }

    public RichTooltip parent(Area parent) {
        return parent(area -> area.set(parent));
    }

    public RichTooltip parent(IWidget parent) {
        return parent(area -> {
            area.setSize(parent.getArea());
            area.setPos(0, 0);
        });
    }

    public RichTooltip(Area parent) {
        this(area -> area.set(parent));
    }

    public RichTooltip(Supplier<Area> parent) {
        this(area -> area.set(parent.get()));
    }

    public RichTooltip(Consumer<Area> parent) {
        this.parent = parent;
    }

    public void buildTooltip() {
        this.dirty = false;
        if (this.tooltipBuilder != null) {
            this.text.clearText();
            this.tooltipBuilder.accept(this);
            this.appliedMargin = false;
        }
    }

    public void draw(GuiContext context) {
        draw(context, ItemStack.EMPTY);
    }

    public void draw(GuiContext context, @Nullable ItemStack stack) {
        if (this.autoUpdate) markDirty();
        if (isEmpty()) return;

        if (this.maxWidth <= 0) {
            this.maxWidth = Integer.MAX_VALUE;
        }
        if (stack == null) stack = ItemStack.EMPTY;
        if (!this.appliedMargin) {
            if (this.titleMargin > 0) {
                this.text.insertTitleMargin(this.titleMargin);
            }
            this.appliedMargin = true;
        }
        Area screen = context.getScreenArea();
        this.maxWidth = Math.min(this.maxWidth, screen.width);
        int mouseX = context.getAbsMouseX(), mouseY = context.getAbsMouseY();
        TextRenderer renderer = TextRenderer.SHARED;
        RichText copy = this.text.copy();

        var components = copy.getAsComponents();
        List<ClientTooltipComponent> clientComponents = new ArrayList<>();
        // TODO: Is this the right way to go from component to ClientTooltipComponent?
        // probably not, see forge's RenderHelper.gatherTooltipComponents
        for(var component : components){
            if(!(component instanceof ClientTooltipComponent clientComponent)) continue;
            clientComponents.add(clientComponent);
        }

        // This doesn't work, the GatherComponents needs a List<Either<FormattedText, TooltipComponent>>
        RichTooltipEvent.GatherComponents gatherEvent = new RichTooltipEvent.GatherComponents(stack, screen.width, screen.height, copy, this.maxWidth, copy.getRichText());
        if (MinecraftForge.EVENT_BUS.post(gatherEvent)) return; // canceled
        // this only turns the text and not any drawables into strings

        RichTooltipEvent.Pre preEvent = new RichTooltipEvent.Pre(stack, context.getGraphics(), mouseX, mouseY, screen.width, screen.height, context.getFont(), clientComponents, DefaultTooltipPositioner.INSTANCE);
        if (MinecraftForge.EVENT_BUS.post(preEvent)) return; // canceled
        // we are supposed to now use the strings of the event, but we can't properly determine where to put them
        mouseX = preEvent.getX();
        mouseY = preEvent.getY();
        int screenWidth = preEvent.getScreenWidth(), screenHeight = preEvent.getScreenHeight();
        this.maxWidth = gatherEvent.getMaxWidth();

        // simulate to figure how big this tooltip is without any restrictions
        copy.setupRenderer(renderer, 0, 0, this.maxWidth, -1, Color.WHITE.main, false);
        copy.compileAndDraw(renderer, context, true);

        Rectangle area = determineTooltipArea(copy, context, renderer, screenWidth, screenHeight, mouseX, mouseY);
        renderer.setPos(area.x, area.y);
        renderer.setAlignment(copy.getAlignment(), (float) Math.ceil(area.width + copy.getScale()), -1);
        List<ITextLine> compiledLines = copy.compileAndDraw(renderer, context, true);
        area.width = (int) renderer.getLastTrimmedWidth();
        area.height = (int) renderer.getLastTrimmedHeight();

        /* TODO: Find alternative for this
        // spotless:off
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableBlend();
        // spotless:on
        */

        // This fires the RichTooltipEvent.Color
        GuiDraw.drawTooltipBackground(context.getGraphics(), stack, clientComponents, area.x, area.y, area.width, area.height, this);

        // We don't have a PostBackground event in 1.20
        //MinecraftForge.EVENT_BUS.post(new RichTooltipEvent.PostBackground(stack, copy.getAsComponents(), area.x, area.y, TextRenderer.getFontRenderer(), area.width, area.height, copy));

        // TODO: alternative for this?
        //GlStateManager.color(1f, 1f, 1f, 1f);

        renderer.setPos(area.x, area.y);
        renderer.setSimulate(false);
        renderer.drawCompiled(context, compiledLines);

        // We don't have a PostText event in 1.20
        //MinecraftForge.EVENT_BUS.post(new RichTooltipEvent.PostText(stack, copy.getAsComponents(), area.x, area.y, TextRenderer.getFontRenderer(), area.width, area.height, copy));
    }

    public java.awt.Rectangle determineTooltipArea(RichText text, GuiContext context, TextRenderer renderer, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        int width = (int) renderer.getLastTrimmedWidth();
        int height = (int) renderer.getLastTrimmedHeight();
        if (width > screenWidth - 14) {
            width = screenWidth - 14;
        }

        Pos pos = this.pos;
        if (pos == null) {
            pos = context.isMuiContext() ? context.getMuiContext().getScreen().getCurrentTheme().getTooltipPosOverride() : null;
            if (pos == null) pos = ConfigHolder.INSTANCE.client.ui.tooltipPos;
        }
        if (pos == Pos.FIXED) {
            return new java.awt.Rectangle(this.x, this.y, width, height);
        }

        Area area = HOLDER;
        this.parent.accept(area);
        if (area.x == 0 && area.y == 0 && area.width == 0 && area.height == 0) {
            pos = Pos.NEXT_TO_MOUSE;
        }

        if (pos == Pos.NEXT_TO_MOUSE) {
            // vanilla style, tooltip floats next to mouse
            // note that this behaves slightly different from vanilla (better imo)
            final int padding = 8;
            // magic number to place tooltip nicer. Look at GuiScreen#L237
            final int mouseOffset = 12;
            int x = mouseX + mouseOffset, y = mouseY - mouseOffset;
            if (x < padding) {
                x = padding; // this can't happen mathematically since mouse is always positive
            } else if (x + width + padding > screenWidth) {
                // doesn't fit on the right side of the screen
                if (screenWidth - mouseX < mouseX) { // check if left side has more space
                    x -= mouseOffset * 2 + width; // flip side of cursor if other side has more space
                    if (x < padding) {
                        x = padding; // went of screen
                    }
                    width = mouseX - mouseOffset - padding; // max space on left side
                } else {
                    width = screenWidth - padding - x; // max space on right side
                }
                // recalculate with and height
                renderer.setPos(x, y);
                renderer.setAlignment(text.getAlignment(), width, -1);
                text.compileAndDraw(renderer, context, true);
                width = (int) renderer.getLastTrimmedWidth();
                height = (int) renderer.getLastTrimmedHeight();
            }
            y = Mth.clamp(y, padding, screenHeight - padding - height);
            return new java.awt.Rectangle(x, y, width, height);
        }

        // the rest of the cases will put the tooltip next a given area
        if (this.parent == null) {
            throw new IllegalStateException("Tooltip pos is " + pos.name() + ", but no widget parent is set!");
        }

        int minWidth = text.getMinWidth();

        int shiftAmount = 10;
        int padding = 7;

        area.transformAndRectanglerize(context);
        int x = 0, y = 0;
        if (pos.axis.isVertical()) { // above or below
            x = area.x + (width < area.width ? shiftAmount : -shiftAmount);
            x = Mth.clamp(x, padding, screenWidth - padding - width);

            if (pos == Pos.VERTICAL) {
                int bottomSpace = screenHeight - area.ey();
                pos = bottomSpace < height + padding && bottomSpace < area.y ? Pos.ABOVE : Pos.BELOW;
            }

            if (pos == Pos.BELOW) {
                y = area.ey() + padding;
            } else if (pos == Pos.ABOVE) {
                y = area.y - height - padding;
            }
        } else if (pos.axis.isHorizontal()) {
            boolean usedMoreSpaceSide = false;
            Pos oPos = pos;
            if (oPos == Pos.HORIZONTAL) {
                if (area.x > screenWidth - area.ex()) {
                    pos = Pos.LEFT;
                    // x = 0;
                } else {
                    pos = Pos.RIGHT;
                    x = screenWidth - area.ex() + padding;
                }
            }

            if (height < area.height) {
                y = area.y + shiftAmount;
            } else {
                y = area.y - shiftAmount;
                if (y < padding) {
                    y = padding;
                }
            }

            if (x + width > screenWidth - padding) {
                int maxWidth;
                if (pos == Pos.LEFT) {
                    maxWidth = Math.max(minWidth, area.x - padding * 2);
                } else {
                    maxWidth = Math.max(minWidth, screenWidth - area.ex() - padding * 2);
                }
                usedMoreSpaceSide = true;
                renderer.setAlignment(text.getAlignment(), maxWidth);
                text.compileAndDraw(renderer, context, true);
                width = (int) renderer.getLastTrimmedWidth();
                height = (int) renderer.getLastTrimmedHeight();
            }

            if (oPos == Pos.HORIZONTAL && !usedMoreSpaceSide) {
                int rightSpace = screenWidth - area.ex();
                pos = rightSpace < width + padding && rightSpace < area.x ? Pos.LEFT : Pos.RIGHT;
            }

            if (pos == Pos.RIGHT) {
                x = area.ex() + padding;
            } else if (pos == Pos.LEFT) {
                x = area.x - width - padding;
                if (x < padding) {
                    x = padding;
                    width = area.x - x - padding;
                }
            }
        }
        return new java.awt.Rectangle(x, y, width, height);
    }

    public boolean isEmpty() {
        if (this.dirty) buildTooltip();
        return this.text.isEmpty();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public RichTooltip pos(Pos pos) {
        this.pos = pos;
        return this;
    }

    public RichTooltip pos(int x, int y) {
        this.pos = Pos.FIXED;
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public RichTooltip getThis() {
        return this;
    }

    @Override
    public IRichTextBuilder<?> getRichText() {
        return text;
    }

    public RichTooltip showUpTimer(int showUpTimer) {
        return this.setShowUpTimer(showUpTimer);
    }

    public RichTooltip tooltipBuilder(Consumer<RichTooltip> tooltipBuilder) {
        Consumer<RichTooltip> existingBuilder = this.tooltipBuilder;
        if (existingBuilder != null) {
            this.tooltipBuilder = tooltip -> {
                existingBuilder.accept(this);
                tooltipBuilder.accept(this);
            };
        } else {
            this.tooltipBuilder = tooltipBuilder;
        }
        markDirty();
        return this;
    }

    public RichTooltip addFromItem(ItemStack item) {
        List<Component> lines = MCHelper.getItemToolTip(item);
        add(lines.get(0));
        if (lines.size() > 1) {
            spaceLine();
            for (int i = 1, n = lines.size(); i < n; i++) {
                add(lines.get(i)).newLine();
            }
        }
        return this;
    }

    public RichTooltip titleMargin() {
        return titleMargin(0);
    }

    public RichTooltip titleMargin(int margin) {
        this.titleMargin = margin;
        this.appliedMargin = false;
        return this;
    }

    private static void findIngredientArea(Area area, int x, int y) {
        Screen screen = MCHelper.getCurrentScreen();
        if (screen instanceof ContainerScreenWrapper guiContainer) {
            Slot slot = guiContainer.getSlotUnderMouse();
            if (slot != null) {
                int sx = slot.x + guiContainer.getGuiLeft();
                int sy = slot.y + guiContainer.getGuiTop();
                if (sx >= 0 && sy >= 0) {
                    area.set(sx - 1, sy - 1, 18, 18);
                    return;
                }
            }
        }
        // TODO: Jei support?
        // spotless:off
        /*
        if (ModularUI.isJeiLoaded()) {
            IShowsRecipeFocuses overlay = (IShowsRecipeFocuses) ModularUIJeiPlugin.getRuntime().getIngredientListOverlay();
            IClickedIngredient<?> ingredient = overlay.getIngredientUnderMouse(x, y);
            if (ingredient == null || ingredient.getArea() == null) {
                overlay = (IShowsRecipeFocuses) ModularUIJeiPlugin.getRuntime().getBookmarkOverlay();
                ingredient = overlay.getIngredientUnderMouse(x, y);
            }
            if (ingredient != null && ingredient.getArea() != null) {
                java.awt.Rectangle slot = ingredient.getArea();
                area.set(slot.x - 1, slot.y - 1, 18, 18);
                return;
            }
        }
         */
        area.set(Area.ZERO);
    }

    @ApiStatus.Internal
    public static void injectRichTooltip(ItemStack stack, List<String> lines, int x, int y) {
        RichTooltip tooltip = new RichTooltip();
        tooltip.parent(area -> RichTooltip.findIngredientArea(area, x, y));
        // Other positions don't really work due to the lack of GuiContext in non-modular uis
        tooltip.add(lines.get(0)).newLine();
        if (lines.size() > 1) {
            if (!stack.isEmpty()) {
                tooltip.spaceLine();
            }
            for (int i = 1, n = lines.size(); i < n; i++) {
                tooltip.add(lines.get(i)).newLine();
            }
        }

        tooltip.draw(GuiContext.getDefault(), stack);
    }

    public enum Pos {

        ABOVE(GuiAxis.Y),
        BELOW(GuiAxis.Y),
        LEFT(GuiAxis.X),
        RIGHT(GuiAxis.X),
        VERTICAL(GuiAxis.Y),
        HORIZONTAL(GuiAxis.X),
        NEXT_TO_MOUSE(null),
        FIXED(null);

        public final GuiAxis axis;

        Pos(GuiAxis axis) {
            this.axis = axis;
        }
    }
}
