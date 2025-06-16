package com.gregtechceu.gtceu.api.multiblock.error;

import com.gregtechceu.gtceu.api.block.IFilterType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

public class FilterMatchingError extends PatternError {

    IFilterType coilType1, coilType2;

    public FilterMatchingError(BlockPos pos, IFilterType type1, IFilterType type2) {
        super(pos, Collections.emptyList());
        coilType1 = type1;
        coilType2 = type2;
    }

    @Override
    public List<Component> getErrorInfo() {
        return List.of(Component.literal("Mismatched filters: " + coilType1.getCleanroomType().getName() + " vs \n" +
                coilType2.getCleanroomType().getName() + " at: " + pos.getX() + " " + pos.getY() + " " + pos.getZ()));
    }
}
