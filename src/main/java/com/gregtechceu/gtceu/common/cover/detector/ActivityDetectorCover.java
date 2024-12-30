package com.gregtechceu.gtceu.common.cover.detector;

import com.gregtechceu.gtceu.api.capability.gregtech.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.gregtech.ICoverableBlock;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ActivityDetectorCover extends DetectorCover {

    public ActivityDetectorCover(CoverDefinition definition, ICoverableBlock coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    @Override
    public boolean canAttach() {
        return GTCapabilityHelper.getWorkable(coverHolder.getLevel(), coverHolder.getPos(), attachedSide) != null;
    }

    @Override
    protected void update() {
        if (this.coverHolder.getOffsetTimer() % 20 != 0) {
            return;
        }

        var workable = GTCapabilityHelper.getWorkable(coverHolder.getLevel(), coverHolder.getPos(), attachedSide);

        boolean isCurrentlyWorking = workable.isActive() && workable.isWorkingEnabled();

        setRedstoneSignalOutput(isCurrentlyWorking != isInverted() ? 15 : 0);
    }
}
