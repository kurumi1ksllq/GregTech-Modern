package brachy.modularui.widget.sizer;

import brachy.modularui.api.widget.IWidget;

public class DelegatingResizer extends StandardResizer {

    public DelegatingResizer(IWidget widget) {
        super(widget);
    }
}
