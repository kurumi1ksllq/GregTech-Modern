package com.gregtechceu.gtceu.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;


public class CommandLang {

    public static void init(RegistrateLangProvider provider){}

    public static void generateCommandLang(RegistrateLangProvider provider){
        provider.add("command.gtceu.dump_data.success", "Dumped %s resources from registry %s to %s");
        provider.add("command.gtceu.place_vein.failure", "Failed to place vein %s at position %s");
        provider.add("command.gtceu.place_vein.success", "Placed vein %s at position %s");
        provider.add("command.gtceu.share_prospection_data.notification", "%s is sharing prospecting data with you!");
        provider.add("command.gtceu.medical_condition.get", "Player %s has these medical conditions:");
        provider.add("command.gtceu.medical_condition.get.empty", "Player %s has no medical conditions.");
        provider.add("command.gtceu.medical_condition.get.element", "Condition %s§r: %s minutes %s seconds");
        provider.add("command.gtceu.medical_condition.get.element.permanent",
                "Condition %s§r: %s minutes %s seconds (permanent)");

    }
}
