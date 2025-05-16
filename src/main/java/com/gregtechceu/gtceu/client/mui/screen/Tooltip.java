package com.gregtechceu.gtceu.client.mui.screen;

import com.gregtechceu.gtceu.api.mui.base.drawable.IDrawable;
import com.gregtechceu.gtceu.api.mui.base.drawable.IIcon;
import com.gregtechceu.gtceu.api.mui.base.drawable.IKey;
import com.gregtechceu.gtceu.api.mui.base.widget.IWidget;
import com.gregtechceu.gtceu.api.mui.drawable.GuiDraw;
import com.gregtechceu.gtceu.api.mui.drawable.Icon;
import com.gregtechceu.gtceu.api.mui.drawable.IconRenderer;
import com.gregtechceu.gtceu.api.mui.drawable.text.TextIcon;
import com.gregtechceu.gtceu.api.mui.drawable.text.TextRenderer;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Rectangle;
import com.gregtechceu.gtceu.client.mui.component.DrawableTooltipComponent;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.utils.Color;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Area;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Deprecated
public class Tooltip {

    private final IWidget parent;
    private final List<IDrawable> lines = new ArrayList<>();
    private List<IDrawable> additionalLines = new ArrayList<>();
    private RichTooltip.Pos pos = null;
    private Consumer<Tooltip> tooltipBuilder;
    private int showUpTimer = 0;

    private int x = 0, y = 0;
    private int maxWidth = Integer.MAX_VALUE;
    private boolean textShadow = true;
    private int textColor = Color.WHITE.main;
    private float scale = 1.0f;
    private Alignment alignment = Alignment.TopLeft;
    private boolean autoUpdate = false;
    private boolean hasTitleMargin = true;
    private int linePadding = 1;

    private boolean dirty = true;

    public Tooltip(IWidget parent) {
        this.parent = parent;
    }

    public void buildTooltip() {
        this.dirty = false;
        this.lines.clear();
        List<IDrawable> additionalLines = this.additionalLines;
        this.additionalLines = this.lines;
        if (this.tooltipBuilder != null) {
            this.tooltipBuilder.accept(this);
        }
        this.lines.addAll(additionalLines);
        this.additionalLines = additionalLines;
        if (this.hasTitleMargin && this.lines.size() > 1) {
            this.lines.add(1, Icon.EMPTY_2PX);
        }
    }

    public void draw(GuiContext context) {
        draw(context, ItemStack.EMPTY);
    }

    public void draw(GuiContext context, @Nullable ItemStack stack) {
        if (this.autoUpdate) {
            markDirty();
        }
        if (isEmpty()) return;

        if (this.maxWidth <= 0) {
            this.maxWidth = Integer.MAX_VALUE;
        }
        if (stack == null) stack = ItemStack.EMPTY;
        Area screen = context.getScreenArea();
        int mouseX = context.getMouseX(), mouseY = context.getMouseY();
        IconRenderer renderer = IconRenderer.SHARED;

        // this only turns the text and not any drawables into strings
        List<Either<FormattedText, TooltipComponent>> textLines = lines.stream()
                .<Either<FormattedText, TooltipComponent>>map(drawable -> {
                    if (drawable instanceof IKey key) {
                        return Either.left(key.getFormatted());
                    } else if (drawable instanceof TextIcon textIcon) {
                        return Either.left(textIcon.getText());
                    } else {
                        return Either.right(new DrawableTooltipComponent(drawable));
                    }
                })
                .collect(Collectors.toList());

        var gatherEvent = new RenderTooltipEvent.GatherComponents(stack, screen.width, screen.height, textLines, this.maxWidth);
        if (MinecraftForge.EVENT_BUS.post(gatherEvent)) return; // canceled
        this.maxWidth = gatherEvent.getMaxWidth();
        textLines = gatherEvent.getTooltipElements();
        List<ClientTooltipComponent> components = textLines.stream()
                .map(either -> either.map(
                        text -> ClientTooltipComponent.create(text instanceof Component ? ((Component) text).getVisualOrderText() : Language.getInstance().getVisualOrder(text)),
                        ClientTooltipComponent::create
                ))
                .toList();

        RenderTooltipEvent.Pre event = new RenderTooltipEvent.Pre(stack, context.getGraphics(),
                mouseX, mouseY, screen.width, screen.height,
                TextRenderer.getFont(), components, DefaultTooltipPositioner.INSTANCE);
        if (MinecraftForge.EVENT_BUS.post(event)) return; // canceled
        //lines = event.getLines();
        mouseX = event.getX();
        mouseY = event.getY();
        int screenWidth = event.getScreenWidth(), screenHeight = event.getScreenHeight();

        renderer.setShadow(this.textShadow);
        renderer.setColor(this.textColor);
        renderer.setScale(this.scale);
        renderer.setAlignment(this.alignment, this.maxWidth);
        renderer.setLinePadding(this.linePadding);
        renderer.setSimulate(true);
        renderer.setPos(0, 0);

        //List<IIcon> icons = renderer.measureStringLines(this.lines);
        renderer.draw(context, this.lines);

        Rectangle area = determineTooltipArea(context, this.lines, renderer, screenWidth, screenHeight, mouseX, mouseY);

        Lighting.setupForFlatItems();
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();

        GuiDraw.drawTooltipBackground(context.getGraphics(), stack, components, area.x, area.y, area.width, area.height);

        // MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostBackground(stack, textLines, area.x, area.y, TextRenderer.getFont(), area.width, area.height));

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        renderer.setSimulate(false);
        //renderer.setAlignment(Alignment.TopLeft, area.width, area.height);
        renderer.setPos(area.x, area.y);
        renderer.draw(context, this.lines);

        // MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostText(stack, textLines, area.x, area.y, TextRenderer.getFont(), area.width, area.height));
    }

