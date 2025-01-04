package com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity;

import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jetbrains.annotations.NotNull;

public class PipeCapabilityWrapper implements ICapabilityProvider {

    protected byte activeMask;
    protected final PipeBlockEntity owner;
    protected final WorldPipeNode node;

    protected final Object2ObjectMap<Capability<?>, IPipeCapabilityObject> capabilities;

    protected final int inactiveKey;
    protected final int activeKey;

    public PipeCapabilityWrapper(@NotNull PipeBlockEntity owner, @NotNull WorldPipeNode node,
                                 Object2ObjectMap<Capability<?>, IPipeCapabilityObject> capabilities,
                                 int inactiveKey, int activeKey) {
        this.owner = owner;
        this.node = node;
        this.inactiveKey = inactiveKey;
        this.activeKey = activeKey;
        this.capabilities = capabilities;
        for (IPipeCapabilityObject o : capabilities.values()) {
            o.init(owner, this);
        }
    }

    public void invalidate() {}

    public void setActive(@NotNull Direction facing) {
        if (!isActive(facing)) {
            setActiveInternal(facing);
        }
    }

    protected void setActiveInternal(@NotNull Direction facing) {
        this.activeMask |= 1 << facing.ordinal();
        this.node.setSortingKey(this.activeMask > 0 ? activeKey : inactiveKey);
        this.owner.notifyBlockUpdate();
    }

    public void setIdle(@NotNull Direction facing) {
        if (isActive(facing)) {
            setIdleInternal(facing);
        }
    }

    protected void setIdleInternal(@NotNull Direction facing) {
        this.activeMask &= ~(1 << facing.ordinal());
        this.node.setSortingKey(this.activeMask > 0 ? activeKey : inactiveKey);
        this.owner.notifyBlockUpdate();
    }

    public boolean isActive(@NotNull Direction facing) {
        return (this.activeMask & 1 << facing.ordinal()) > 0;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, Direction facing) {
        IPipeCapabilityObject obj = capabilities.get(capability);
        if (obj == null) return LazyOptional.empty();
        return obj.getCapability(capability, facing);
    }
}
