package com.gregtechceu.gtceu.api.capability.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.content.SerializerInteger;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * @author KilaBash
 * @date 2023/2/20
 * @implNote ItemRecipeCapability
 */
public class CWURecipeCapability extends RecipeCapability<Integer> {

    public final static CWURecipeCapability CAP = new CWURecipeCapability();

    protected CWURecipeCapability() {
        super("cwu", 0xFFEEEE00, false, 3, SerializerInteger.INSTANCE);
    }

    @Override
    public Integer copyInner(Integer content) {
        return content;
    }

    @Override
    public Integer copyWithModifier(Integer content, ContentModifier modifier) {
        return modifier.apply(content);
    }

    @Override
    public void addXEIInfo(FlowLayout group, GTRecipe recipe, List<Content> contents, boolean perTick,
                           boolean isInput) {
        if (perTick) {
            int cwu = contents.stream().map(Content::getContent).mapToInt(CWURecipeCapability.CAP::of).sum();
            group.child(UIComponents.label(Component.translatable("gtceu.recipe.computation_per_tick", cwu)));
        }
        if (recipe.data.getBoolean("duration_is_total_cwu")) {
            group.child(UIComponents.label(Component.translatable("gtceu.recipe.total_computation", recipe.duration)));
        }
    }
}
