package com.gregtechceu.gtceu.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

import static com.gregtechceu.gtceu.data.lang.LangUtil.*;

public class ToolLang {

    public static void init(RegistrateLangProvider provider) {
        initDeathMessages(provider);
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

    public static void generateToolClassKey(RegistrateLangProvider provider){

        provider.add("gtceu.tool.class.sword", "Sword");
        provider.add("gtceu.tool.class.pickaxe", "Pickaxe");
        provider.add("gtceu.tool.class.shovel", "Shovel");
        provider.add("gtceu.tool.class.axe", "Axe");
        provider.add("gtceu.tool.class.hoe", "Hoe");
        provider.add("gtceu.tool.class.mining_hammer", "Mining Hammer");
        provider.add("gtceu.tool.class.spade", "Spade");
        provider.add("gtceu.tool.class.saw", "Saw");
        provider.add("gtceu.tool.class.hammer", "Hammer");
        provider.add("gtceu.tool.class.mallet", "Soft Mallet");
        provider.add("gtceu.tool.class.wrench", "Wrench");
        provider.add("gtceu.tool.class.file", "File");
        provider.add("gtceu.tool.class.crowbar", "Crowbar");
        provider.add("gtceu.tool.class.screwdriver", "Screwdriver");
        provider.add("gtceu.tool.class.mortar", "Mortar");
        provider.add("gtceu.tool.class.wire_cutter", "Wire Cutter");
        provider.add("gtceu.tool.class.knife", "Knife");
        provider.add("gtceu.tool.class.butchery_knife", "Butchery Knife");
        provider.add("gtceu.tool.class.scythe", "Scythe");
        provider.add("gtceu.tool.class.rolling_pin", "Rolling Pin");
        provider.add("gtceu.tool.class.plunger", "Plunger");
        provider.add("gtceu.tool.class.shears", "Shears");
        provider.add("gtceu.tool.class.drill", "Drill");

    }
    public static void generateBehaviorKey(RegistrateLangProvider provider){
        provider.add("item.gtceu.tool.behavior.silk_ice", "§bIce Cutter: §fSilk Harvests Ice");
        provider.add("item.gtceu.tool.behavior.torch_place", "§eSpelunker: §fPlaces Torches on Right-Click");
        provider.add("item.gtceu.tool.behavior.tree_felling", "§4Lumberjack: §fTree Felling");
        provider.add("item.gtceu.tool.behavior.strip_log", "§5Artisan: §fStrips Logs");
        provider.add("item.gtceu.tool.behavior.scrape", "§bPolisher: §fRemoves Oxidation");
        provider.add("item.gtceu.tool.behavior.remove_wax", "§6Cleaner: §fRemoves Wax");
        provider.add("item.gtceu.tool.behavior.shield_disable", "§cBrute: §fDisables Shields");
        provider.add("item.gtceu.tool.behavior.relocate_mining", "§2Magnetic: §fRelocates Mined Blocks and Mob Drops");
        provider.add("item.gtceu.tool.behavior.aoe_mining", "§5Area-of-Effect: §f%sx%sx%s");
        provider.add("item.gtceu.tool.behavior.ground_tilling", "§eFarmer: §fTills Ground");
        provider.add("item.gtceu.tool.behavior.grass_path", "§eLandscaper: §fCreates Grass Paths");
        provider.add("item.gtceu.tool.behavior.rail_rotation", "§eRailroad Engineer: §fRotates Rails");
        provider.add("item.gtceu.tool.behavior.crop_harvesting", "§aHarvester: §fHarvests Crops");
        provider.add("item.gtceu.tool.behavior.plunger", "§9Plumber: §fDrains Fluids");
        provider.add("item.gtceu.tool.behavior.block_rotation", "§2Mechanic: §fRotates Blocks");
        provider.add("item.gtceu.tool.behavior.damage_boost", "§4Damage Boost: §fExtra damage against %s");

        //mode switching
        provider.add("item.behavior.mode_switch.tooltip", "Use while sneaking to switch mode");
        provider.add("item.behavior.mode_switch.mode_switched", "§eMode Set to: %s");
        provider.add("item.behavior.mode_switch.current_mode", "Mode: %s");

    }

    public static void generateToolKeys(RegistrateLangProvider provider){
        replace(provider, "item.gtceu.tool.sword", "%s Sword");
        replace(provider, "item.gtceu.tool.pickaxe", "%s Pickaxe");
        replace(provider, "item.gtceu.tool.shovel", "%s Shovel");
        replace(provider, "item.gtceu.tool.axe", "%s Axe");
        replace(provider, "item.gtceu.tool.hoe", "%s Hoe");
        replace(provider, "item.gtceu.tool.saw", "%s Saw");
        replace(provider, "item.gtceu.tool.hammer", "%s Hammer");
        provider.add("item.gtceu.tool.hammer.tooltip", "§8Crushes Blocks when harvesting them");
        replace(provider, "item.gtceu.tool.mallet", "%s Soft Mallet");
        multilineLang(provider, "item.gtceu.tool.mallet.tooltip",
                "§8Sneak to Pause Machine After Current Recipe.\n§8Stops/Starts Machines");
        replace(provider, "item.gtceu.tool.wrench", "%s Wrench");
        provider.add("item.gtceu.tool.wrench.tooltip", "§8Hold left click to dismantle Machines");
        replace(provider, "item.gtceu.tool.file", "%s File");
        replace(provider, "item.gtceu.tool.crowbar", "%s Crowbar");
        provider.add("item.gtceu.tool.crowbar.tooltip", "§8Dismounts Covers");
        replace(provider, "item.gtceu.tool.screwdriver", "%s Screwdriver");
        provider.add("item.gtceu.tool.screwdriver.tooltip", "§8Adjusts Covers and Machines");
        replace(provider, "item.gtceu.tool.mortar", "%s Mortar");
        replace(provider, "item.gtceu.tool.wire_cutter", "%s Wire Cutter");
        replace(provider, "item.gtceu.tool.knife", "%s Knife");
        replace(provider, "item.gtceu.tool.butchery_knife", "%s Butchery Knife");
        provider.add("item.gtceu.tool.butchery_knife.tooltip", "§8Has a slow Attack Rate");
        replace(provider, "item.gtceu.tool.scythe", "%s Scythe");
        provider.add("item.gtceu.tool.scythe.tooltip", "§8Because a Scythe doesn't make Sense");
        replace(provider, "item.gtceu.tool.rolling_pin", "%s Rolling Pin");
        replace(provider, "item.gtceu.tool.lv_drill", "%s Drill (LV)");
        replace(provider, "item.gtceu.tool.mv_drill", "%s Drill (MV)");
        replace(provider, "item.gtceu.tool.hv_drill", "%s Drill (HV)");
        replace(provider, "item.gtceu.tool.ev_drill", "%s Drill (EV)");
        replace(provider, "item.gtceu.tool.iv_drill", "%s Drill (IV)");
        replace(provider, "item.gtceu.tool.lv_wirecutter", "%s Wire Cutter (LV)");
        replace(provider, "item.gtceu.tool.hv_wirecutter", "%s Wire Cutter (HV)");
        replace(provider, "item.gtceu.tool.iv_wirecutter", "%s Wire Cutter (IV)");
        replace(provider, "item.gtceu.tool.mining_hammer", "%s Mining Hammer");
        provider.add("item.gtceu.tool.mining_hammer.tooltip",
                "§8Mines a large area at once (unless you're crouching)");
        replace(provider, "item.gtceu.tool.spade", "%s Spade");
        provider.add("item.gtceu.tool.spade.tooltip", "§8Mines a large area at once (unless you're crouching)");
        replace(provider, "item.gtceu.tool.lv_chainsaw", "%s Chainsaw (LV)");
        replace(provider, "item.gtceu.tool.mv_chainsaw", "%s Chainsaw (MV)");
        replace(provider, "item.gtceu.tool.hv_chainsaw", "%s Chainsaw (HV)");
        replace(provider, "item.gtceu.tool.lv_wrench", "%s Wrench (LV)");
        provider.add("item.gtceu.tool.lv_wrench.tooltip", "§8Hold left click to dismantle Machines");
        replace(provider, "item.gtceu.tool.hv_wrench", "%s Wrench (HV)");
        provider.add("item.gtceu.tool.hv_wrench.tooltip", "§8Hold left click to dismantle Machines");
        replace(provider, "item.gtceu.tool.iv_wrench", "%s Wrench (IV)");
        provider.add("item.gtceu.tool.iv_wrench.tooltip", "§8Hold left click to dismantle Machines");
        replace(provider, "item.gtceu.tool.buzzsaw", "%s Buzzsaw (LV)");
        provider.add("item.gtceu.tool.buzzsaw.tooltip", "§8Not suitable for harvesting Blocks");
        replace(provider, "item.gtceu.tool.lv_screwdriver", "%s Screwdriver (LV)");
        provider.add("item.gtceu.tool.lv_screwdriver.tooltip", "§8Adjusts Covers and Machines");
        replace(provider, "item.gtceu.tool.plunger", "%s Plunger");
        provider.add("item.gtceu.tool.plunger.tooltip", "§8Removes Fluids from Machines");
        replace(provider, "item.gtceu.tool.shears", "%s Shears");
    }

    public static void generateTooltips(RegistrateLangProvider provider){
        provider.add("item.gtceu.tool.tooltip.crafting_uses", "%s §aCrafting Uses");
        provider.add("item.gtceu.tool.tooltip.max_uses", "%s §eTotal Durability");
        provider.add("item.gtceu.tool.tooltip.general_uses", "%s §bDurability");
        provider.add("item.gtceu.tool.tooltip.attack_damage", "%s §cAttack Damage");
        provider.add("item.gtceu.tool.tooltip.attack_speed", "%s §9Attack Speed");
        provider.add("item.gtceu.tool.tooltip.mining_speed", "%s §dMining Speed");
        provider.add("item.gtceu.tool.tooltip.harvest_level", "§eHarvest Level %s");
        provider.add("item.gtceu.tool.tooltip.harvest_level_extra", "§eHarvest Level %s §f(%s§f)");
        multiLang(provider, "item.gtceu.tool.harvest_level", "§8Wood", "§7Stone", "§aIron", "§bDiamond",
                "§dNetherite",
                "§9Duranium", "§cNeutronium");
        provider.add("item.gtceu.tool.tooltip.repair_info", "§8Hold SHIFT to show Repair Info");
        provider.add("item.gtceu.tool.tooltip.repair_material", "§8Repair with: §f§a%s");
    }

    public static void generateActionKeys(RegistrateLangProvider provider){
        provider.add("gtceu.tool_action.show_tooltips", "Hold SHIFT to show Tool Info");
        provider.add("gtceu.tool_action.screwdriver.auto_output_covers",
                "§8Use Screwdriver to Allow Input from Output Side or access Covers");
        provider.add("gtceu.tool_action.screwdriver.toggle_mode_covers",
                "§8Use Screwdriver to toggle Modes or access Covers");
        provider.add("gtceu.tool_action.screwdriver.access_covers", "§8Use Screwdriver to access Covers");
        provider.add("gtceu.tool_action.screwdriver.auto_collapse",
                "§8Use Screwdriver to toggle Item collapsing");
        provider.add("gtceu.tool_action.screwdriver.auto_output", "§8Use Screwdriver to toggle Auto-Output");
        provider.add("gtceu.tool_action.screwdriver.toggle_mode", "§8Use Screwdriver to toggle Modes");
        provider.add("gtceu.tool_action.wrench.set_facing", "§8Use Wrench to set Facing");
        provider.add("gtceu.tool_action.wrench.connect",
                "§8Use Wrench to set Connections, sneak to block Connections");
        provider.add("gtceu.tool_action.wire_cutter.connect", "§8Use Wire Cutters to set Connections");
        provider.add("gtceu.tool_action.soft_mallet.reset", "§8Use Soft Mallet to toggle Working");
        provider.add("gtceu.tool_action.soft_mallet.toggle_mode", "§8Use Soft Mallet to toggle Modes");
        provider.add("gtceu.tool_action.hammer", "§8Use Hard Hammer to muffle Sounds");
        provider.add("gtceu.tool_action.crowbar", "§8Use Crowbar to remove Covers");
        provider.add("gtceu.tool_action.tape", "§8Use Tape to fix Maintenance Problems");

    }

    private static void initToolInfo(RegistrateLangProvider provider) {}
}
