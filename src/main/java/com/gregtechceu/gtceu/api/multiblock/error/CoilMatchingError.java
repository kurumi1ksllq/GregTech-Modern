package com.gregtechceu.gtceu.api.multiblock.error;

import com.gregtechceu.gtceu.api.block.ICoilType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

public class CoilMatchingError extends PatternError {

    ICoilType coilType1, coilType2;

    public CoilMatchingError(BlockPos pos, ICoilType type1, ICoilType type2) {
        super(pos, Collections.emptyList());
        coilType1 = type1;
        coilType2 = type2;
    }

    @Override
    public List<Component> getErrorInfo() {
        return List.of(Component.literal("Mismatched coils: " + coilType1.getMaterial().getName() + " vs \n" +
                coilType2.getMaterial().getName() + " at: " + pos.getX() + " " + pos.getY() + " " + pos.getZ()));
    }
}
