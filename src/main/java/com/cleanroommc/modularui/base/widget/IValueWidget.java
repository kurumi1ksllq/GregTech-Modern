package com.cleanroommc.modularui.base.widget;

/**
 * Marks a widget as containing a value
 *
 * @param <T>
 */
public interface IValueWidget<T> extends IWidget {

    /**
     * @return stored value
     */
    T getWidgetValue();
}
