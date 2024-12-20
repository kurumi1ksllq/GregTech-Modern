package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.container.*;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;

import net.minecraft.world.entity.player.Inventory;

import org.w3c.dom.Element;

public class PlayerInventoryComponent extends StackLayout {

    protected PlayerInventoryComponent(Inventory inventory, UITexture slotTexture) {
        super(Sizing.fixed(162), Sizing.fixed(76));
        setByInventory(inventory, slotTexture);
    }

    protected PlayerInventoryComponent() {
        super(Sizing.fixed(162), Sizing.fixed(76));
    }

    public PlayerInventoryComponent setByInventory(Inventory inventory, UITexture slotTexture) {
        GridLayout grid = UIContainers.grid(Sizing.content(), Sizing.content(), 3, 9);
        grid.positioning(Positioning.absolute(0, 0));
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                // .positioning(Positioning.absolute(x * 18, y * 18))
                grid.child(UIComponents.slot(inventory, x + y * 9 + 9)
                        .backgroundTexture(slotTexture),
                        y, x);
            }
        }
        this.child(grid);

        var grid2 = UIContainers.grid(Sizing.content(), Sizing.content(), 1, 9);
        grid2.positioning(Positioning.absolute(0, 58));
        for (int x = 0; x < 9; x++) {
            // .positioning(Positioning.absolute(x * 18, 0))
            grid2.child(UIComponents.slot(inventory, x)
                    .backgroundTexture(slotTexture),
                    0, x);
        }
        this.child(grid2);
        return this;
    }

    public static PlayerInventoryComponent parse(Element element) {
        return new PlayerInventoryComponent();
    }
}
