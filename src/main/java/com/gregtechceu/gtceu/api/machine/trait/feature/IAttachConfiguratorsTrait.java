package com.gregtechceu.gtceu.api.machine.trait.feature;

import com.gregtechceu.gtceu.api.mui.value.sync.PanelSyncManager;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Flow;
import com.gregtechceu.gtceu.client.mui.screen.ModularPanel;

public interface IAttachConfiguratorsTrait {

    default void attachLeftConfigurators(Flow flow, ModularPanel panel, PanelSyncManager syncManager) {}

    default void attachRightConfigurators(Flow flow, ModularPanel panel, PanelSyncManager syncManager) {}
}
