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
import net.minecraftforge.common.Tags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GTTool {

    public enum ToolGroup {
        MATERIAL,
        VANILLA_INWORLD,
        CRAFTING
    }

    public enum ToolType {
        MANUAL,
        MANUAL_SPECIAL,
        ELECTRIC
    }

    public record ToolInfo(@Getter ToolGroup group, @Getter ToolType type, @Getter int priority) { };


    protected Int2ObjectArrayMap<ItemEntryList> items = new Int2ObjectArrayMap<>();

    protected Map<ToolInfo, ItemEntryList> items2 = new HashMap<>();

    public GTTool(Material material) {
        ToolProperty property = material.getProperty(PropertyKey.TOOL);
        var tools = property.getTypes();

        if (property.hasType(GTToolType.PICKAXE)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.MANUAL, material, GTToolType.PICKAXE, 0);
        if (property.hasType(GTToolType.SHOVEL)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.MANUAL, material, GTToolType.SHOVEL, 1);
        if (property.hasType(GTToolType.AXE)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.MANUAL, material, GTToolType.AXE, 2);
        if (property.hasType(GTToolType.SWORD)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.MANUAL, material, GTToolType.SWORD, 3);
        if (property.hasType(GTToolType.HOE)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.MANUAL, material, GTToolType.HOE, 4);
        if (property.hasType(GTToolType.BUTCHERY_KNIFE)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.MANUAL_SPECIAL, material, GTToolType.BUTCHERY_KNIFE, 3);
        if (property.hasType(GTToolType.BUZZSAW)) addToItems(ToolGroup.CRAFTING, ToolType.ELECTRIC, material, GTToolType.BUZZSAW, 3);
        if (property.hasType(GTToolType.CHAINSAW_LV)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.ELECTRIC, material, GTToolType.CHAINSAW_LV, 2);
        if (property.hasType(GTToolType.CROWBAR)) addToItems(ToolGroup.CRAFTING, ToolType.MANUAL, material, GTToolType.CROWBAR, 5);
        if (property.hasType(GTToolType.DRILL_LV)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.ELECTRIC, material, GTToolType.DRILL_LV, 0);
        if (property.hasType(GTToolType.DRILL_MV)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.ELECTRIC, material, GTToolType.DRILL_MV, 0);
        if (property.hasType(GTToolType.DRILL_HV)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.ELECTRIC, material, GTToolType.DRILL_HV, 0);
        if (property.hasType(GTToolType.DRILL_EV)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.ELECTRIC, material, GTToolType.DRILL_EV, 0);
        if (property.hasType(GTToolType.DRILL_IV)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.ELECTRIC, material, GTToolType.DRILL_IV, 0);
        if (property.hasType(GTToolType.FILE)) addToItems(ToolGroup.CRAFTING, ToolType.MANUAL, material, GTToolType.FILE, 4);
        if (property.hasType(GTToolType.HARD_HAMMER)) addToItems(ToolGroup.CRAFTING, ToolType.MANUAL, material, GTToolType.HARD_HAMMER, 6);
        if (property.hasType(GTToolType.KNIFE)) addToItems(ToolGroup.CRAFTING, ToolType.MANUAL_SPECIAL, material, GTToolType.KNIFE, 3);
        if (property.hasType(GTToolType.MINING_HAMMER)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.MANUAL_SPECIAL, material, GTToolType.MINING_HAMMER, 0);
        if (property.hasType(GTToolType.MORTAR)) addToItems(ToolGroup.CRAFTING, ToolType.MANUAL_SPECIAL, material, GTToolType.MORTAR, 0);
        if (property.hasType(GTToolType.PLUNGER)) addToItems(ToolGroup.CRAFTING, ToolType.MANUAL_SPECIAL, material, GTToolType.PLUNGER, 1);
        if (property.hasType(GTToolType.SAW)) addToItems(ToolGroup.CRAFTING, ToolType.MANUAL, material, GTToolType.SAW, 3);
        if (property.hasType(GTToolType.SCREWDRIVER)) addToItems(ToolGroup.CRAFTING, ToolType.MANUAL, material, GTToolType.SCREWDRIVER, 1);
        if (property.hasType(GTToolType.SCREWDRIVER_LV)) addToItems(ToolGroup.CRAFTING, ToolType.ELECTRIC, material, GTToolType.SCREWDRIVER_LV, 1);
        if (property.hasType(GTToolType.SCYTHE)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.MANUAL_SPECIAL, material, GTToolType.SCYTHE, 4);
        if (property.hasType(GTToolType.SHEARS)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.MANUAL, material, GTToolType.SHEARS, 5);
        if (property.hasType(GTToolType.SOFT_MALLET)) addToItems(ToolGroup.CRAFTING, ToolType.MANUAL_SPECIAL, material, GTToolType.SOFT_MALLET, 2);
        if (property.hasType(GTToolType.SPADE)) addToItems(ToolGroup.VANILLA_INWORLD, ToolType.MANUAL_SPECIAL, material, GTToolType.SPADE, 1);
        if (property.hasType(GTToolType.WIRE_CUTTER)) addToItems(ToolGroup.CRAFTING, ToolType.MANUAL, material, GTToolType.WIRE_CUTTER, 2);
        if (property.hasType(GTToolType.WIRE_CUTTER_LV)) addToItems(ToolGroup.CRAFTING, ToolType.ELECTRIC, material, GTToolType.WIRE_CUTTER_LV, 2);
        if (property.hasType(GTToolType.WIRE_CUTTER_HV)) addToItems(ToolGroup.CRAFTING, ToolType.ELECTRIC, material, GTToolType.WIRE_CUTTER_HV, 2);
        if (property.hasType(GTToolType.WIRE_CUTTER_IV)) addToItems(ToolGroup.CRAFTING, ToolType.ELECTRIC, material, GTToolType.WIRE_CUTTER_IV, 2);
        if (property.hasType(GTToolType.WRENCH)) addToItems(ToolGroup.CRAFTING, ToolType.MANUAL, material, GTToolType.WRENCH, 0);
        if (property.hasType(GTToolType.WRENCH_LV)) addToItems(ToolGroup.CRAFTING, ToolType.ELECTRIC, material, GTToolType.WRENCH_LV, 0);
        if (property.hasType(GTToolType.WRENCH_HV)) addToItems(ToolGroup.CRAFTING, ToolType.ELECTRIC, material, GTToolType.WRENCH_HV, 0);
        if (property.hasType(GTToolType.WRENCH_IV)) addToItems(ToolGroup.CRAFTING, ToolType.ELECTRIC, material, GTToolType.WRENCH_IV, 0);

        if (material.hasProperty(PropertyKey.INGOT)) addToItems(ToolGroup.MATERIAL, ToolType.MANUAL, ChemicalHelper.get(TagPrefix.ingot, material), 0);
        if (material.hasProperty(PropertyKey.GEM)) addToItems(ToolGroup.MATERIAL, ToolType.MANUAL, ChemicalHelper.get(TagPrefix.gem, material), 0);
        if (material.hasProperty(PropertyKey.WOOD)) addToItems(ToolGroup.MATERIAL, ToolType.MANUAL, ItemTags.PLANKS, 0);
    }

    private void addToItems(ToolGroup group, ToolType type, Material material, GTToolType toolType, int priority) {
        var stack = ToolHelper.get(toolType, material);
        addToItems(group, type, stack, priority);
    }

    private void addToItems(ToolGroup group, ToolType type, ItemStack stack, int priority) {
        items2.merge(new ToolInfo(group, type, priority), ItemStackList.of(stack), (list, s) -> {
            ((ItemStackList) list).addAll(s.getStacks());
            return list;
        });
    }

    private void addToItems(ToolGroup group, ToolType type, TagKey<Item> itemTag, int priority) {
        items2.merge(new ToolInfo(group, type, priority), ItemTagList.of(itemTag, 1, null), (list, s) -> {
            ((ItemStackList) list).addAll(s.getStacks());
            return list;
        });
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
