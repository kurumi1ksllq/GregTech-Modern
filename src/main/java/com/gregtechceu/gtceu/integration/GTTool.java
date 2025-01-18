package com.gregtechceu.gtceu.integration;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.ToolProperty;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemEntryList;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemStackList;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemTagList;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

import java.util.*;

public class GTTool {

    public enum Group {
        MATERIAL,
        VANILLA_INWORLD,
        CRAFTING
    }

    public class ToolGroup {
        public List<ToolType> types = new ArrayList<>();
        public Group g;

        public ToolGroup(Group g) {
            this.g = g;
        }
    }

    public enum Type {
        MANUAL,
        MANUAL_SPECIAL,
        ELECTRIC
    }

    public class ToolType {
        public Map<Integer, ItemEntryList> itemEntries = new HashMap<>();
        public Type t;

        public ToolType(Type t) {
            this.t = t;
        }
    }


    protected Int2ObjectArrayMap<ItemEntryList> items = new Int2ObjectArrayMap<>();

    protected List<ToolGroup> items2 = new ArrayList<>();

    public ToolGroup getToolGroup(Group g) {
        for(var gr : items2) {
            if(gr.g == g) return gr;
        }
        return null;
    }

    public GTTool(Material material) {
        ToolProperty property = material.getProperty(PropertyKey.TOOL);
        var tools = property.getTypes();

        if (property.hasType(GTToolType.PICKAXE)) addToItems(Group.VANILLA_INWORLD, Type.MANUAL, material, GTToolType.PICKAXE, 0);
        if (property.hasType(GTToolType.SHOVEL)) addToItems(Group.VANILLA_INWORLD, Type.MANUAL, material, GTToolType.SHOVEL, 1);
        if (property.hasType(GTToolType.AXE)) addToItems(Group.VANILLA_INWORLD, Type.MANUAL, material, GTToolType.AXE, 2);
        if (property.hasType(GTToolType.SWORD)) addToItems(Group.VANILLA_INWORLD, Type.MANUAL, material, GTToolType.SWORD, 3);
        if (property.hasType(GTToolType.HOE)) addToItems(Group.VANILLA_INWORLD, Type.MANUAL, material, GTToolType.HOE, 4);
        if (property.hasType(GTToolType.BUTCHERY_KNIFE)) addToItems(Group.VANILLA_INWORLD, Type.MANUAL_SPECIAL, material, GTToolType.BUTCHERY_KNIFE, 3);
        if (property.hasType(GTToolType.BUZZSAW)) addToItems(Group.CRAFTING, Type.ELECTRIC, material, GTToolType.BUZZSAW, 3);
        if (property.hasType(GTToolType.CHAINSAW_LV)) addToItems(Group.VANILLA_INWORLD, Type.MANUAL_SPECIAL, material, GTToolType.CHAINSAW_LV, 2);
        if (property.hasType(GTToolType.CROWBAR)) addToItems(Group.CRAFTING, Type.MANUAL, material, GTToolType.CROWBAR, 5);
        if (property.hasType(GTToolType.DRILL_LV)) addToItems(Group.VANILLA_INWORLD, Type.ELECTRIC, material, GTToolType.DRILL_LV, 0);
        if (property.hasType(GTToolType.DRILL_MV)) addToItems(Group.VANILLA_INWORLD, Type.ELECTRIC, material, GTToolType.DRILL_MV, 0);
        if (property.hasType(GTToolType.DRILL_HV)) addToItems(Group.VANILLA_INWORLD, Type.ELECTRIC, material, GTToolType.DRILL_HV, 0);
        if (property.hasType(GTToolType.DRILL_EV)) addToItems(Group.VANILLA_INWORLD, Type.ELECTRIC, material, GTToolType.DRILL_EV, 0);
        if (property.hasType(GTToolType.DRILL_IV)) addToItems(Group.VANILLA_INWORLD, Type.ELECTRIC, material, GTToolType.DRILL_IV, 0);
        if (property.hasType(GTToolType.FILE)) addToItems(Group.CRAFTING, Type.MANUAL, material, GTToolType.FILE, 4);
        if (property.hasType(GTToolType.HARD_HAMMER)) addToItems(Group.CRAFTING, Type.MANUAL, material, GTToolType.HARD_HAMMER, 6);
        if (property.hasType(GTToolType.KNIFE)) addToItems(Group.CRAFTING, Type.MANUAL_SPECIAL, material, GTToolType.KNIFE, 3);
        if (property.hasType(GTToolType.MINING_HAMMER)) addToItems(Group.VANILLA_INWORLD, Type.MANUAL_SPECIAL, material, GTToolType.MINING_HAMMER, 0);
        if (property.hasType(GTToolType.MORTAR)) addToItems(Group.CRAFTING, Type.MANUAL_SPECIAL, material, GTToolType.MORTAR, 0);
        if (property.hasType(GTToolType.PLUNGER)) addToItems(Group.CRAFTING, Type.MANUAL_SPECIAL, material, GTToolType.PLUNGER, 1);
        if (property.hasType(GTToolType.SAW)) addToItems(Group.CRAFTING, Type.MANUAL, material, GTToolType.SAW, 3);
        if (property.hasType(GTToolType.SCREWDRIVER)) addToItems(Group.CRAFTING, Type.MANUAL, material, GTToolType.SCREWDRIVER, 1);
        if (property.hasType(GTToolType.SCREWDRIVER_LV)) addToItems(Group.CRAFTING, Type.ELECTRIC, material, GTToolType.SCREWDRIVER_LV, 1);
        if (property.hasType(GTToolType.SCYTHE)) addToItems(Group.VANILLA_INWORLD, Type.MANUAL_SPECIAL, material, GTToolType.SCYTHE, 4);
        if (property.hasType(GTToolType.SHEARS)) addToItems(Group.VANILLA_INWORLD, Type.MANUAL, material, GTToolType.SHEARS, 5);
        if (property.hasType(GTToolType.SOFT_MALLET)) addToItems(Group.CRAFTING, Type.MANUAL_SPECIAL, material, GTToolType.SOFT_MALLET, 2);
        if (property.hasType(GTToolType.SPADE)) addToItems(Group.VANILLA_INWORLD, Type.MANUAL_SPECIAL, material, GTToolType.SPADE, 1);
        if (property.hasType(GTToolType.WIRE_CUTTER)) addToItems(Group.CRAFTING, Type.MANUAL, material, GTToolType.WIRE_CUTTER, 2);
        if (property.hasType(GTToolType.WIRE_CUTTER_LV)) addToItems(Group.CRAFTING, Type.ELECTRIC, material, GTToolType.WIRE_CUTTER_LV, 2);
        if (property.hasType(GTToolType.WIRE_CUTTER_HV)) addToItems(Group.CRAFTING, Type.ELECTRIC, material, GTToolType.WIRE_CUTTER_HV, 2);
        if (property.hasType(GTToolType.WIRE_CUTTER_IV)) addToItems(Group.CRAFTING, Type.ELECTRIC, material, GTToolType.WIRE_CUTTER_IV, 2);
        if (property.hasType(GTToolType.WRENCH)) addToItems(Group.CRAFTING, Type.MANUAL, material, GTToolType.WRENCH, 0);
        if (property.hasType(GTToolType.WRENCH_LV)) addToItems(Group.CRAFTING, Type.ELECTRIC, material, GTToolType.WRENCH_LV, 0);
        if (property.hasType(GTToolType.WRENCH_HV)) addToItems(Group.CRAFTING, Type.ELECTRIC, material, GTToolType.WRENCH_HV, 0);
        if (property.hasType(GTToolType.WRENCH_IV)) addToItems(Group.CRAFTING, Type.ELECTRIC, material, GTToolType.WRENCH_IV, 0);

        if (material.hasProperty(PropertyKey.INGOT)) addToItems(Group.MATERIAL, Type.MANUAL, ChemicalHelper.get(TagPrefix.ingot, material), 0);
        if (material.hasProperty(PropertyKey.GEM)) addToItems(Group.MATERIAL, Type.MANUAL, ChemicalHelper.get(TagPrefix.gem, material), 0);
        if (material.hasProperty(PropertyKey.WOOD)) addToItems(Group.MATERIAL, Type.MANUAL, ItemTags.PLANKS, 0);
    }

