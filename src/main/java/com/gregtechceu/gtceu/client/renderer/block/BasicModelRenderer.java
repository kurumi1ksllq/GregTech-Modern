package com.gregtechceu.gtceu.client.renderer.block;

import com.gregtechceu.gtceu.GTCEu;

import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicModelRenderer extends IModelRenderer {

    @OnlyIn(Dist.CLIENT)
    protected Map<ModelState, BakedModel> bakedModelCache;

    public BasicModelRenderer(ResourceLocation modelLocation) {
        super(modelLocation);
    }

    @Override
    public void initRenderer() {
        if (GTCEu.isClientSide()) {
            this.bakedModelCache = new ConcurrentHashMap<>();
        }
        super.initRenderer();
    }

    @OnlyIn(Dist.CLIENT)
    public BakedModel getRotatedModel(ModelState modelState) {
        return bakedModelCache.computeIfAbsent(modelState, state -> getModel().bake(
                ModelFactory.getModeBaker(),
                this::materialMapping,
                modelState,
                modelLocation));
    }
}
