package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.addon.AddonFinder;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.renderer.cover.*;
import com.gregtechceu.gtceu.common.cover.*;
import com.gregtechceu.gtceu.common.cover.detector.*;
import com.gregtechceu.gtceu.common.cover.ender.EnderFluidLinkCover;
import com.gregtechceu.gtceu.common.cover.voiding.AdvancedFluidVoidingCover;
import com.gregtechceu.gtceu.common.cover.voiding.AdvancedItemVoidingCover;
import com.gregtechceu.gtceu.common.cover.voiding.FluidVoidingCover;
import com.gregtechceu.gtceu.common.cover.voiding.ItemVoidingCover;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoader;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class GTCovers {

    public static final int[] ALL_TIERS = GTValues.tiersBetween(GTValues.LV,
            GTCEuAPI.isHighTier() ? GTValues.OpV : GTValues.UV);
    public static final int[] ALL_TIERS_WITH_ULV = GTValues.tiersBetween(GTValues.ULV,
            GTCEuAPI.isHighTier() ? GTValues.OpV : GTValues.UV);

    static {
        GTRegistries.COVERS.unfreeze();
    }

    public static final CoverDefinition FACADE = register("facade", FacadeCover::new,
            () -> () -> FacadeCoverRenderer.INSTANCE);

    public static final CoverDefinition ITEM_FILTER = register("item_filter", ItemFilterCover::new);
    public static final CoverDefinition FLUID_FILTER = register("fluid_filter", FluidFilterCover::new);

    public static final CoverDefinition INFINITE_WATER = register("infinite_water", InfiniteWaterCover::new);
    public static final CoverDefinition ENDER_FLUID_LINK = register("ender_fluid_link", EnderFluidLinkCover::new);
    public static final CoverDefinition SHUTTER = register("shutter", ShutterCover::new);
    public static final CoverDefinition STORAGE = register("storage", StorageCover::new);

    public static final CoverDefinition[] CONVEYORS = registerTiered("conveyor", ConveyorCover::new,
            () -> tier -> new IOCoverRendererBuilder(
                    GTCEu.id("block/cover/conveyor"),
                    GTCEu.id("block/cover/conveyor_emissive"),
                    null,
                    GTCEu.id("block/cover/conveyor_inverted_emissive"))
                    .build(),
            ALL_TIERS);

    public static final CoverDefinition[] ROBOT_ARMS = registerTiered("robot_arm", RobotArmCover::new,
            () -> tier -> new IOCoverRendererBuilder(
                    GTCEu.id("block/cover/arm"),
                    GTCEu.id("block/cover/arm_emissive"),
                    null,
                    GTCEu.id("block/cover/arm_inverted_emissive"))
                    .build(),
            ALL_TIERS);

    public static final CoverDefinition[] PUMPS = registerTiered("pump", PumpCover::new,
            () -> tier -> IOCoverRendererBuilder.PUMP_LIKE_COVER_RENDERER, ALL_TIERS);

    public static final CoverDefinition[] FLUID_REGULATORS = registerTiered("fluid_regulator", FluidRegulatorCover::new,
            () -> tier -> IOCoverRendererBuilder.PUMP_LIKE_COVER_RENDERER, ALL_TIERS);

    public static final CoverDefinition COMPUTER_MONITOR = register("computer_monitor", ComputerMonitorCover::new);
    public static final CoverDefinition MACHINE_CONTROLLER = register("machine_controller",
            MachineControllerCover::new);

    // Voiding
    public static final CoverDefinition ITEM_VOIDING = register("item_voiding", ItemVoidingCover::new);
    public static final CoverDefinition ITEM_VOIDING_ADVANCED = register("item_voiding_advanced",
            AdvancedItemVoidingCover::new);
    public static final CoverDefinition FLUID_VOIDING = register("fluid_voiding", FluidVoidingCover::new);
    public static final CoverDefinition FLUID_VOIDING_ADVANCED = register("fluid_voiding_advanced",
            AdvancedFluidVoidingCover::new);

    // Detectors
    public static final CoverDefinition ACTIVITY_DETECTOR = register("activity_detector", ActivityDetectorCover::new);
    public static final CoverDefinition ACTIVITY_DETECTOR_ADVANCED = register("activity_detector_advanced",
            AdvancedActivityDetectorCover::new);
    public static final CoverDefinition FLUID_DETECTOR = register("fluid_detector", FluidDetectorCover::new);
    public static final CoverDefinition FLUID_DETECTOR_ADVANCED = register("fluid_detector_advanced",
            AdvancedFluidDetectorCover::new);
    public static final CoverDefinition ITEM_DETECTOR = register("item_detector", ItemDetectorCover::new);
    public static final CoverDefinition ITEM_DETECTOR_ADVANCED = register("item_detector_advanced",
            AdvancedItemDetectorCover::new);
    public static final CoverDefinition ENERGY_DETECTOR = register("energy_detector", EnergyDetectorCover::new);
    public static final CoverDefinition ENERGY_DETECTOR_ADVANCED = register("energy_detector_advanced",
            AdvancedEnergyDetectorCover::new);
    public static final CoverDefinition MAINTENANCE_DETECTOR = register("maintenance_detector",
            MaintenanceDetectorCover::new);

    // Solar Panels
    public static final CoverDefinition SOLAR_PANEL_BASIC = register("solar_panel", CoverSolarPanel::new);
    public static final CoverDefinition[] SOLAR_PANEL = registerTiered("solar_panel", CoverSolarPanel::new,
            () -> tier -> new CoverRendererBuilder(GTCEu.id("block/cover/solar_panel")).build(), ALL_TIERS_WITH_ULV);

    ///////////////////////////////////////////////
    // *********** UTIL METHODS ***********//
    ///////////////////////////////////////////////

    private static CoverDefinition register(String id, CoverDefinition.CoverBehaviourProvider behaviorCreator) {
        return register(id, behaviorCreator,
                () -> () -> new CoverRendererBuilder(GTCEu.id("block/cover/" + id)).build());
    }

    private static CoverDefinition register(String id, CoverDefinition.CoverBehaviourProvider behaviorCreator,
                                            Supplier<Supplier<CoverRenderer>> coverRenderer) {
        return register(GTCEu.id(id), behaviorCreator, coverRenderer);
    }

    public static CoverDefinition register(ResourceLocation id, CoverDefinition.CoverBehaviourProvider behaviorCreator,
                                           Supplier<Supplier<CoverRenderer>> coverRenderer) {
        var definition = new CoverDefinition(id, behaviorCreator, coverRenderer);
        GTRegistries.COVERS.register(definition.getId(), definition);
        return definition;
    }

    private static CoverDefinition[] registerTiered(String id,
                                                    CoverDefinition.TieredCoverBehaviourProvider behaviorCreator,
                                                    Supplier<IntFunction<CoverRenderer>> coverRenderer,
                                                    int... tiers) {
        return Arrays.stream(tiers).mapToObj(tier -> {
            var name = id + "." + GTValues.VN[tier].toLowerCase(Locale.ROOT);
            return register(name, (def, coverable, side) -> behaviorCreator.create(def, coverable, side, tier),
                    () -> () -> coverRenderer.get().apply(tier));
        }).toArray(CoverDefinition[]::new);
    }

    private static CoverDefinition[] registerTiered(String id,
                                                    CoverDefinition.TieredCoverBehaviourProvider behaviorCreator,
                                                    int... tiers) {
        return Arrays.stream(tiers).mapToObj(tier -> {
            var name = id + "." + GTValues.VN[tier].toLowerCase(Locale.ROOT);
            return register(name, (def, coverable, side) -> behaviorCreator.create(def, coverable, side, tier));
        }).toArray(CoverDefinition[]::new);
    }

    public static void init() {
        AddonFinder.getAddons().forEach(IGTAddon::registerCovers);
        ModLoader.get().postEvent(new GTCEuAPI.RegisterEvent<>(GTRegistries.COVERS, CoverDefinition.class));
        GTRegistries.COVERS.freeze();
    }
}
