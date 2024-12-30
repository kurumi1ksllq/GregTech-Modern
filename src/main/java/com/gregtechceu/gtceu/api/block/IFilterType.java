package com.gregtechceu.gtceu.api.block;

import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;

import net.minecraft.util.StringRepresentable;

import org.jetbrains.annotations.NotNull;

/**
 * IFilterType is an interface that provides methods to get the properties of Filters.
 * This is useful for Filters that have different properties based on the type of filter.
 * For example, a Filter that provides different cleanroom types based on the level of the filter.
 * @see CleanroomType
 */
public interface IFilterType extends StringRepresentable {
    /**
     * Get the cleanroom type of this filter.
     * @return The cleanroom type of this filter.
     */
    @NotNull
    CleanroomType getCleanroomType();
}