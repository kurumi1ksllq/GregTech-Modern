package com.gregtechceu.gtceu.client.util;

import com.gregtechceu.gtceu.client.renderer.pipe.util.ColorData;
import com.gregtechceu.gtceu.client.renderer.pipe.util.SpriteInformation;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

@SideOnly(Side.CLIENT)
public class RecolorableBakedQuad extends BakedQuad {

    private final Int2ObjectOpenHashMap<RecolorableBakedQuad> cache;
    private final SpriteInformation spriteInformation;

    @Getter
    private final int color;

    /**
     * Create a new recolorable quad based off of a baked quad prototype.
     * 
     * @param prototype         the prototype.
     * @param spriteInformation the sprite information of this baked quad.
     */
    public RecolorableBakedQuad(BakedQuad prototype, SpriteInformation spriteInformation) {
        this(prototype, -1, spriteInformation);
    }

    /**
     * Create a new recolorable quad based off of a baked quad prototype and a color.
     *
     * @param prototype         the prototype.
     * @param color             the color this quad will have.
     * @param spriteInformation the sprite information of this baked quad.
     */
    public RecolorableBakedQuad(BakedQuad prototype, int color, SpriteInformation spriteInformation) {
        this(prototype, color, spriteInformation, new Int2ObjectOpenHashMap<>());
    }

    protected RecolorableBakedQuad(BakedQuad prototype, int color,
                                   SpriteInformation spriteInformation,
                                   Int2ObjectOpenHashMap<RecolorableBakedQuad> cache) {
        super(prototype.getVertices(), prototype.getTintIndex(), prototype.getDirection(),
                spriteInformation.sprite(), prototype.isShade(), prototype.hasAmbientOcclusion());
        this.spriteInformation = spriteInformation;
        this.cache = cache;
        this.color = color;

        this.gtceu$setTextureKey(prototype.gtceu$getTextureKey());
    }

    /**
     * Get a recolorable quad based off of this quad but aligned with the given color data.
     * 
     * @param data the color data.
     * @return a quad colored based on the color data.
     */
    public RecolorableBakedQuad withColor(ColorData data) {
        if (!spriteInformation.colorable()) return this;
        int argb = data.colorsARGB()[spriteInformation.colorID()];
        return cache.computeIfAbsent(argb,
                (c) -> new RecolorableBakedQuad(this, c, this.spriteInformation, this.cache));
    }
}
