package com.gregtechceu.gtceu.client.model;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.util.GTQuadTransformers;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public record BloomMetadataSection(boolean bloom) {

    public static final String SECTION_NAME = GTCEu.MOD_ID;
    public static final BloomMetadataSection MISSING = new BloomMetadataSection(false);

    public static boolean hasBloom(TextureAtlasSprite sprite) {
        ResourceLocation textureLoc = SpriteSource.TEXTURE_ID_CONVERTER.idToFile(sprite.contents().name());
        return hasBloom(textureLoc);
    }

    public static boolean hasBloom(ResourceLocation res) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(res);
            if (resource.isPresent()) {
                return resource.get().metadata().getSection(Serializer.INSTANCE)
                        .orElse(MISSING).bloom;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public static boolean hasBloom(BakedQuad quad, int[] ambientPackedLights) {
        if (!quad.isShade() || !quad.hasAmbientOcclusion()) {
            return true;
        }
        if (hasBloom(quad.getSprite())) {
            return true;
        }
        return ConfigHolder.INSTANCE.client.shader.emissiveTexturesHaveBloom && isEmissive(quad, ambientPackedLights);
    }

    public static boolean isEmissive(BakedQuad quad, int[] ambientPackedLights) {
        int[] quadPackedLights = GTQuadTransformers.getPackedLights(quad);

        for (int i = 0; i < 4; i++) {
            int quadLight = quadPackedLights[i];
            int qBlock = LightTexture.block(quadLight), qSky = LightTexture.sky(quadLight);

            int ambientLight = ambientPackedLights[i];
            int aBlock = LightTexture.block(ambientLight), aSky = LightTexture.sky(ambientLight);

            if (qBlock > aBlock || qSky > aSky) {
                return true;
            }
        }
        return false;
    }

    public static class Serializer implements MetadataSectionSerializer<BloomMetadataSection> {

        static BloomMetadataSection.Serializer INSTANCE = new BloomMetadataSection.Serializer();

        @NotNull
        @Override
        public String getMetadataSectionName() {
            return SECTION_NAME;
        }

        @Override
        public BloomMetadataSection fromJson(JsonObject json) {
            boolean bloom = false;
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("bloom")) {
                    JsonElement element = obj.get("bloom");
                    if (element.isJsonPrimitive() &&
                            element.getAsJsonPrimitive().isBoolean()) {
                        bloom = element.getAsBoolean();
                    }
                }
            }
            return new BloomMetadataSection(bloom);
        }
    }
}
