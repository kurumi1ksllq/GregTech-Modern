package com.gregtechceu.gtceu.api.mui.drawable.text;

import com.gregtechceu.gtceu.api.mui.base.drawable.IIcon;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;
import com.gregtechceu.gtceu.api.mui.theme.WidgetTheme;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Box;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TextIcon implements IIcon {

    private final String text;
    private final int width, height;
    private final float scale;
    private final Alignment alignment;
    private static final Box margin = new Box();

    public TextIcon(String text, int width, int height, float scale, Alignment alignment) {
        this.text = text;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.alignment = alignment;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        TextRenderer.SHARED.setPos(x, y);
        TextRenderer.SHARED.setAlignment(this.alignment, width);
        TextRenderer.SHARED.setScale(this.scale);
        TextRenderer.SHARED.drawSimple(this.text);
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public Box getMargin() {
        return margin;
    }

    public String getText() {
        return this.text;
    }
}
