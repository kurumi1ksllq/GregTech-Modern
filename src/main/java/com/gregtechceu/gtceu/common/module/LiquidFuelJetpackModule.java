package com.gregtechceu.gtceu.common.module;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.armor.IArmorLogic;
import com.gregtechceu.gtceu.api.item.module.AppliedItemModule;
import com.gregtechceu.gtceu.api.item.module.ArmorLogicItemModule;
import com.gregtechceu.gtceu.api.item.module.ITieredItemModule;
import com.gregtechceu.gtceu.common.item.armor.PowerlessJetpack;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public class LiquidFuelJetpackModule extends ArmorLogicItemModule implements ITieredItemModule {

    private static final PowerlessJetpack JETPACK = new PowerlessJetpack();

    public LiquidFuelJetpackModule(ResourceLocation id) {
        super(id);
    }

    @Override
    protected @Nullable IArmorLogic getArmorLogic(AppliedItemModule module) {
        return JETPACK;
    }

    @Override
    public int getTier() {
        return GTValues.LV;
    }
}
