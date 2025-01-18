package com.gregtechceu.gtceu.integration;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemEntryList;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemEntryHandler;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;

import it.unimi.dsi.fastutil.Pair;

import java.util.*;

public class GTToolWidget extends WidgetGroup {

    protected static LinkedHashMap<Integer, Pair<Integer, Integer>> SLOT_LOCS = new LinkedHashMap<>();

    static {
        SLOT_LOCS.put(0, Pair.of(29, 2)); // pickaxe
        SLOT_LOCS.put(1, Pair.of(29, 20)); // shovel
        SLOT_LOCS.put(2, Pair.of(29, 38)); // axe
        SLOT_LOCS.put(3, Pair.of(29, 56)); // sword
        SLOT_LOCS.put(4, Pair.of(29, 74)); // hoe
        SLOT_LOCS.put(5, Pair.of(47, 56)); // butchery knife
        SLOT_LOCS.put(6, Pair.of(128, 56)); // buzz saw
        SLOT_LOCS.put(7, Pair.of(47, 38)); // chainsaw
        SLOT_LOCS.put(8, Pair.of(110, 92)); // crowbar
        SLOT_LOCS.put(9, Pair.of(65, 2)); // electric drills
        SLOT_LOCS.put(10, Pair.of(110, 74)); // file
        SLOT_LOCS.put(11, Pair.of(110, 110)); // hard hammer
        SLOT_LOCS.put(12, Pair.of(47, 56)); // knife
        SLOT_LOCS.put(13, Pair.of(47, 2)); // mining hammer
        SLOT_LOCS.put(14, Pair.of(92, 2)); // mortar
        SLOT_LOCS.put(15, Pair.of(92, 20)); // plunger
        SLOT_LOCS.put(16, Pair.of(110, 56)); // saw
        SLOT_LOCS.put(17, Pair.of(110, 20)); // screwdriver
        SLOT_LOCS.put(18, Pair.of(128, 20)); // screwdriver lv
        SLOT_LOCS.put(19, Pair.of(47, 74)); // scythe
        SLOT_LOCS.put(20, Pair.of(29, 92)); // shears
        SLOT_LOCS.put(21, Pair.of(92, 38)); // soft mallet
        SLOT_LOCS.put(22, Pair.of(47, 20)); // spade
        SLOT_LOCS.put(23, Pair.of(110, 38)); // wire cutter
        SLOT_LOCS.put(24, Pair.of(128, 38)); // electric wire cutters
        SLOT_LOCS.put(25, Pair.of(110, 2)); // wrench
        SLOT_LOCS.put(26, Pair.of(128, 2)); // electric wrenches
        SLOT_LOCS.put(40, Pair.of(2, 2)); // main material
    }

    public GTToolWidget(Material material) {
        super(0, 0, 176, 166);
        setClientSideWidget();
        setRecipe(new GTTool(material));
    }

    public void setRecipe(GTTool recipeWrapper) {
        WidgetGroup itemGroup = new WidgetGroup();


        //if(recipeWrapper.items.isEmpty()) return;

        List<ItemEntryList> items = new ArrayList<>();
        boolean hasInworld = false;
        for(var g : recipeWrapper.items2) {
            if(g.g == GTTool.Group.VANILLA_INWORLD) hasInworld = true;
            for(var t : g.types) {
                t.itemEntries.forEach((p, v) -> items.add(v));
            }
        }
        //recipeWrapper.items2.forEach((key, value) -> items.add(value));

        CycleItemEntryHandler itemHandler = new CycleItemEntryHandler(items);

        boolean hasVanilla = false, hasCraftingManual = false;
        int lowestPriority = Integer.MAX_VALUE;

        int slotIndex = 0;
        for(var group : recipeWrapper.items2) {
            for(var type : group.types) {
                final int[] lowestP = {Integer.MAX_VALUE};
                type.itemEntries.forEach((key, value) -> lowestP[0] = Math.min(key, lowestP[0]));
                for(var entry : type.itemEntries.entrySet()) {
                    int x = 0;
                    switch(group.g) {
                        case MATERIAL -> x += 0;
                        case VANILLA_INWORLD -> x += 27 + type.t.ordinal() * 18;
                        case CRAFTING -> {
                            x += 27 + (hasInworld ? recipeWrapper.getToolGroup(GTTool.Group.VANILLA_INWORLD).types.size() * 18 + 27 : 0);
                            x += switch(type.t) {
                                case MANUAL -> 0;
                                case MANUAL_SPECIAL -> (group.types.size() > 1 ? -18 : 0);
                                case ELECTRIC -> (group.types.size() > 1 ? 18 : 0);
                            };
                        }
                    }

                    int y = Math.max(0, (entry.getKey() - lowestP[0]) * 18);

                    int finalSlotIndex = slotIndex;
                    itemGroup = itemGroup
                            .addWidget(new SlotWidget(itemHandler, slotIndex, x, y)
                                    .setCanTakeItems(false).setCanPutItems(false)
                                    .setIngredientIO(IngredientIO.INPUT)
                                    .setOnAddedTooltips((slot, tooltip) -> recipeWrapper.getTooltip(finalSlotIndex, tooltip))
                                    .setBackground(GuiTextures.SLOT));
                    slotIndex++;
                }
            }
        }
            /*var x = 0;
            var y = 0;
            var info = entry.getKey();
            if(info.getGroup() == GTTool.Group.VANILLA_INWORLD) {
                x += 27;
                if (info.getType() == GTTool.Type.MANUAL_SPECIAL) {
                    x += 18;
                } else if (info.getType() == GTTool.Type.ELECTRIC) {
                    x += 36;
                }
            } else if(info.getGroup() == GTTool.Group.CRAFTING) {
                x += 9  + (hasCraftingManual ? 18 : 0);
                x += hasVanilla ? groupSize.get(1) : 0;
                if (info.getType() == GTTool.Type.MANUAL_SPECIAL) {
                    x -= 18;
                } else if (info.getType() == GTTool.Type.ELECTRIC) {
                    x += 18;
                }
            }

            y += 18 * Math.min(0, (info.getPriority() - lowestPriority));

            int finalSlotIndex = slotIndex;
            itemGroup = itemGroup
                    .addWidget(new SlotWidget(itemHandler, slotIndex, x, y)
                            .setCanTakeItems(false).setCanPutItems(false)
                            .setIngredientIO(IngredientIO.INPUT)
                            .setOnAddedTooltips((slot, tooltip) -> recipeWrapper.getTooltip(finalSlotIndex, tooltip))
                            .setBackground(GuiTextures.SLOT));
            slotIndex++;
        }*/



        //List<ItemEntryList> items = recipeWrapper.items.int2ObjectEntrySet().stream().map(Map.Entry::getValue).toList();

        /*for (var entry : SLOT_LOCS.entrySet()) {
            int x = entry.getValue().left(), y = entry.getValue().right();
            if(recipeWrapper.items.containsKey(entry.getKey())) {
                int finalSlotIndex = slotIndex;
                itemGroup = itemGroup
                        .addWidget(new SlotWidget(itemHandler, slotIndex, x, y)
                                .setCanTakeItems(false).setCanPutItems(false)
                                .setIngredientIO(IngredientIO.INPUT)
                                .setOnAddedTooltips((slot, tooltip) -> recipeWrapper.getTooltip(finalSlotIndex, tooltip))
                                .setBackground(GuiTextures.SLOT));

            } else  {
                //itemGroup.addWidget(new ImageWidget(x, y, 18, 18, GuiTextures.SLOT));
            }

        }*/

        this.addWidget(itemGroup);
    }
}
