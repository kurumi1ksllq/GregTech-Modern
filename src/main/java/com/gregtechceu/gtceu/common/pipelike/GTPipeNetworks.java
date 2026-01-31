package com.gregtechceu.gtceu.common.pipelike;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.pipenet.PipeNetworkType;

public class GTPipeNetworks {

    public static PipeNetworkType FLUID = new PipeNetworkType(GTCEu.id("fluid"));
    public static PipeNetworkType ITEM = new PipeNetworkType(GTCEu.id("item_pipe"));
    public static PipeNetworkType ENERGY = new PipeNetworkType(GTCEu.id("energy"));
    public static PipeNetworkType DUCT = new PipeNetworkType(GTCEu.id("duct"));
    public static PipeNetworkType LASER = new PipeNetworkType(GTCEu.id("laser"));
    public static PipeNetworkType OPTICAL = new PipeNetworkType(GTCEu.id("optical"));
}
