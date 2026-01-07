package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.component.SpoilContext;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Shadow
    public abstract ItemStack getItem();

    @Unique
    private boolean gtceu$wasOnGround = ((Entity) (Object) this).onGround();

    @Inject(method = "tick", at = @At(value = "HEAD"))
    public void beforeTick(CallbackInfo ci) {
        ItemEntity entity = (ItemEntity) (Object) this;
        if (GTValues.FOOLS.getAsBoolean() && !gtceu$wasOnGround && entity.onGround() &&
                ChemicalHelper.getPrefix(entity.getItem().getItem()) == TagPrefix.rodLong) {
            GTSoundEntries.METAL_PIPE.playFrom(entity, 10, 1);
        }
        gtceu$wasOnGround = entity.onGround();

        getItem().getCapability(GTCapability.CAPABILITY_SPOILABLE_ITEM)
                .ifPresent(spoilable -> spoilable.updateFreshness(new SpoilContext(entity), true));
    }
}
