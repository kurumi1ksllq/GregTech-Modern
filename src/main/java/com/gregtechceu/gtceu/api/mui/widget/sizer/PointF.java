package com.gregtechceu.gtceu.api.mui.widget.sizer;

import lombok.Getter;
import lombok.Setter;

public final class PointF {

    public static final PointF ZERO = new PointF(0, 0);

    @Getter @Setter
    public float x;
    @Getter @Setter
    public float y;

    public PointF() {
        this(0, 0);
    }

    public PointF(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static PointF fromTopLeft(Rectangle bounds) {
        return new PointF(bounds.getX(), bounds.getY());
    }

    public PointF move(float x, float y) {
        return new PointF(this.x + x, this.y + y);
    }

    public boolean isIn(Rectangle rect) {
        return x >= rect.getX()
                && y >= rect.getY()
                && x < rect.getX() + rect.getWidth()
                && y < rect.getY() + rect.getHeight();
    }

}
