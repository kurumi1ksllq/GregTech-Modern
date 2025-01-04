package com.gregtechceu.gtceu.common.pipelike.net.item;

import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.transfer.item.ItemHandlerList;

import net.minecraftforge.items.IItemHandler;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public record ItemNetworkView(ItemHandlerList handler, BiMap<IItemHandler, NetNode> handlerNetNodeBiMap) {

    public static final ItemNetworkView EMPTY = ItemNetworkView.of(ImmutableBiMap.of());

    public static ItemNetworkView of(BiMap<IItemHandler, NetNode> handlerNetNodeBiMap) {
        return new ItemNetworkView(new ItemHandlerList(handlerNetNodeBiMap.keySet()), handlerNetNodeBiMap);
    }
}
