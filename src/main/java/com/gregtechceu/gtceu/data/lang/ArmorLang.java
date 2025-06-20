package com.gregtechceu.gtceu.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

import static com.gregtechceu.gtceu.data.lang.LangUtil.*;

public class ArmorLang {

    public static void init(RegistrateLangProvider provider) {
        generateArmorKeys(provider);
        generateHudKeys(provider);
        generateTooltipKeys(provider);
    }

    public static void generateArmorKeys(RegistrateLangProvider provider) {


        // nanomuscle suit
        provider.add("armor.gtceu.nms.nightvision.enabled", "§aNanoMuscle™ Suite: NightVision Enabled");
        provider.add("armor.gtceu.nms.nightvision.disabled", " §cNanoMuscle™ Suite: NightVision Disabled");
        provider.add("armor.gtceu.nms.boosted_jump.enabled", "§aNanoMuscle™ Suite: Jump Boost Enabled");
        provider.add("armor.gtceu.nms.boosted_jump.disabled", " §cNanoMuscle™ Suite: Jump Boost Disabled");
        provider.add("armor.gtceu.nms.nightvision.error", "NanoMuscle™ Suite: §cNot enough power!");
        provider.add("armor.gtceu.nms.charge.enabled", "§aNanoMuscle™ Suite: Charging Enabled");
        provider.add("armor.gtceu.nms.charge.disable", " §cNanoMuscle™ Suite: Charging Disabled");
        provider.add("armor.gtceu.nms.charge.error", "NanoMuscle™ Suite: §cNot enough power for charging!");


        // quantum suit
        provider.add("armor.gtceu.qts.nightvision.enabled", "§a§aQuarkTech™ Suite: NightVision Enabled");
        provider.add("armor.gtceu.qts.nightvision.disabled", " §cQuarkTech™ Suite: NightVision Disabled");
        provider.add("armor.gtceu.qts.nightvision.error", " §cQuarkTech™ Suite: §cNot enough power!");
        provider.add("armor.gtceu.qts.charge.enabled", "§aQuarkTech™ Suite: Charging Enabled");
        provider.add("armor.gtceu.qts.charge.disable", " §cQuarkTech™ Suite: Charging Disabled");
        provider.add("armor.gtceu.qts.charge.error", " §cQuarkTech™ Suite: §cNot enough power for charging!");


        //jetpacks
        provider.add("armor.gtceu.jetpack.flight.enable", "§aJetpack: Flight Enabled");
        provider.add("armor.gtceu.jetpack.flight.disable", " §cJetpack: Flight Disabled");
        provider.add("armor.gtceu.jetpack.hover.enable", "§aJetpack: Hover Mode Enabled");
        provider.add("armor.gtceu.jetpack.hover.disable", " §cJetpack: Hover Mode Disabled");
        provider.add("armor.gtceu.jetpack.emergency_hover_mode", "§aEmergency Hover Mode Enabled!");


        //action bar messages
        provider.add("armor.gtceu.message.nightvision.enabled", "§aNightVision: §aOn");
        provider.add("armor.gtceu.message.nightvision.disabled", " §cNightVision: §cOff");
        provider.add("armor.gtceu.message.nightvision.error", "§cNot enough power!");


     }

    public static void generateTooltipKeys(RegistrateLangProvider provider){

        provider.add("item.liquid_fuel_jetpack.tooltip", "Uses Combustion Generator Fuels for Thrust");

        // basic ability tooltips
        provider.add("armor.gtceu.tooltip.stepassist", "Provides Step-Assist");
        provider.add("armor.gtceu.tooltip.speed", "Increases Running Speed");
        provider.add("armor.gtceu.tooltip.jump", "Increases Jump Height and Distance");
        provider.add("armor.gtceu.tooltip.falldamage", "Nullifies Fall Damage");
        provider.add("armor.gtceu.tooltip.potions", "Nullifies Harmful Effects");
        provider.add("armor.gtceu.tooltip.burning", "Nullifies Burning");
        provider.add("armor.gtceu.tooltip.freezing", "Prevents Freezing");
        provider.add("armor.gtceu.tooltip.breath", "Replenishes Underwater Breath Bar");
        provider.add("armor.gtceu.tooltip.autoeat", "Replenishes Food Bar by Using Food from Inventory");


        //energy tooltips (iaddinfo)
        provider.add("armor.gtceu.energy_share.error", "Energy Supply: §cNot enough power for gadgets charging!");
        provider.add("armor.gtceu.energy_share.enable", "§aEnergy Supply: Gadgets charging enabled");
        provider.add("armor.gtceu.energy_share.disable", "Energy Supply: Gadgets charging disabled");
        provider.add("armor.gtceu.energy_share.tooltip", "Supply mode: %s");
        provider.add("armor.gtceu.energy_share.tooltip.guide",
                "To change mode shift-right click when holding item");
    }

    public static void generateHudKeys(RegistrateLangProvider provider){

        provider.add("armor.gtceu.hud.status.enabled", "§aON");
        provider.add("armor.gtceu.hud.status.disabled", "§cOFF");
        provider.add("armor.gtceu.hud.energy_lvl", "Energy Level: %s");
        provider.add("armor.gtceu.hud.engine_enabled", "§aEngine Enabled: %s");
        provider.add("armor.gtceu.hud.fuel_lvl", "Fuel Level: %s");
        provider.add("armor.gtceu.hud.hover_mode", "Hover Mode: %s");
        provider.add("mataarmor.hud.supply_mode", "Supply Mode: %s");
        provider.add("armor.gtceu.hud.gravi_engine", "GraviEngine: %s");

    }
}
