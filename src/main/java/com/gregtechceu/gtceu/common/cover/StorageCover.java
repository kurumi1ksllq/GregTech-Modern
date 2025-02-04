package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.machine.MachineCoverContainer;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.GridLayout;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.util.SlotGenerator;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StorageCover extends CoverBehavior implements IUICover {

    @Persisted
    @DescSynced
    public final CustomItemStackHandler inventory;
    private final int SIZE = 18;

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(StorageCover.class,
            CoverBehavior.MANAGED_FIELD_HOLDER);

    public StorageCover(@NotNull CoverDefinition definition, @NotNull ICoverable coverableView,
                        @NotNull Direction attachedSide) {
        super(definition, coverableView, attachedSide);
        inventory = new CustomItemStackHandler(SIZE) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
    }

    @Override
    @NotNull
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    @NotNull
    public List<ItemStack> getAdditionalDrops() {
        var list = super.getAdditionalDrops();
        for (int slot = 0; slot < SIZE; slot++) {
            list.add(inventory.getStackInSlot(slot));
        }
        return list;
    }

    @Override
    public boolean canAttach() {
        if (!(coverHolder instanceof MachineCoverContainer)) return false;
        for (var dir : Direction.values()) {
            if (coverHolder.hasCover(dir) && coverHolder.getCoverAtSide(dir) instanceof StorageCover)
                return false;
        }
        return super.canAttach();
    }

    @Override
    public void loadServerUI(Player player, UIContainerMenu<CoverBehavior> menu, CoverBehavior holder) {
        var generator = SlotGenerator.begin(menu::addSlot, 0, 0);
        for (int slot = 0; slot < SIZE; slot++) {
            generator.slot(inventory, slot, 0, 0);
        }
    }

    @Override
    public ParentUIComponent createUIWidget(UIAdapter<StackLayout> adapter) {
        var menu = adapter.menu();

        final var group = UIContainers.stack(Sizing.fixed(126), Sizing.fixed(87));

        group.child(UIComponents.label(Component.translatable(getUITitle()))
                .positioning(Positioning.absolute(10, 5)));

        GridLayout grid = UIContainers.grid(Sizing.content(), Sizing.content(), 1, SIZE);
        grid.positioning(Positioning.absolute(7, 21));
        for (int slot = 0; slot < SIZE; slot++) {
            grid.child(UIComponents.slot(menu.getSlot(slot))
                    .positioning(Positioning.absolute((slot % 6) * 18, (slot / 6) * 18)),
                    0, slot);
        }
        group.child(grid);

        return group;
    }

    private String getUITitle() {
        return "cover.storage.title";
    }

    @Override
    public @Nullable IFancyConfigurator getConfigurator() {
        return new StorageCoverConfigurator();
    }

    private class StorageCoverConfigurator implements IFancyConfigurator {

        @Override
        public Component getTitle() {
            return Component.translatable("cover.storage.title");
        }

        @Override
        public UITexture getIcon() {
            return GuiTextures.MAINTENANCE_ICON;
        }

        @Override
        public UIComponent createConfigurator(UIAdapter<StackLayout> adapter) {
            final var group = UIContainers.stack(Sizing.fixed(126), Sizing.fixed(87));
            group.padding(Insets.both(7, 21));

            for (int slot = 0; slot < SIZE; slot++) {
                group.child(UIComponents.slot(inventory, slot)
                        .positioning(Positioning.absolute((slot % 6) * 18, (slot / 6) * 18)));
            }

            return group;
        }
    }
}
