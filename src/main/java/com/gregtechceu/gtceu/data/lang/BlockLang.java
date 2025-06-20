package com.gregtechceu.gtceu.data.lang;

import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.tterrag.registrate.providers.RegistrateLangProvider;

import static com.gregtechceu.gtceu.data.lang.LangUtil.*;

public class BlockLang {

    public static void init(RegistrateLangProvider provider) {
        initCasingLang(provider);
    }

    private static void initCasingLang(RegistrateLangProvider provider) {
        provider.add("block.gtceu.lamp.tooltip.inverted", "Inverted");
        provider.add("block.gtceu.lamp.tooltip.no_bloom", "No Bloom");
        provider.add("block.gtceu.lamp.tooltip.no_light", "No Light");

        // Coils
        replace(provider, "block.gtceu.hssg_coil_block", "HSS-G Coil Block");
        replace(provider, "block.gtceu.rtm_alloy_coil_block", "RTM Alloy Coil Block");

        replace(provider, "block.gtceu.wire_coil.tooltip_extended_info", "§7Hold SHIFT to show Coil Bonus Info");
        replace(provider, "block.gtceu.wire_coil.tooltip_heat", "§cBase Heat Capacity: §f%d K");
        replace(provider, "block.gtceu.wire_coil.tooltip_smelter", "§8Multi Smelter:");
        replace(provider, "block.gtceu.wire_coil.tooltip_parallel_smelter", "  §5Max Parallel: §f%s");
        replace(provider, "block.gtceu.wire_coil.tooltip_energy_smelter", "  §aEnergy Usage: §f%s EU/t §8per recipe");
        replace(provider, "block.gtceu.wire_coil.tooltip_pyro", "§8Pyrolyse Oven:");
        replace(provider, "block.gtceu.wire_coil.tooltip_speed_pyro", "  §bProcessing Speed: §f%s%%");
        replace(provider, "block.gtceu.wire_coil.tooltip_cracking", "§8Cracking Unit:");
        replace(provider, "block.gtceu.wire_coil.tooltip_energy_cracking", "  §aEnergy Usage: §f%s%%");

        // Substation capacitors
        provider.add("block.gtceu.substation_capacitor.tooltip_empty", "§7For filling space in your Power Substation");
        provider.add("block.gtceu.substation_capacitor.tooltip_filled", "§cEnergy Capacity: §f%d EU");

        // Casings
        replace(provider, "block.gtceu.bronze_brick_casing", "Bricked Bronze Casing");
        replace(provider, "block.gtceu.steel_brick_casing", "Bricked Wrought Iron Casing");
        replace(provider, "block.gtceu.heatproof_machine_casing", "Heat Proof Invar Machine Casing");
        replace(provider, "block.gtceu.frostproof_machine_casing", "Frost Proof Aluminium Machine Casing");
        replace(provider, "block.gtceu.steel_machine_casing", "Solid Steel Machine Casing");
        replace(provider, "block.gtceu.clean_machine_casing", "Clean Stainless Steel Casing");
        replace(provider, "block.gtceu.stable_machine_casing", "Stable Titanium Machine Casing");
        replace(provider, "block.gtceu.robust_machine_casing", "Robust Tungstensteel Machine Casing");
        replace(provider, "block.gtceu.casing_coke_bricks", "Coke Oven Bricks");
        replace(provider, "block.gtceu.inert_machine_casing", "Chemically Inert PTFE Machine Casing");
        replace(provider, "block.gtceu.sturdy_machine_casing", "Sturdy HSS-E Machine Casing");
        replace(provider, "block.gtceu.casing_grate", "Grate Machine Casing");
        replace(provider, "block.gtceu.assembly_line_unit", "Assembly Control Casing");
        replace(provider, "block.gtceu.ptfe_pipe_casing", "PTFE Pipe Casing");
        replace(provider, "block.gtceu.bronze_gearbox", "Bronze Gearbox Casing");
        replace(provider, "block.gtceu.steel_gearbox", "Steel Gearbox Casing");
        replace(provider, "block.gtceu.stainless_steel_gearbox", "Stainless Steel Gearbox Casing");
        replace(provider, "block.gtceu.titanium_gearbox", "Titanium Gearbox Casing");
        replace(provider, "block.gtceu.tungstensteel_gearbox", "Tungstensteel Gearbox Casing");
        replace(provider, "block.gtceu.steel_turbine_casing", "Magnalium Turbine Casing");
        replace(provider, "block.gtceu.titanium_turbine_casing", "Titanium Turbine Casing");
        replace(provider, "block.gtceu.stainless_steel_turbine_casing", "Stainless Turbine Casing");
        replace(provider, "block.gtceu.tungstensteel_turbine_casing", "Tungstensteel Turbine Casing");
        replace(provider, "block.gtceu.bronze_pipe_casing", "Bronze Pipe Casing");
        replace(provider, "block.gtceu.steel_pipe_casing", "Steel Pipe Casing");
        replace(provider, "block.gtceu.titanium_pipe_casing", "Titanium Pipe Casing");
        replace(provider, "block.gtceu.tungstensteel_pipe_casing", "Tungstensteel Pipe Casing");
        replace(provider, "block.gtceu.palladium_substation", "Palladium Substation Casing");

        replace(provider, "block.gtceu.steam_casing_bronze", "Bronze Hull");
        provider.add("block.gtceu.steam_casing_bronze.tooltip", "§7For your first Steam Machines");
        replace(provider, "block.gtceu.steam_casing_bricked_bronze", "Bricked Bronze Hull");
        provider.add("block.gtceu.steam_casing_bricked_bronze.tooltip", "§7For your first Steam Machines");
        replace(provider, "block.gtceu.steam_casing_steel", "Steel Hull");
        provider.add("block.gtceu.steam_casing_steel.tooltip", "§7For improved Steam Machines");
        replace(provider, "block.gtceu.steam_casing_bricked_steel", "Bricked Wrought Iron Hull");
        provider.add("block.gtceu.steam_casing_bricked_steel.tooltip", "§7For improved Steam Machines");

        // GCYM Casings
        replace(provider, "block.gtceu.laser_safe_engraving_casing", "Laser-Safe Engraving Casing");
        replace(provider, "block.gtceu.large_scale_assembler_casing", "Large-Scale Assembler Casing");
        replace(provider, "block.gtceu.reaction_safe_mixing_casing", "Reaction-Safe Mixing Casing");
        replace(provider, "block.gtceu.vibration_safe_casing", "Vibration-Safe Casing");

        replace(provider, "block.gtceu.superconducting_coil", "Superconducting Coil Block");
        replace(provider, "block.gtceu.fusion_coil", "Fusion Coil Block");
        replace(provider, "block.gtceu.fusion_casing", "Fusion Machine Casing");
        replace(provider, "block.gtceu.fusion_casing_mk2", "Fusion Machine Casing MK II");
        replace(provider, "block.gtceu.fusion_casing_mk3", "Fusion Machine Casing MK III");

        provider.add("block.filter_casing.tooltip", "Creates a §aParticle-Free§7 environment");
        provider.add("block.sterilizing_filter_casing.tooltip", "Creates a §aSterilized§7 environment");

        provider.add("block.gtceu.explosive.breaking_tooltip",
                "Primes explosion when mined, sneak mine to pick back up");
        provider.add("block.gtceu.explosive.lighting_tooltip", "Cannot be lit with Redstone");
        provider.add("block.gtceu.powderbarrel.drops_tooltip",
                "Slightly larger than TNT, drops all destroyed Blocks as Items");
        provider.add("block.gtceu.itnt.drops_tooltip", "Much larger than TNT, drops all destroyed Blocks as Items");

        provider.add("block.gtceu.normal_laser_pipe.tooltip",
                "§7Transmitting power with §fno loss§7 in straight lines");
        provider.add("block.gtceu.normal_optical_pipe.tooltip", "§7Transmitting §fComputation§7 or §fResearch Data§7");

        replace(provider, GTBlocks.BATTERY_EMPTY_TIER_I.get().getDescriptionId(), "Empty Tier I Capacitor");
        replace(provider, GTBlocks.BATTERY_LAPOTRONIC_EV.get().getDescriptionId(), "EV Lapotronic Capacitor");
        replace(provider, GTBlocks.BATTERY_LAPOTRONIC_IV.get().getDescriptionId(), "IV Lapotronic Capacitor");
        replace(provider, GTBlocks.BATTERY_EMPTY_TIER_II.get().getDescriptionId(), "Empty Tier II Capacitor");
        replace(provider, GTBlocks.BATTERY_LAPOTRONIC_LuV.get().getDescriptionId(), "LuV Lapotronic Capacitor");
        replace(provider, GTBlocks.BATTERY_LAPOTRONIC_ZPM.get().getDescriptionId(), "ZPM Lapotronic Capacitor");
        replace(provider, GTBlocks.BATTERY_EMPTY_TIER_III.get().getDescriptionId(), "Empty Tier III Capacitor");
        replace(provider, GTBlocks.BATTERY_LAPOTRONIC_UV.get().getDescriptionId(), "UV Lapotronic Capacitor");
        replace(provider, GTBlocks.BATTERY_ULTIMATE_UHV.get().getDescriptionId(), "UHV Ultimate Capacitor");


    }
    public static void generatePipeKeys(RegistrateLangProvider provider){
        //cable
        provider.add("gtceu.cable.voltage", "§aMax Voltage:§r §a%d §a(%s§a)");
        provider.add("gtceu.cable.amperage", "§eMax Amperage:§r §e%d");
        provider.add("gtceu.cable.loss_per_block", "§cLoss/Meter/Ampere:§r §c%d§7 EU-Volt");
        provider.add("gtceu.cable.superconductor", "%s §dSuperconductor");


        provider.add("gtceu.fluid_pipe.capacity", "§9Capacity: §f%d mB");
        provider.add("gtceu.fluid_pipe.max_temperature", "§cTemperature Limit: §f%d K");
        provider.add("gtceu.fluid_pipe.channels", "§eChannels: §f%d");
        provider.add("gtceu.fluid_pipe.gas_proof", "§6Can handle Gases");
        provider.add("gtceu.fluid_pipe.acid_proof", "§6Can handle Acids");
        provider.add("gtceu.fluid_pipe.cryo_proof", "§6Can handle Cryogenics");
        provider.add("gtceu.fluid_pipe.plasma_proof", "§6Can handle all Plasmas");
        provider.add("gtceu.fluid_pipe.not_gas_proof", "§4Gases may leak!");
        provider.add("gtceu.item_pipe.priority", "§9Priority: §f%d");
        provider.add("gtceu.duct_pipe.transfer_rate", "§bAir transfer rate: %s");

    }

