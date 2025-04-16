package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.utils.memoization.GTMemoizer;

import net.minecraft.world.level.block.Block;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/3/4
 * @implNote MultiblockAbility
 *           Fine, It's not really needed。It used to specify which blocks are available.
 *           Only registered blocks can be used as part of gtceu's multiblock.
 */
public class PartAbility {

    public static final PartAbility NONE = new PartAbility("none", "gtceu.part_ability.none");
    public static final PartAbility EXPORT_ITEMS = new PartAbility("export_items", "gtceu.part_ability.export_items");
    public static final PartAbility IMPORT_ITEMS = new PartAbility("import_items", "gtceu.part_ability.import_items");
    public static final PartAbility EXPORT_FLUIDS = new PartAbility("export_fluids",
            "gtceu.part_ability.export_fluids");
    public static final PartAbility IMPORT_FLUIDS = new PartAbility("import_fluids",
            "gtceu.part_ability.import_fluids");

    public static final PartAbility EXPORT_FLUIDS_1X = new PartAbility("export_fluids_1x",
            "gtceu.part_ability.export_fluids_1x");
    public static final PartAbility IMPORT_FLUIDS_1X = new PartAbility("import_fluids_1x",
            "gtceu.part_ability.import_fluids_1x");
    public static final PartAbility EXPORT_FLUIDS_4X = new PartAbility("export_fluids_4x",
            "gtceu.part_ability.export_fluids_4x");
    public static final PartAbility IMPORT_FLUIDS_4X = new PartAbility("import_fluids_4x",
            "gtceu.part_ability.import_fluids_4x");
    public static final PartAbility EXPORT_FLUIDS_9X = new PartAbility("export_fluids_9x",
            "gtceu.part_ability.export_fluids_9x");
    public static final PartAbility IMPORT_FLUIDS_9X = new PartAbility("import_fluids_9x",
            "gtceu.part_ability.import_fluids_9x");

    public static final PartAbility INPUT_ENERGY = new PartAbility("input_energy", "gtceu.part_ability.input_energy");
    public static final PartAbility OUTPUT_ENERGY = new PartAbility("output_energy",
            "gtceu.part_ability.output_energy");
    public static final PartAbility SUBSTATION_INPUT_ENERGY = new PartAbility("substation_input_energy",
            "gtceu.part_ability.substation_input_energy");
    public static final PartAbility SUBSTATION_OUTPUT_ENERGY = new PartAbility("substation_output_energy",
            "gtceu.part_ability.substation_output_energy");
    public static final PartAbility ROTOR_HOLDER = new PartAbility("rotor_holder", "gtceu.part_ability.rotor_holder");
    public static final PartAbility PUMP_FLUID_HATCH = new PartAbility("pump_fluid_hatch",
            "gtceu.part_ability.pump_input_hatch");
    public static final PartAbility STEAM = new PartAbility("steam", "gtceu.part_ability.steam");
    public static final PartAbility STEAM_IMPORT_ITEMS = new PartAbility("steam_import_items",
            "gtceu.part_ability.steam.import_items");
    public static final PartAbility STEAM_EXPORT_ITEMS = new PartAbility("steam_export_items",
            "gtceu.part_ability.steam.export_items");
    public static final PartAbility MAINTENANCE = new PartAbility("maintenance", "gtceu.part_ability.maintenance");
    public static final PartAbility MUFFLER = new PartAbility("muffler", "gtceu.part_ability.muffler");
    public static final PartAbility TANK_VALVE = new PartAbility("tank_valve", "gtceu.part_ability.tank_valve");
    public static final PartAbility PASSTHROUGH_HATCH = new PartAbility("passthrough_hatch",
            "gtceu.part_ability.passthrough_hatch");
    public static final PartAbility PARALLEL_HATCH = new PartAbility("parallel_hatch",
            "gtceu.part_ability.parallel_hatch");
    public static final PartAbility INPUT_LASER = new PartAbility("input_laser", "gtceu.part_ability.input_laser");
    public static final PartAbility OUTPUT_LASER = new PartAbility("output_laser", "gtceu.part_ability.output_laser");

    public static final PartAbility COMPUTATION_DATA_RECEPTION = new PartAbility("computation_data_reception",
            "gtceu.part_ability.computation_data_reception");
    public static final PartAbility COMPUTATION_DATA_TRANSMISSION = new PartAbility("computation_data_transmission",
            "gtceu.part_ability.computation_data_transmission");
    public static final PartAbility OPTICAL_DATA_RECEPTION = new PartAbility("optical_data_reception",
            "gtceu.part_ability.optical_data_reception");
    public static final PartAbility OPTICAL_DATA_TRANSMISSION = new PartAbility("optical_data_transmission",
            "gtceu.part_ability.optical_data_transmission");

    public static final PartAbility DATA_ACCESS = new PartAbility("data_access", "gtceu.part_ability.data_access");

    public static final PartAbility HPCA_COMPONENT = new PartAbility("hpca_component",
            "gtceu.part_ability.hpca_component");
    public static final PartAbility OBJECT_HOLDER = new PartAbility("object_holder",
            "gtceu.part_ability.object_holder");

    /**
     * tier -> available blocks
     */
    private final Int2ObjectMap<Set<Block>> registry = new Int2ObjectOpenHashMap<>();

    private final Supplier<Collection<Block>> allBlocks = GTMemoizer
            .memoize(() -> registry.values().stream().flatMap(Collection::stream).toList());

    @Getter
    private final String name;
    @Getter
    private final String langKey;

    public PartAbility(String name) {
        this.name = name;
        this.langKey = null;
    }

    public PartAbility(String name, String langKey) {
        this.name = name;
        this.langKey = langKey;
    }

    public void register(int tier, Block block) {
        registry.computeIfAbsent(tier, T -> new HashSet<>()).add(block);
    }

    public Collection<Block> getAllBlocks() {
        return allBlocks.get();
    }

    public boolean isApplicable(Block block) {
        return getAllBlocks().contains(block);
    }

    public Collection<Block> getBlocks(int... tiers) {
        return registry.int2ObjectEntrySet().stream()
                .filter(entry -> ArrayUtils.contains(tiers, entry.getIntKey()))
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }

    /**
     * [from, to]
     */
    public Collection<Block> getBlockRange(int from, int to) {
        return registry.int2ObjectEntrySet().stream()
                .filter(entry -> entry.getIntKey() <= to && entry.getIntKey() >= from)
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }
}
