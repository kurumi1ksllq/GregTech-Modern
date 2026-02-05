package brachy.modularui.widgets.menu;

import brachy.modularui.api.ITheme;
import brachy.modularui.api.IThemeApi;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.theme.WidgetThemeEntry;
import brachy.modularui.widget.ParentWidget;

public class Menu<W extends Menu<W>> extends ParentWidget<W> implements IMenuPart {

    private AbstractMenuButton<?> menuSource;

    void setMenuSource(AbstractMenuButton<?> source) {
        this.menuSource = source;
    }

    @Override
    public void onMouseLeaveArea() {
        super.onMouseLeaveArea();
        checkClose();
    }

    protected void checkClose() {
        if (this.menuSource != null && !this.menuSource.isBelowMouse() && !isSelfOrChildHovered()) {
            this.menuSource.closeMenu(true);
            this.menuSource.checkClose();
        }
    }

    @Override
    protected void onChildAdd(IWidget child) {
        super.onChildAdd(child);
        if (!child.resizer().hasHeight()) {
            child.resizer().height(12);
        }
        if (!child.resizer().hasWidth()) {
            child.resizer().widthRel(1f);
        }
    }

    @Override
    protected WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
        return theme.getWidgetTheme(IThemeApi.PANEL);
    }
}
