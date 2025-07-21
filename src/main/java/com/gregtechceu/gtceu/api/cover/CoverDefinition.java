package com.gregtechceu.gtceu.api.cover;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.client.renderer.pipe.cover.CoverRenderer;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class CoverDefinition {

    public interface CoverBehaviourProvider {

        CoverBehavior create(CoverDefinition definition, ICoverable coverable, Direction side);
    }

    public interface TieredCoverBehaviourProvider {

        CoverBehavior create(CoverDefinition definition, ICoverable coverable, Direction side, int tier);
    }

    @Getter
    private final ResourceLocation id;
    private final CoverBehaviourProvider behaviorCreator;
    @Getter
    private final @Nullable Supplier<CoverRenderer> coverRenderer;

    public CoverDefinition(ResourceLocation id, CoverBehaviourProvider behaviorCreator,
                           Supplier<Supplier<CoverRenderer>> coverRenderer) {
        this.behaviorCreator = behaviorCreator;
        this.id = id;
        if (GTCEu.isClientSide()) {
            this.coverRenderer = ClientHelper.initRenderer(coverRenderer);
        } else {
            this.coverRenderer = null;
        }
    }

    public CoverBehavior createCoverBehavior(ICoverable coverHolder, Direction side) {
        return behaviorCreator.create(this, coverHolder, side);
    }

    private static class ClientHelper {

        private static Supplier<ICoverRenderer> initRenderer(Supplier<Supplier<CoverRenderer>> coverRenderer) {
            CoverRenderer value = coverRenderer.get().get();
            return () -> value;
        }
    }
}
