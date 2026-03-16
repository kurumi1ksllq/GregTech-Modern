package com.gregtechceu.gtceu.api.machine.mui;

import com.gregtechceu.gtceu.api.mui.GuiError;
import com.gregtechceu.gtceu.api.mui.base.widget.IWidget;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Area;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Box;
import com.gregtechceu.gtceu.api.mui.widget.sizer.ResizeNode;
import com.gregtechceu.gtceu.api.mui.widget.sizer.StandardResizer;

public class CoverSingleChildResizer extends StandardResizer {

    private final IWidget targetChild;

    public CoverSingleChildResizer(IWidget widget, IWidget targetChild) {
        super(widget);
        this.targetChild = targetChild;
    }

    @Override
    public boolean postResize() {

        IWidget widget = getWidget();
        if (!widget.hasChildren()) {
            coverChildrenForEmpty();
            return isSelfFullyCalculated();
        }

        // non layout widgets can have their children in any position
        // we try to wrap all edges as close as possible to all widgets
        // this means for each edge there is at least one widget that touches it (plus padding and margin)

        // children are now calculated and now this area can be calculated if it requires children's area

        int moveChildrenX = 0, moveChildrenY = 0;

        Box padding = getWidget().getArea().getPadding();
        // first calculate the area the children span
        int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y0 = Integer.MAX_VALUE, y1 = Integer.MIN_VALUE;
        int w = 0, h = 0;
        boolean hasIndependentChildX = false;
        boolean hasIndependentChildY = false;

        var child = targetChild;
        Box margin = child.getArea().getMargin();
        ResizeNode resizeable = child.resizer();
        Area area = child.getArea();
        if (!resizeable.dependsOnParentX()) {
            hasIndependentChildX = true;
            if (resizeable.isWidthCalculated() && resizeable.isXCalculated()) {
                w = Math.max(w, area.requestedWidth() + padding.horizontal());
                x0 = Math.min(x0, area.rx - padding.left() - margin.left());
                x1 = Math.max(x1, area.rx + area.width + padding.right + margin.right);
            } else {
                return isSelfFullyCalculated();
            }
        }

        if (!resizeable.dependsOnParentY()) {
            hasIndependentChildY = true;
            if (resizeable.isHeightCalculated() && resizeable.isYCalculated()) {
                h = Math.max(h, area.requestedHeight() + padding.vertical());
                y0 = Math.min(y0, area.ry - padding.top() - margin.top());
                y1 = Math.max(y1, area.ry + area.height + padding.bottom + margin.bottom);
            } else {
                return isSelfFullyCalculated();
            }
        }
        if ((!hasIndependentChildX) || (!hasIndependentChildY)) {
            GuiError.throwNew(getWidget(), GuiError.Type.SIZING,
                    "Can't cover children when all children depend on their parent!");
            return false;
        }
        if (x1 == Integer.MIN_VALUE) x1 = 0;
        if (y1 == Integer.MIN_VALUE) y1 = 0;
        if (x0 == Integer.MAX_VALUE) x0 = 0;
        if (y0 == Integer.MAX_VALUE) y0 = 0;
        if (w > x1 - x0) x1 = x0 + w; // we found at least one widget which was wider than what was calculated by start
        // and end pos
        if (h > y1 - y0) y1 = y0 + h;

        // now calculate new x, y, width and height based on the children area
        Area relativeTo = getParent().getArea();
        // apply the size to this widget
        // the return value is the amount of pixels we need to move the children
        moveChildrenX = this.x.postApply(getWidget().getArea(), relativeTo, x0, x1);
        moveChildrenY = this.y.postApply(getWidget().getArea(), relativeTo, y0, y1);
        // since the edges might have been moved closer to the widgets, the widgets should move back into it's original
        // (absolute) position
        if (moveChildrenX != 0 || moveChildrenY != 0) {
            if (resizeable.isXCalculated()) area.rx += moveChildrenX;
            if (resizeable.isYCalculated()) area.ry += moveChildrenY;
        }
        return isSelfFullyCalculated();
    }
}
