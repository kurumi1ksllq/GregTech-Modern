package com.gregtechceu.gtceu.api.graphnet.net;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.graphnet.GraphClassType;
import com.gregtechceu.gtceu.api.graphnet.graph.GraphEdge;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;
import com.gregtechceu.gtceu.api.graphnet.predicate.EdgePredicateHandler;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.IPredicateTestObject;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetEdge implements INBTSerializable<CompoundTag> {

    public static final GraphClassType<NetEdge> TYPE = new GraphClassType<>(GTCEu.MOD_ID, "NetEdge",
            n -> new NetEdge());

    /**
     * For interacting with the internal graph representation ONLY, do not use or set this field otherwise.
     */
    @ApiStatus.Internal
    public @Nullable GraphEdge wrapper;

    private @Nullable EdgePredicateHandler predicateHandler;

    private @Nullable NetLogicData data;

    @Nullable
    @Contract("null->null")
    public static NetEdge unwrap(GraphEdge e) {
        return e == null ? null : e.wrapped;
    }

    public @Nullable NetNode getSource() {
        if (wrapper == null) return null;
        return wrapper.getSource().wrapped;
    }

    public @Nullable NetNode getTarget() {
        if (wrapper == null) return null;
        return wrapper.getTarget().wrapped;
    }

    public @Nullable NetNode getOppositeNode(@NotNull NetNode node) {
        if (getSource() == node) return getTarget();
        else if (getTarget() == node) return getSource();
        else return null;
    }

    public double getWeight() {
        return wrapper == null ? Double.POSITIVE_INFINITY : wrapper.getWeight();
    }

    /**
     * Should only be used on fake edges that are not registered to the graph.
     */
    public void setData(@NotNull NetLogicData data) {
        if (this.wrapper == null) this.data = data;
    }

    /**
     * This data is transient and should not be written to.
     */
    public @NotNull NetLogicData getData() {
        if (this.data == null) {
            this.data = NetLogicData.unionNullable(getSource() == null ? null : getSource().getData(),
                    getTarget() == null ? null : getTarget().getData());
            // if we can't calculate it, create a new one just to guarantee nonnullness
            if (this.data == null) this.data = new NetLogicData();
        }
        return this.data;
    }

    @NotNull
    public EdgePredicateHandler getPredicateHandler() {
        if (predicateHandler == null) predicateHandler = new EdgePredicateHandler();
        return predicateHandler;
    }

    public boolean test(IPredicateTestObject object) {
        if (predicateHandler == null) return true;
        else return predicateHandler.test(object);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        // we don't need to write our NetLogicData to NBT because we can regenerate it from our nodes
        if (predicateHandler != null && !predicateHandler.shouldIgnore())
            tag.put("Predicate", predicateHandler.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("Predicate")) {
            this.predicateHandler = new EdgePredicateHandler();
            this.predicateHandler.deserializeNBT(nbt.getList("Predicate", Tag.TAG_COMPOUND));
        }
    }

    public GraphClassType<? extends NetEdge> getType() {
        return TYPE;
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}
