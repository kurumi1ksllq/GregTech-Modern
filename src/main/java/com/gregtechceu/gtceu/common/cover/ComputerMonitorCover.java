package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.capability.gregtech.ICoverableBlock;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;

import net.minecraft.core.Direction;

public class ComputerMonitorCover extends CoverBehavior {

    public ComputerMonitorCover(CoverDefinition definition, ICoverableBlock coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    @Override
    public boolean canPipePassThrough() {
        return false;
    }

    // No implementation here, this cover is just for decorative purposes
}
