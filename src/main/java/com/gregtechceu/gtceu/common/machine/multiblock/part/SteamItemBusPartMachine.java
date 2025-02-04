package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.GridLayout;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.world.entity.player.Player;

public class SteamItemBusPartMachine extends ItemBusPartMachine {

    private final String autoTooltipKey;

    public SteamItemBusPartMachine(IMachineBlockEntity holder, IO io, Object... args) {
        super(holder, 1, io, args);
        autoTooltipKey = io == IO.IN ? "gtceu.gui.item_auto_input.tooltip" : "gtceu.gui.item_auto_output.tooltip";
    }

    @Override
    public void loadClientUI(Player player, UIAdapter<StackLayout> adapter, MetaMachine holder) {
        int rowSize = (int) Math.sqrt(getInventorySize());
        int xOffset = rowSize == 10 ? 9 : 0;

        FlowLayout group = UIContainers.horizontalFlow(Sizing.fixed(176 + xOffset * 2),
                Sizing.fixed(18 + 18 * rowSize + 94));
        group.surface(GuiTextures.BACKGROUND_STEAM.get(ConfigHolder.INSTANCE.machines.steelSteamMultiblocks)::draw);

        group.child(UIComponents.label(getBlockState().getBlock().getName())
                .positioning(Positioning.absolute(10, 5)))
                .child(UIComponents
                        .toggleButton(GuiTextures.BUTTON_ITEM_OUTPUT, this::isWorkingEnabled, this::setWorkingEnabled)
                        .shouldUseBaseBackground()
                        .setTooltipText("gtceu.gui.item_auto_input.tooltip")
                        .positioning(Positioning.absolute(2, 18 + 18 * rowSize + 12 - 20)))
                .child(UIComponents.playerInventory(player.getInventory(),
                        GuiTextures.SLOT_STEAM.get(ConfigHolder.INSTANCE.machines.steelSteamMultiblocks))
                        .positioning(Positioning.absolute(7 + xOffset, 18 + 18 * rowSize + 12)));

        GridLayout grid = UIContainers.grid(Sizing.content(), Sizing.content(), rowSize, rowSize);
        grid.positioning(Positioning.absolute(88 - rowSize * 9, 18));
        for (int y = 0; y < rowSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                int index = y * rowSize + x;
                grid.child(UIComponents.slot(getInventory().storage, index)
                        .canInsert(io.support(IO.IN))
                        .canExtract(true)
                        .backgroundTexture(GuiTextures.SLOT_STEAM
                                .get(ConfigHolder.INSTANCE.machines.steelSteamMultiblocks)),
                        x, y);
            }
        }

        adapter.rootComponent.child(group);
    }
}
