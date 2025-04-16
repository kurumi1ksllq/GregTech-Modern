package com.gregtechceu.gtceu.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class ToolLang {

    public static void init(RegistrateLangProvider provider) {
        initDeathMessages(provider);
        initPartAbilityLang(provider);
        initToolInfo(provider);
    }

    private static void initDeathMessages(RegistrateLangProvider provider) {
        provider.add("death.attack.gtceu.heat", "%s was boiled alive");
        provider.add("death.attack.gtceu.frost", "%s explored cryogenics");
        provider.add("death.attack.gtceu.chemical", "%s had a chemical accident");
        provider.add("death.attack.gtceu.electric", "%s was electrocuted");
        provider.add("death.attack.gtceu.radiation", "%s glows with joy now");
        provider.add("death.attack.gtceu.turbine", "%s put their head into a turbine");
        provider.add("death.attack.gtceu.explosion", "%s exploded");
        provider.add("death.attack.gtceu.explosion.player", "%s exploded with help of %s");
        provider.add("death.attack.gtceu.heat.player", "%s was boiled alive by %s");
        provider.add("death.attack.gtceu.pickaxe", "%s got mined by %s");
        provider.add("death.attack.gtceu.shovel", "%s got dug up by %s");
        provider.add("death.attack.gtceu.axe", "%s has been chopped by %s");
        provider.add("death.attack.gtceu.hoe", "%s had their head tilled by %s");
        provider.add("death.attack.gtceu.hammer", "%s was squashed by %s");
        provider.add("death.attack.gtceu.mallet", "%s got hammered to death by %s");
        provider.add("death.attack.gtceu.mining_hammer", "%s was mistaken for Ore by %s");
        provider.add("death.attack.gtceu.spade", "%s got excavated by %s");
        provider.add("death.attack.gtceu.wrench", "%s gave %s a whack with the Wrench!");
        provider.add("death.attack.gtceu.file", "%s has been filed D for 'Dead' by %s");
        provider.add("death.attack.gtceu.crowbar", "%s lost half a life to %s");
        provider.add("death.attack.gtceu.screwdriver", "%s has screwed with %s for the last time!");
        provider.add("death.attack.gtceu.mortar", "%s was ground to dust by %s");
        provider.add("death.attack.gtceu.wire_cutter", "%s has cut the cable for the Life Support Machine of %s");
        provider.add("death.attack.gtceu.scythe", "%s had their soul taken by %s");
        provider.add("death.attack.gtceu.knife", "%s was gently poked by %s");
        provider.add("death.attack.gtceu.butchery_knife", "%s was butchered by %s");
        provider.add("death.attack.gtceu.drill_lv", "%s was drilled with 32V by %s");
        provider.add("death.attack.gtceu.drill_mv", "%s was drilled with 128V by %s");
        provider.add("death.attack.gtceu.drill_hv", "%s was drilled with 512V by %s");
        provider.add("death.attack.gtceu.drill_ev", "%s was drilled with 2048V by %s");
        provider.add("death.attack.gtceu.drill_iv", "%s was drilled with 8192V by %s");
        provider.add("death.attack.gtceu.chainsaw_lv", "%s was massacred by %s");
        provider.add("death.attack.gtceu.wrench_lv", "%s's pipes were loosened by %s");
        provider.add("death.attack.gtceu.wrench_hv", "%s's pipes were loosened by %s");
        provider.add("death.attack.gtceu.wrench_iv", "%s had a Monkey Wrench thrown into their plans by %s");
        provider.add("death.attack.gtceu.buzzsaw", "%s got buzzed by %s");
        provider.add("death.attack.gtceu.screwdriver_lv", "%s had their screws removed by %s");

        provider.add("death.attack.gtceu.medical_condition/asbestosis", "%s got mesothelioma");
        provider.add("death.attack.gtceu.medical_condition/chemical_burns", "%s had a chemical accident");
        provider.add("death.attack.gtceu.medical_condition/poison",
                "%s forgot that poisonous materials are, in fact, poisonous");
        provider.add("death.attack.gtceu.medical_condition/silicosis",
                "%s didn't die of tuberculosis. it was silicosis.");
        provider.add("death.attack.gtceu.medical_condition/arsenicosis", "%s got arsenic poisoning");
        provider.add("death.attack.gtceu.medical_condition/berylliosis", "%s mined emeralds a bit too greedily");
        provider.add("death.attack.gtceu.medical_condition/carcinogen", "%s got leukemia");
        provider.add("death.attack.gtceu.medical_condition/irritant", "%s got a §n§lREALLY§r bad rash");
        provider.add("death.attack.gtceu.medical_condition/methanol_poisoning",
                "%s tried to drink moonshine during the prohibition");
        provider.add("death.attack.gtceu.medical_condition/nausea", "%s died of nausea");
        provider.add("death.attack.gtceu.medical_condition/none", "%s died of... nothing?");
        provider.add("death.attack.gtceu.medical_condition/weak_poison", "%s ate lead (or mercury!)");
        provider.add("death.attack.gtceu.medical_condition/carbon_monoxide_poisoning", "%s left the stove on");
    }

    private static void initPartAbilityLang(RegistrateLangProvider provider) {
        provider.add("gtceu.part_ability.none", "None");
        provider.add("gtceu.part_ability.export_items", "Item Output");
        provider.add("gtceu.part_ability.import_items", "Item Input");
        provider.add("gtceu.part_ability.export_fluids", "Fluid Output");
        provider.add("gtceu.part_ability.import_fluids", "Fluid Input");

        provider.add("gtceu.part_ability.export_items_plural", "Item Outputs");
        provider.add("gtceu.part_ability.import_items_plural", "Item Inputs");
        provider.add("gtceu.part_ability.export_fluids_plural", "Fluid Outputs");
        provider.add("gtceu.part_ability.import_fluids_plural", "Fluid Inputs");

        provider.add("gtceu.part_ability.export_fluids_1x", "Fluid Output");
        provider.add("gtceu.part_ability.export_fluids_4x", "Quad Fluid");
        provider.add("gtceu.part_ability.export_fluids_9x", "Nonuple Fluid");
        provider.add("gtceu.part_ability.import_fluids_1x", "Fluid Input");
        provider.add("gtceu.part_ability.import_fluids_4x", "Import Quad Fluid");
        provider.add("gtceu.part_ability.import_fluids_9x", "Import Nonuple Fluid");

        provider.add("gtceu.part_ability.input_energy", "Energy Input");
        provider.add("gtceu.part_ability.output_energy", "Energy Output");
        provider.add("gtceu.part_ability.substation_input_energy", "Substation Energy Input");
        provider.add("gtceu.part_ability.substation_output_energy", "Substation Energy Output");

        provider.add("gtceu.part_ability.rotor_holder", "Rotor Holder");
        provider.add("gtceu.part_ability.pump_input_hatch", "Pump Input Hatch");

        provider.add("gtceu.part_ability.steam", "Steam Hatch");
        provider.add("gtceu.part_ability.steam.import_items", "Steam Input Bus");
        provider.add("gtceu.part_ability.steam.export_items", "Steam Output Bus");

        provider.add("gtceu.part_ability.maintenance", "Maintenance Hatch");
        provider.add("gtceu.part_ability.muffler", "Muffler Hatch");
        provider.add("gtceu.part_ability.tank_valve", "Tank Valve");
        provider.add("gtceu.part_ability.passthrough_hatch", "Passthrough Hatch");
        provider.add("gtceu.part_ability.parallel_hatch", "Parallel Hatch");

        provider.add("gtceu.part_ability.input_laser", "Laser Target");
        provider.add("gtceu.part_ability.output_laser", "Laser Source");

        provider.add("gtceu.part_ability.computation_data_reception", "Computation Data Reception");
        provider.add("gtceu.part_ability.computation_data_transmission", "Computation Data Transmission");
        provider.add("gtceu.part_ability.optical_data_reception", "Optical Data Reception");
        provider.add("gtceu.part_ability.optical_data_transmission", "Optical Data Transmission");

        provider.add("gtceu.part_ability.data_access", "Data Access Hatch");

        provider.add("gtceu.part_ability.hpca_component", "HPCA Component");
        provider.add("gtceu.part_ability.object_holder", "Object Holder");
    }

    private static void initToolInfo(RegistrateLangProvider provider) {}
}
