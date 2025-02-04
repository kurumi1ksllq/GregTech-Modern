package com.gregtechceu.gtceu.api.cover.filter;

import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.api.ui.holder.HeldItemUIHolder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author KilaBash
 * @date 2023/3/14
 * @implNote Filter
 */
public interface Filter<T, S extends Filter<T, S>> extends Predicate<T> {

    void loadServerUI(Player player, UIContainerMenu<HeldItemUIHolder> menu, HeldItemUIHolder holder);

    UIComponent openConfigurator(int x, int y, UIAdapter<StackLayout> adapter);

    CompoundTag saveFilter();

    void setOnUpdated(Consumer<S> onUpdated);

    default boolean isBlackList() {
        return false;
    }

    default boolean isBlank() {
        return false;
    }
}
