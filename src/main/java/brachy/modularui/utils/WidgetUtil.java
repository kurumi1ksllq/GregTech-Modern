package brachy.modularui.utils;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.ModularScreen;

import java.util.ArrayList;
import java.util.List;

public class WidgetUtil {

    public static List<IWidget> getFlatWidgetCollection(ModularScreen screen) {
        List<IWidget> list = new ArrayList<>();
        addToFlatWidgetCollection(screen.getMainPanel(), list);
        return list;
    }

    public static List<IWidget> getFlatWidgetCollection(IWidget widget) {
        List<IWidget> list = new ArrayList<>();
        addToFlatWidgetCollection(widget, list);
        return list;
    }

    public static void addToFlatWidgetCollection(IWidget widget, List<IWidget> list) {
        list.add(widget);
        for (IWidget child : widget.getChildren()) {
            addToFlatWidgetCollection(child, list);
        }
    }
}
