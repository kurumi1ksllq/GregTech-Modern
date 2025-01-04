package com.gregtechceu.gtceu.api.graphnet.net;

import com.gregtechceu.gtceu.api.graphnet.GraphClassType;
import com.gregtechceu.gtceu.api.graphnet.graph.GraphVertex;
import com.gregtechceu.gtceu.api.graphnet.group.NetGroup;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class NetNode implements ITagSerializable<CompoundTag>, IContentChangeAware {

    @Getter
    @Setter
    public Runnable onContentsChanged = () -> {};

    /**
     * For interacting with the internal graph representation ONLY, do not use or set this field otherwise.
     */
    @ApiStatus.Internal
    public GraphVertex wrapper;

    /**
     * Sorts nodes into distinct groups in NetGroups for later use.
     */
    @Getter
    protected int sortingKey = 0;

    @Getter
    private final @NotNull IGraphNet net;
    @Getter
    private final @NotNull NetLogicData data;
    @Setter
    private @Nullable NetGroup group = null;

    public NetNode(@NotNull IGraphNet net) {
        this.net = net;
        this.data = net.getDefaultNodeData();
    }

    /**
     * Sets the distinct group in a NetGroup this node will be sorted into.
     */
    public void setSortingKey(int key) {
        if (key != sortingKey) {
            NetGroup group = getGroupUnsafe();
            if (group != null) group.notifySortingChange(this, sortingKey, key);
            sortingKey = key;
        }
    }

    public boolean traverse(long queryTick, boolean simulate) {
        return true;
    }

    @Nullable
    @Contract("null->null")
    public static NetNode unwrap(GraphVertex n) {
        return n == null ? null : n.wrapped;
    }

    @NotNull
    public NetGroup getGroupSafe() {
        if (this.group == null) {
            new NetGroup(this.getNet()).addNode(this);
            // addNodes automatically sets our group to the new group
        }
        return this.group;
    }

    @Nullable
    public NetGroup getGroupUnsafe() {
        return this.group;
    }

    /**
     * Use this to remove references that would keep this node from being collected by the garbage collector.
     * This is called when a node is removed from the graph and should be discarded.
     */
    public void onRemove() {}

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Data", this.data.serializeNBT());
        tag.putInt("SortingKey", sortingKey);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.sortingKey = nbt.getInt("SortingKey");
        this.data.clearData();
        this.data.deserializeNBT((ListTag) nbt.get("Data"));
    }

    /**
     * Used to determine if two nodes are equal, for graph purposes.
     * Should not change over the lifetime of a node, except when {@link #deserializeNBT(CompoundTag)} is called.
     * 
     * @return equivalency data. Needs to work with {@link Objects#equals(Object, Object)}
     */
    public abstract Object getEquivalencyData();

    public abstract @NotNull GraphClassType<? extends NetNode> getType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetNode node = (NetNode) o;
        return Objects.equals(getEquivalencyData(), node.getEquivalencyData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEquivalencyData());
    }
}
