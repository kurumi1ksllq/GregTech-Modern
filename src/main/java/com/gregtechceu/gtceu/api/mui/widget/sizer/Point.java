package com.gregtechceu.gtceu.api.mui.widget.sizer;

import lombok.Getter;
import lombok.Setter;

public final class Point {

    public static final Point ZERO = new Point(0, 0);

    @Getter @Setter
    public int x;
    @Getter @Setter
    public int y;

    public Point() {
        this(0, 0);
    }

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Point fromTopLeft(Rectangle bounds) {
        return new Point(bounds.getX(), bounds.getY());
    }

    public Point move(int x, int y) {
        return new Point(this.x + x, this.y + y);
    }

    public boolean isIn(Rectangle rect) {
        return x >= rect.getX()
                && y >= rect.getY()
                && x < rect.getX() + rect.getWidth()
                && y < rect.getY() + rect.getHeight();
    }

}
