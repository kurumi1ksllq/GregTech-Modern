package com.gregtechceu.gtceu.client.renderer.pipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block.PipeMaterialBlock;
import com.gregtechceu.gtceu.client.model.BaseBakedModel;
import com.gregtechceu.gtceu.client.renderer.pipe.util.MaterialModelSupplier;
import com.gregtechceu.gtceu.client.util.GTQuadTransformers;
import com.gregtechceu.gtceu.client.util.ModelUtils;
import com.gregtechceu.gtceu.client.util.RecolorableBakedQuad;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static com.gregtechceu.gtceu.api.machine.IMachineBlockEntity.*;

public class PipeModelRedirector extends BaseBakedModel {

    private final boolean ambientOcclusion;
    @Getter
    private final boolean gui3d;

    public final MaterialModelSupplier supplier;
    public final Function<ItemStack, Material> stackMaterialFunction;

    @Getter
    private final ModelResourceLocation loc;
    @Getter
    @Setter
    private TextureAtlasSprite defaultParticleIcon = null;

    private final FakeItemOverrides fakeItemOverrideList = new FakeItemOverrides();

    public PipeModelRedirector(ModelResourceLocation loc, MaterialModelSupplier supplier,
                               Function<ItemStack, Material> stackMaterialFunction) {
        this(loc, supplier, stackMaterialFunction, true, true);
    }

    public PipeModelRedirector(ModelResourceLocation loc, MaterialModelSupplier supplier,
                               Function<ItemStack, Material> stackMaterialFunction,
                               boolean ambientOcclusion, boolean gui3d) {
        this.loc = loc;
        this.supplier = supplier;
        this.stackMaterialFunction = stackMaterialFunction;
        this.ambientOcclusion = ambientOcclusion;
        this.gui3d = gui3d;

        PipeModelRegistry.MODELS.put(loc, this);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource rand, @NotNull ModelData data,
                                             @Nullable RenderType renderType) {
        if (state == null) {
            return Collections.emptyList();
        }

        Material mat = Objects.requireNonNullElse(data.get(PipeRenderProperties.MATERIAL_PROPERTY), GTMaterials.NULL);
        if (mat.isNull() && state.getBlock() instanceof PipeMaterialBlock block) {
            mat = block.material;
        }
        AbstractPipeModel<?> model = supplier.getModel(mat);
        if (model == null) {
            return Collections.emptyList();
        }
        List<BakedQuad> quads = model.getQuads(state, side, rand, data, renderType);
        for (ListIterator<BakedQuad> iter = quads.listIterator(); iter.hasNext();) {
            BakedQuad quad = iter.next();
            if (quad instanceof RecolorableBakedQuad recolorable) {
                iter.set(GTQuadTransformers.setColor(recolorable, recolorable.getColor(), true));
            }
        }

        return quads;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                           @NotNull BlockState state, @NotNull ModelData modelData) {
        return modelData.derive()
                .with(MODEL_DATA_LEVEL, level)
                .with(MODEL_DATA_POS, pos)
                .with(MODEL_DATA_STATE, state)
                .build();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return ambientOcclusion;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        AbstractPipeModel<?> model = supplier.getModel(GTMaterials.NULL);
        if (model != null) {
            return model.getParticleIcon(ModelData.EMPTY);
        } else if (defaultParticleIcon != null) {
            return defaultParticleIcon;
        } else {
            return ModelUtils.getBlockSprite(GTCEu.id("block/pipe/pipe_side"));
        }
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        Material mat = Objects.requireNonNullElse(data.get(PipeRenderProperties.MATERIAL_PROPERTY), GTMaterials.NULL);
        BlockState state = data.get(MODEL_DATA_STATE);

        if (mat.isNull() && state != null && state.getBlock() instanceof PipeMaterialBlock block) {
            mat = block.material;
        }
        AbstractPipeModel<?> model = supplier.getModel(mat);
        if (model != null) {
            return model.getParticleIcon(data);
        } else {
            return getParticleIcon();
        }
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return fakeItemOverrideList;
    }

    @FunctionalInterface
    public interface ModelRedirectorSupplier {

        PipeModelRedirector create(ModelResourceLocation loc, MaterialModelSupplier supplier,
                                   Function<ItemStack, Material> stackMaterialFunction);
    }

    protected static class FakeItemOverrides extends ItemOverrides {

        @Nullable
        @Override
        public BakedModel resolve(@NotNull BakedModel originalModel, @NotNull ItemStack stack,
                                  @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            if (originalModel instanceof PipeModelRedirector model) {
                PipeItemModel<?> item = model.supplier.getModel(model.stackMaterialFunction.apply(stack))
                        .getItemModel(model, stack, level, entity);
                if (item != null) return item;
            }
            return originalModel;
        }
    }
}
