package com.gregtechceu.gtceu.common.module;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.armor.IArmorLogic;
import com.gregtechceu.gtceu.api.item.module.AppliedItemModule;
import com.gregtechceu.gtceu.api.item.module.ArmorLogicItemModule;
import com.gregtechceu.gtceu.api.item.module.ITieredItemModule;
import com.gregtechceu.gtceu.common.item.armor.Jetpack;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public class JetpackModule extends ArmorLogicItemModule implements ITieredItemModule {

    private static final Jetpack JETPACK = new Jetpack(
            15,
            1_000_000L * (long) Math.max(1, Math.pow(4, ConfigHolder.INSTANCE.tools.voltageTierImpeller - 2)),
            ConfigHolder.INSTANCE.tools.voltageTierImpeller);

    public JetpackModule(ResourceLocation id) {
        super(id);
    }

    @Override
    protected @Nullable IArmorLogic getArmorLogic(AppliedItemModule module) {
        return JETPACK;
    }

    @Override
    public int getTier() {
        return GTValues.HV;
    }
}