    public static void generateMultibockKeys(RegistrateLangProvider provider){

        provider.add("gtceu.multiblock.work_paused", "Work Paused.");
        provider.add("gtceu.multiblock.running", "Running perfectly.");
        provider.add("gtceu.multiblock.idling", "§6Idling.");
        provider.add("gtceu.multiblock.research_station.researching", "§6Researching.");
        provider.add("gtceu.multiblock.not_enough_energy", "WARNING: Machine needs more energy.");
        provider.add("gtceu.multiblock.not_enough_energy_output", "WARNING: Energy Dynamo Tier Too Low!");
        provider.add("gtceu.multiblock.waiting", "WARNING: Machine is waiting.");
        provider.add("gtceu.multiblock.progress_percent", "Progress: %s%%");
        provider.add("gtceu.multiblock.progress", "Progress: %ss / %ss (%s%%)");
        provider.add("gtceu.multiblock.output_line.0", "%s x §e%s§r (%ss/ea)");
        provider.add("gtceu.multiblock.output_line.1", "%s x §e%s§r (%s/s)");
        provider.add("gtceu.multiblock.output_line.2", "%s ≈ §e%s§r (%ss/ea)");
        provider.add("gtceu.multiblock.output_line.3", "%s ≈ §e%s§r (%s/s)");
        provider.add("gtceu.multiblock.invalid_structure", "Invalid structure.");
        provider.add("gtceu.multiblock.invalid_structure.tooltip",
                "This block is a controller of the multiblock structure. For building help, see structure template in JEI.");
        provider.add("gtceu.multiblock.validation_failed", "Invalid amount of inputs/outputs.");
        provider.add("gtceu.multiblock.max_recipe_tier", "Max Recipe Tier: %s");
        provider.add("gtceu.multiblock.max_recipe_tier_hover", "The maximum tier of recipes that can be run");
        provider.add("gtceu.multiblock.max_energy_per_tick", "Max EU/t: §a%s (%s§r)");
        provider.add("gtceu.multiblock.max_energy_per_tick_hover",
                "The maximum EU/t available for running recipes or overclocking");
        provider.add("gtceu.multiblock.max_energy_per_tick_amps", "Max EU/t: %s (%sA %s)");
        provider.add("gtceu.multiblock.energy_consumption", "Energy Usage: %s EU/t (%s)");
        provider.add("gtceu.multiblock.generation_eu", "Outputting: §a%s EU/t");
        provider.add("gtceu.multiblock.universal.no_problems", "No Maintenance Problems!");
        provider.add("gtceu.multiblock.universal.has_problems", "Has Maintenance Problems!");
        provider.add("gtceu.multiblock.universal.has_problems_header",
                "Fix the following issues in a Maintenance Hatch:");
        provider.add("gtceu.multiblock.universal.problem.wrench", "§7Pipe is loose. (§aWrench§7)");
        provider.add("gtceu.multiblock.universal.problem.screwdriver", "§7Screws are loose. (§aScrewdriver§7)");
        provider.add("gtceu.multiblock.universal.problem.soft_mallet", "§7Something is stuck. (§aSoft Mallet§7)");
        provider.add("gtceu.multiblock.universal.problem.hard_hammer", "§7Plating is dented. (§aHard Hammer§7)");
        provider.add("gtceu.multiblock.universal.problem.wire_cutter", "§7Wires burned out. (§aWire Cutter§7)");
        provider.add("gtceu.multiblock.universal.problem.crowbar", "§7That doesn't belong there. (§aCrowbar§7)");
        provider.add("gtceu.multiblock.universal.muffler_obstructed", "Muffler Hatch is Obstructed!");
        provider.add("gtceu.multiblock.universal.muffler_obstructed.tooltip",
                "Muffler Hatch must have a block of airspace in front of it.");
        provider.add("gtceu.multiblock.universal.rotor_obstructed", "Rotor is Obstructed!");
        provider.add("gtceu.multiblock.universal.distinct", "Distinct Buses:");
        provider.add("gtceu.multiblock.universal.distinct.no", "No");
        provider.add("gtceu.multiblock.universal.distinct.yes", "Yes");
        provider.add("gtceu.multiblock.universal.distinct.info",
                "If enabled, each Item Input Bus will be treated as fully distinct from each other for recipe lookup. Useful for things like Programmed Circuits, Extruder Shapes, etc.");
        provider.add("gtceu.multiblock.parallel", "Performing up to %d Recipes in Parallel");
        provider.add("gtceu.multiblock.parallel.exact", "Performing %d Recipes in Parallel");
        provider.add("gtceu.multiblock.multiple_recipemaps.header", "Machine Mode:");
        provider.add("gtceu.multiblock.multiple_recipemaps.tooltip",
                "Screwdriver the controller to change which machine mode to use.");
        provider.add("gtceu.multiblock.multiple_recipemaps_recipes.tooltip", "Machine Modes: §e%s§r");
        provider.add("gtceu.multiblock.multiple_recipemaps.switch_message",
                "The machine must be off to switch modes!");
        provider.add("gtceu.multiblock.preview.zoom", "Use mousewheel or right-click + drag to zoom");
        provider.add("gtceu.multiblock.preview.rotate", "Click and drag to rotate");
        provider.add("gtceu.multiblock.preview.select", "Right-click to check candidates");
        provider.add("gtceu.multiblock.pattern.error", "Expected components (%s) at (%s).");
        provider.add("gtceu.multiblock.pattern.error.limited_exact", "§cExactly: %d§r");
        provider.add("gtceu.multiblock.pattern.error.limited_within", "§cBetween %d and %d§r");
        multiLang(provider, "gtceu.multiblock.pattern.error.limited", "§cMaximum: %d§r", "§cMinimum: %d§r",
                "§cMaximum: %d per layer§r", "§cMinimum: %d per layer§r");
        provider.add("gtceu.multiblock.pattern.error.coils", "§cAll heating coils must be the same§r");
        provider.add("gtceu.multiblock.pattern.error.filters", "§cAll filters must be the same§r");
        provider.add("gtceu.multiblock.pattern.error.batteries", "§cAll batteries must be the same§r");
        provider.add("gtceu.multiblock.pattern.clear_amount_1", "§6Must have a clear 1x1x1 space in front§r");
        provider.add("gtceu.multiblock.pattern.clear_amount_3", "§6Must have a clear 3x3x1 space in front§r");
        provider.add("gtceu.multiblock.pattern.single", "§6Only this block can be used§r");
        provider.add("gtceu.multiblock.pattern.location_end", "§cVery End§r");
        provider.add("gtceu.multiblock.pattern.replaceable_air", "Replaceable by Air");

        provider.add("gtceu.multiblock.computation.max", "Max CWU/t: %s");
        provider.add("gtceu.multiblock.computation.usage", "Using: %s");
        provider.add("gtceu.multiblock.computation.non_bridging", "Non-bridging connection found");
        provider.add("gtceu.multiblock.computation.non_bridging.detailed",
                "A Reception Hatch is linked to a machine which cannot bridge");
        provider.add("gtceu.multiblock.computation.not_enough_computation", "Machine needs more computation!");

    }
}