    public Rectangle determineTooltipArea(GuiContext context, List<IDrawable> lines, IconRenderer renderer, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        int width = (int) renderer.getLastWidth();
        int height = (int) renderer.getLastHeight();

        RichTooltip.Pos pos = this.pos;
        if (pos == null) {
            pos = context.isMuiContext() ? context.getMuiContext().getScreen().getCurrentTheme().getTooltipPosOverride() : null;
            if (pos == null) pos = ModularUIConfig.tooltipPos;
        }
        if (pos == RichTooltip.Pos.FIXED) {
            return new Rectangle(this.x, this.y, width, height);
        }

        if (pos == RichTooltip.Pos.NEXT_TO_MOUSE) {
            final int padding = 8;
            // magic number to place tooltip nicer. Look at Screen#L237
            final int mouseOffset = 12;
            int x = mouseX + mouseOffset, y = mouseY - mouseOffset;
            if (x < padding) {
                x = padding;
            } else if (x + width + padding > screenWidth) {
                x -= mouseOffset * 2 + width; // flip side of cursor
                if (x < padding) {
                    x = padding;
                }
            }
            y = Mth.clamp(y, padding, screenHeight - padding - height);
            return new Rectangle(x, y, width, height);
        }

        if (this.parent == null) {
            throw new IllegalStateException("Tooltip pos is " + this.pos.name() + ", but no widget parent is set!");
        }

        int minWidth = 0;
        for (IDrawable line : lines) {
            if (line instanceof IIcon icon && !(line instanceof TextIcon)) {
                minWidth = Math.max(minWidth, icon.getWidth());
            } else if (!(line instanceof IKey)) {
                minWidth = Math.max(minWidth, 18);
            }
        }

        int shiftAmount = 10;
        int padding = 7;

        Area area = Area.SHARED;
        area.set(this.parent.getArea());
        area.setPos(0, 0); // context is transformed to this widget
        area.transformAndRectanglerize(context);
        int x = 0, y = 0;
        if (pos.axis.isVertical()) {
            if (width < area.width) {
                x = area.x + shiftAmount;
            } else {
                x = area.x - shiftAmount;
                if (x < padding) {
                    x = padding;
                } else if (x + width > screenWidth - padding) {
                    int maxWidth = Math.max(minWidth, screenWidth - x - padding);
                    renderer.setAlignment(this.alignment, maxWidth);
                    renderer.draw(context, lines);
                    width = (int) renderer.getLastWidth();
                    height = (int) renderer.getLastHeight();
                }
            }

            RichTooltip.Pos pos1 = pos;
            if (pos == RichTooltip.Pos.VERTICAL) {
                int bottomSpace = screenHeight - area.ey();
                pos1 = bottomSpace < height + padding && bottomSpace < area.y ? RichTooltip.Pos.ABOVE : RichTooltip.Pos.BELOW;
            }

            if (pos1 == RichTooltip.Pos.BELOW) {
                y = area.ey() + padding;
            } else if (pos1 == RichTooltip.Pos.ABOVE) {
                y = area.y - height - padding;
            }
        } else if (pos.axis.isHorizontal()) {
            boolean usedMoreSpaceSide = false;
            RichTooltip.Pos pos1 = pos;
            if (pos == RichTooltip.Pos.HORIZONTAL) {
                if (area.x > screenWidth - area.ex()) {
                    pos1 = RichTooltip.Pos.LEFT;
                    // x = 0;
                } else {
                    pos1 = RichTooltip.Pos.RIGHT;
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
                if (pos1 == RichTooltip.Pos.LEFT) {
                    maxWidth = Math.max(minWidth, area.x - padding * 2);
                } else {
                    maxWidth = Math.max(minWidth, screenWidth - area.ex() - padding * 2);
                }
                usedMoreSpaceSide = true;
                renderer.setAlignment(this.alignment, maxWidth);
                renderer.draw(context, lines);
                width = (int) renderer.getLastWidth();
                height = (int) renderer.getLastHeight();
            }

            if (pos == RichTooltip.Pos.HORIZONTAL && !usedMoreSpaceSide) {
                int rightSpace = screenWidth - area.ex();
                pos1 = rightSpace < width + padding && rightSpace < area.x ? RichTooltip.Pos.LEFT : RichTooltip.Pos.RIGHT;
            }

            if (pos1 == RichTooltip.Pos.RIGHT) {
                x = area.ex() + padding;
            } else if (pos1 == RichTooltip.Pos.LEFT) {
                x = area.x - width - padding;
            }
        }
        return new Rectangle(x, y, width, height);
    }

    public boolean isEmpty() {
        if (this.dirty) {
            buildTooltip();
        }
        return this.lines.isEmpty();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public int getShowUpTimer() {
        return this.showUpTimer;
    }

    @Nullable
    public Consumer<Tooltip> getTooltipBuilder() {
        return this.tooltipBuilder;
    }

    public boolean isAutoUpdate() {
        return this.autoUpdate;
    }

    public boolean hasTitleMargin() {
        return this.hasTitleMargin;
    }

    public Tooltip pos(RichTooltip.Pos pos) {
        this.pos = pos;
        return this;
    }

    public Tooltip pos(int x, int y) {
        this.pos = RichTooltip.Pos.FIXED;
        this.x = x;
        this.y = y;
        return this;
    }

    public Tooltip alignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    public Tooltip textShadow(boolean textShadow) {
        this.textShadow = textShadow;
        return this;
    }

    public Tooltip textColor(int textColor) {
        this.textColor = textColor;
        return this;
    }

    public Tooltip scale(float scale) {
        this.scale = scale;
        return this;
    }

    public Tooltip showUpTimer(int showUpTimer) {
        this.showUpTimer = showUpTimer;
        return this;
    }

    public Tooltip tooltipBuilder(Consumer<Tooltip> tooltipBuilder) {
        Consumer<Tooltip> existingBuilder = this.tooltipBuilder;
        if (existingBuilder != null) {
            this.tooltipBuilder = tooltip -> {
                existingBuilder.accept(this);
                tooltipBuilder.accept(this);
            };
        } else {
            this.tooltipBuilder = tooltipBuilder;
        }
        return this;
    }

    public Tooltip setAutoUpdate(boolean update) {
        this.autoUpdate = update;
        return this;
    }

    public Tooltip setHasTitleMargin(boolean hasTitleMargin) {
        this.hasTitleMargin = hasTitleMargin;
        return this;
    }

    /**
     * By default, tooltips have 1px of space between lines. Set to 0 if you want to disable it.
     */
    public Tooltip setLinePadding(int linePadding) {
        this.linePadding = linePadding;
        return this;
    }

    public Tooltip addLine(IDrawable drawable) {
        this.additionalLines.add(drawable);
        return this;
    }

    public Tooltip addLine(String line) {
        return addLine(IKey.str(line));
    }

    public Tooltip addLine(Component line) {
        return addLine(IKey.lang(line.copy()));
    }

    public Tooltip addDrawableLines(Iterable<IDrawable> lines) {
        for (IDrawable line : lines) {
            addLine(line);
        }
        return this;
    }

    public Tooltip addStringLines(Iterable<String> lines) {
        for (String line : lines) {
            addLine(IKey.str(line));
        }
        return this;
    }
}
