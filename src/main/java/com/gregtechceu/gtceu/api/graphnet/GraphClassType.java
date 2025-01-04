package com.gregtechceu.gtceu.api.graphnet;

import com.gregtechceu.gtceu.api.graphnet.net.IGraphNet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class GraphClassType<T> implements StringRepresentable {

    private final @NotNull String name;
    private final @NotNull Function<IGraphNet, T> supplier;

    public GraphClassType(@NotNull ResourceLocation name, @NotNull Function<IGraphNet, T> supplier) {
        this.name = name.toString();
        this.supplier = supplier;
    }

    public GraphClassType(@NotNull String namespace, @NotNull String name, @NotNull Function<IGraphNet, T> supplier) {
        this.name = namespace + ":" + name;
        this.supplier = supplier;
    }

    public final T getNew(@NotNull IGraphNet net) {
        return supplier.apply(net);
    }

    @Override
    public final @NotNull String getSerializedName() {
        return name;
    }
}
