package com.gregtechceu.gtceu.common.mui.widgets;

import com.gregtechceu.gtceu.api.mui.base.widget.IValueWidget;
import com.gregtechceu.gtceu.api.mui.widget.Widget;

public class ValueWidget<W extends ValueWidget<W, T>, T> extends Widget<W> implements IValueWidget<T> {

    private final T widgetValue;

    public ValueWidget(T widgetValue) {
        this.widgetValue = widgetValue;
    }

    @Override
    public T getWidgetValue() {
        return widgetValue;
    }
}