    private void addToItems(Group group, Type type, Material material, GTToolType toolType, int priority) {
        var stack = ToolHelper.get(toolType, material);
        addToItems(group, type, stack, priority);
    }

    private void addToItems(Group group, Type type, ItemStack stack, int priority) {
        boolean hasGroup = false;
        for(var g : items2) {
            if(g.g == group) {
                hasGroup = true;
                boolean hasType = false;
                for(var t : g.types) {
                    if(t.t == type) {
                        hasType = true;
                        t.itemEntries.merge(priority, ItemStackList.of(stack), (list, s) -> {
                            ((ItemStackList) list).addAll(s.getStacks());
                            return list;
                        });
                    }
                }
                if(!hasType) {
                    ToolType t = new ToolType(type);
                    t.itemEntries.merge(priority, ItemStackList.of(stack), (list, s) -> {
                        ((ItemStackList) list).addAll(s.getStacks());
                        return list;
                    });
                    g.types.add(t);
                }
            }
        }

        if(!hasGroup) {
            ToolType t = new ToolType(type);
            ToolGroup g = new ToolGroup(group);
            t.itemEntries.merge(priority, ItemStackList.of(stack), (list, s) -> {
                ((ItemStackList) list).addAll(s.getStacks());
                return list;
            });
            g.types.add(t);
            items2.add(g);
        }
    }

    private void addToItems(Group group, Type type, TagKey<Item> itemTag, int priority) {
        boolean hasGroup = false;
        for(var g : items2) {
            if(g.g == group) {
                hasGroup = true;
                boolean hasType = false;
                for(var t : g.types) {
                    if(t.t == type) {
                        hasType = true;
                        t.itemEntries.merge(priority, ItemTagList.of(itemTag, 1, null), (list, s) -> {
                            ((ItemStackList) list).addAll(s.getStacks());
                            return list;
                        });
                    }
                }
                if(!hasType) {
                    ToolType t = new ToolType(type);
                    t.itemEntries.merge(priority, ItemTagList.of(itemTag, 1, null), (list, s) -> {
                        ((ItemStackList) list).addAll(s.getStacks());
                        return list;
                    });
                    g.types.add(t);
                }
            }
        }

        if(!hasGroup) {
            ToolType t = new ToolType(type);
            ToolGroup g = new ToolGroup(group);
            t.itemEntries.merge(priority, ItemTagList.of(itemTag, 1, null), (list, s) -> {
                ((ItemStackList) list).addAll(s.getStacks());
                return list;
            });
            g.types.add(t);
            items2.add(g);
        }
    }

    private void addToItems(int index, Material material, GTToolType type) {
        addToItems(index, ToolHelper.get(type, material));
    }

    private void addToItems(int index, ItemStack stack) {
        items.merge(index, ItemStackList.of(stack), (list, s) -> {
            ((ItemStackList) list).addAll(s.getStacks());
            return list;
        });
    }

    private void addToItems(int index, TagKey<Item> tag) {
        items.merge(index, ItemTagList.of(tag, 1, null), (list, s) -> {
            ((ItemStackList) list).addAll(s.getStacks());
            return list;
        });
    }

    public void getTooltip(int slotIndex, List<Component> tooltips) {}
}
