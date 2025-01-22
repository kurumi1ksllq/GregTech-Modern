package com.gregtechceu.gtceu.common.data.materials;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty.GasTier;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;

public class GCYMMaterials {

    public static void register() {
        TantalumCarbide = new Material.Builder(GTCEu.id("tantalum_carbide"))
                .ingot(4).fluid()
                .color(0x999900).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_PLATE)
                .componentStacks(new MaterialStack(Tantalum, 1), new MaterialStack(Carbon, 1))
                .blast(b -> b.temp(4120, GasTier.MID)
                        .blastStats(VA[EV], 1200))
                .buildAndRegister();

        HSLASteel = new Material.Builder(GTCEu.id("hsla_steel"))
                .ingot(3).fluid()
                .color(0x686868).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_PLATE, GENERATE_ROD, GENERATE_FRAME, GENERATE_SPRING)
                .componentStacks(new MaterialStack(Invar, 2), new MaterialStack(Vanadium, 1),
                        new MaterialStack(Titanium, 1), new MaterialStack(Molybdenum, 1))
                .blast(b -> b.temp(1711, GasTier.LOW)
                        .blastStats(VA[GTValues.HV], 1000))
                .buildAndRegister();

        MolybdenumDisilicide = new Material.Builder(GTCEu.id("molybdenum_disilicide"))
                .ingot(2).fluid()
                .color(0x564A84).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_SPRING, GENERATE_RING, GENERATE_PLATE, GENERATE_LONG_ROD)
                .componentStacks(new MaterialStack(Molybdenum, 1), new MaterialStack(Silicon, 2))
                .blast(b -> b.temp(2300, GasTier.MID)
                        .blastStats(VA[EV], 800))
                .buildAndRegister();

        Zeron100 = new Material.Builder(GTCEu.id("zeron_100"))
                .ingot(5).fluid()
                .color(0x294972).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_PLATE)
                .componentStacks(new MaterialStack(Iron, 10), new MaterialStack(Nickel, 2),
                        new MaterialStack(Tungsten, 2), new MaterialStack(Niobium, 1), new MaterialStack(Cobalt, 1))
                .blast(b -> b.temp(3693, GasTier.MID)
                        .blastStats(VA[EV], 1000))
                .buildAndRegister();

        WatertightSteel = new Material.Builder(GTCEu.id("watertight_steel"))
                .ingot(4).fluid()
                .color(0x2B4B56).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_PLATE, GENERATE_ROD, GENERATE_FRAME)
                .componentStacks(new MaterialStack(Iron, 7), new MaterialStack(Aluminium, 4),
                        new MaterialStack(Nickel, 2), new MaterialStack(Chromium, 1), new MaterialStack(Sulfur, 1))
                .blast(b -> b.temp(3850, GasTier.MID)
                        .blastStats(VA[EV], 800))
                .buildAndRegister();

        IncoloyMA956 = new Material.Builder(GTCEu.id("incoloy_ma_956"))
                .ingot(5).fluid()
                .color(0x2D9B66).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_PLATE, GENERATE_ROD, GENERATE_FRAME)
                .componentStacks(new MaterialStack(VanadiumSteel, 4), new MaterialStack(Manganese, 2),
                        new MaterialStack(Aluminium, 5), new MaterialStack(Yttrium, 2))
                .blast(b -> b.temp(3652, GasTier.MID)
                        .blastStats(VA[EV], 800))
                .buildAndRegister();

        MaragingSteel300 = new Material.Builder(GTCEu.id("maraging_steel_300"))
                .ingot(4).fluid()
                .color(0x505B6E).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_ROD, GENERATE_FRAME)
                .componentStacks(new MaterialStack(Iron, 16), new MaterialStack(Titanium, 1),
                        new MaterialStack(Aluminium, 1), new MaterialStack(Nickel, 4), new MaterialStack(Cobalt, 2))
                .blast(b -> b.temp(4000, GasTier.HIGH)
                        .blastStats(VA[EV], 1000))
                .buildAndRegister();

        HastelloyX = new Material.Builder(GTCEu.id("hastelloy_x"))
                .ingot(5).fluid()
                .color(0x5784B8).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_PLATE, GENERATE_FRAME)
                .componentStacks(new MaterialStack(Nickel, 8), new MaterialStack(Iron, 3),
                        new MaterialStack(Tungsten, 4), new MaterialStack(Molybdenum, 2),
                        new MaterialStack(Chromium, 1), new MaterialStack(Niobium, 1))
                .blast(b -> b.temp(4200, GasTier.HIGH)
                        .blastStats(VA[EV], 900))
                .buildAndRegister();

        Stellite100 = new Material.Builder(GTCEu.id("stellite_100"))
                .ingot(4).fluid()
                .color(0xCFCFEE).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_PLATE)
                .componentStacks(new MaterialStack(Iron, 4), new MaterialStack(Chromium, 3),
                        new MaterialStack(Tungsten, 2), new MaterialStack(Molybdenum, 1))
                .blast(b -> b.temp(3790, GasTier.HIGH)
                        .blastStats(VA[EV], 1000))
                .buildAndRegister();

        TitaniumCarbide = new Material.Builder(GTCEu.id("titanium_carbide"))
                .ingot(3).fluid()
                .color(0x90092F).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_PLATE)
                .componentStacks(new MaterialStack(Titanium, 1), new MaterialStack(Carbon, 1))
                .blast(b -> b.temp(3430, GasTier.MID)
                        .blastStats(VA[EV], 1000))
                .buildAndRegister();

        TitaniumTungstenCarbide = new Material.Builder(GTCEu.id("titanium_tungsten_carbide"))
                .ingot(6).fluid()
                .color(0x680B0B).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_PLATE)
                .componentStacks(new MaterialStack(TitaniumCarbide, 2), new MaterialStack(TungstenCarbide, 1))
                .blast(b -> b.temp(3800, GasTier.HIGH)
                        .blastStats(VA[EV], 1000))
                .buildAndRegister();

        HastelloyC276 = new Material.Builder(GTCEu.id("hastelloy_c_276"))
                .ingot(6).fluid()
                .color(0xAB2F2F).iconSet(METALLIC)
                .appendFlags(STD_METAL, GENERATE_PLATE, GENERATE_FRAME)
                .componentStacks(new MaterialStack(Nickel, 12), new MaterialStack(Molybdenum, 8),
                        new MaterialStack(Chromium, 7), new MaterialStack(Tungsten, 1), new MaterialStack(Cobalt, 1),
                        new MaterialStack(Copper, 1))
                .blast(b -> b.temp(3800, GasTier.HIGH)
                        .blastStats(VA[EV], 1000))
                .buildAndRegister();
    }
}
