package brachy.modularui.value.sync;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.widgets.DynamicSyncedWidget;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

public interface IDynamicSyncNotifiable {

    /**
     * An internal function which is used to link the {@link DynamicSyncedWidget}.
     */
    @ApiStatus.Internal
    void attachDynamicWidgetListener(Consumer<IWidget> onWidgetUpdate);
}
