package com.gregtechceu.gtceu.client;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidDefinition;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreDefinition;
import com.gregtechceu.gtceu.api.ui.UIContainerScreen;
import com.gregtechceu.gtceu.api.ui.parsing.UIModelLoader;
import com.gregtechceu.gtceu.api.ui.texture.NinePatchTexture;
import com.gregtechceu.gtceu.client.particle.HazardParticle;
import com.gregtechceu.gtceu.client.renderer.entity.GTBoatRenderer;
import com.gregtechceu.gtceu.client.renderer.entity.GTExplosiveRenderer;
import com.gregtechceu.gtceu.client.ui.ScreenInternals;
import com.gregtechceu.gtceu.common.CommonProxy;
import com.gregtechceu.gtceu.common.data.GTBlockEntities;
import com.gregtechceu.gtceu.common.data.GTEntityTypes;
import com.gregtechceu.gtceu.common.data.GTParticleTypes;
import com.gregtechceu.gtceu.common.entity.GTBoat;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.map.ClientCacheManager;
import com.gregtechceu.gtceu.integration.map.cache.client.GTClientCache;
import com.gregtechceu.gtceu.integration.map.ftbchunks.FTBChunksPlugin;
import com.gregtechceu.gtceu.integration.map.layer.Layers;
import com.gregtechceu.gtceu.integration.map.layer.builtin.FluidRenderLayer;
import com.gregtechceu.gtceu.integration.map.layer.builtin.OreRenderLayer;
import com.gregtechceu.gtceu.utils.input.KeyBind;

import net.minecraft.Util;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.ApiStatus;

import static com.gregtechceu.gtceu.api.ui.UIContainerMenu.MENU_TYPE;

/**
 * @author KilaBash
 * @date 2023/7/30
 * @implNote ClientProxy
 */
public class ClientProxy extends CommonProxy {

    public static final BiMap<ResourceLocation, GTOreDefinition> CLIENT_ORE_VEINS = HashBiMap.create();
    public static final BiMap<ResourceLocation, BedrockFluidDefinition> CLIENT_FLUID_VEINS = HashBiMap.create();
    public static final BiMap<ResourceLocation, BedrockOreDefinition> CLIENT_BEDROCK_ORE_VEINS = HashBiMap.create();

    public ClientProxy() {
        super();
        init();
    }

    public static void init() {
        if (!GTCEu.isDataGen()) {
            ClientCacheManager.registerClientCache(GTClientCache.instance, "gtceu");
            Layers.registerLayer(OreRenderLayer::new, "ore_veins");
            Layers.registerLayer(FluidRenderLayer::new, "bedrock_fluids");
        }
        loadRenderdoc();

        ScreenInternals.init();
    }

    private static final String LINUX_RENDERDOC_WARNING = """

            ========================================
            Ignored 'gtceu.renderdocPath' property as this Minecraft instance is not running on Windows.
            Please populate the LD_PRELOAD environment variable instead
            ========================================""";

    private static final String MAC_RENDERDOC_WARNING = """

            ========================================
            Ignored 'gtceu.renderdocPath' property as this Minecraft instance is not running on Windows.
            RenderDoc is not supported on macOS
            ========================================""";

    private static final String GENERIC_RENDERDOC_WARNING = """

            ========================================
            Ignored 'gtceu.renderdocPath' property as this Minecraft instance is not running on Windows.
            ========================================""";

    @ApiStatus.Internal
    public static void loadRenderdoc() {
        final var renderdocPath = System.getProperty("gtceu.renderdocPath");
        if (renderdocPath != null) {
            if (Util.getPlatform() == Util.OS.WINDOWS) {
                System.load(renderdocPath);
            } else {
                GTCEu.LOGGER.warn(switch (Util.getPlatform()) {
                    case LINUX -> LINUX_RENDERDOC_WARNING;
                    case OSX -> MAC_RENDERDOC_WARNING;
                    default -> GENERIC_RENDERDOC_WARNING;
                });
            }
        }
    }

    @SubscribeEvent
    public void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GTEntityTypes.DYNAMITE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(GTEntityTypes.POWDERBARREL.get(), GTExplosiveRenderer::new);
        event.registerEntityRenderer(GTEntityTypes.INDUSTRIAL_TNT.get(), GTExplosiveRenderer::new);

        event.registerBlockEntityRenderer(GTBlockEntities.GT_SIGN.get(), SignRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.GT_HANGING_SIGN.get(), HangingSignRenderer::new);

        event.registerEntityRenderer(GTEntityTypes.BOAT.get(), c -> new GTBoatRenderer(c, false));
        event.registerEntityRenderer(GTEntityTypes.CHEST_BOAT.get(), c -> new GTBoatRenderer(c, true));
    }

    @SubscribeEvent
    public void onRegisterEntityLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (var type : GTBoat.BoatType.values()) {
            event.registerLayerDefinition(GTBoatRenderer.getBoatModelName(type), BoatModel::createBodyModel);
            event.registerLayerDefinition(GTBoatRenderer.getChestBoatModelName(type), ChestBoatModel::createBodyModel);
        }
    }

    @SubscribeEvent
    public void registerKeyBindings(RegisterKeyMappingsEvent event) {
        KeyBind.onRegisterKeyBinds(event);
    }

    @SubscribeEvent
    public void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("hud", new HudGuiOverlay());
    }

    @SubscribeEvent
    public void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GTParticleTypes.HAZARD_PARTICLE.get(), HazardParticle.Provider::new);
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        if (ConfigHolder.INSTANCE.compat.minimap.toggle.ftbChunksIntegration &&
                GTCEu.isModLoaded(GTValues.MODID_FTB_CHUNKS)) {
            FTBChunksPlugin.addEventListeners();
        }
    }

    @SubscribeEvent
    public void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new UIModelLoader());
        event.registerReloadListener(new NinePatchTexture.MetadataLoader());
    }

    @SubscribeEvent
    public void setupClient(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MENU_TYPE, UIContainerScreen::new);
        });
    }
}
