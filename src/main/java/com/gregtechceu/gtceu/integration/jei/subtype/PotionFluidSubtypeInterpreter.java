package com.gregtechceu.gtceu.integration.jei.subtype;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

/* From JEI's Potion item subtype interpreter */
public class PotionFluidSubtypeInterpreter implements IIngredientSubtypeInterpreter<FluidStack> {

    @Override
    public String apply(FluidStack ingredient, UidContext context) {
        if (!ingredient.has(DataComponents.POTION_CONTENTS))
            return IIngredientSubtypeInterpreter.NONE;

        PotionContents potion = ingredient.get(DataComponents.POTION_CONTENTS);
        Optional<Holder<Potion>> potionType = potion.potion();
        String potionTypeString = Potion.getName(potionType, "");

        StringBuilder stringBuilder = new StringBuilder(potionTypeString);
        List<MobEffectInstance> effects = potion.customEffects();

        for (MobEffectInstance effect : potionType.get().value().getEffects()) {
            stringBuilder.append(";")
                    .append(effect);
        }
        for (MobEffectInstance effect : effects) {
            stringBuilder.append(";")
                    .append(effect);
        }
        return stringBuilder.toString();
    }
}
