package com.gregtechceu.gtceu.common.pipelike.net.duct;

import com.gregtechceu.gtceu.api.graphnet.group.PathCacheGroupData;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.path.NetPath;
import com.gregtechceu.gtceu.api.graphnet.path.PathBuilder;
import com.gregtechceu.gtceu.api.graphnet.traverse.NetIteratorSupplier;
import com.gregtechceu.gtceu.common.pipelike.net.energy.StandardEnergyPath;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class DuctGroupData extends PathCacheGroupData {

    public DuctGroupData(NetIteratorSupplier iteratorSupplier) {
        super(iteratorSupplier);
    }

    public DuctGroupData(NetIteratorSupplier iteratorSupplier,
                         @NotNull Object2ObjectOpenHashMap<NetNode, SecondaryCache> cache) {
        super(iteratorSupplier, cache);
    }

    @Override
    protected PathBuilder createBuilder(@NotNull NetNode origin) {
        return new StandardEnergyPath.Builder(origin);
    }

    @Override
    protected NetPath buildSingleton(@NotNull NetNode singleton) {
        return new StandardEnergyPath.SingletonEnergyPath(singleton);
    }

    @Override
    protected @NotNull PathCacheGroupData buildFilteredCache(@NotNull Set<NetNode> filterNodes) {
        Object2ObjectOpenHashMap<NetNode, SecondaryCache> child = new Object2ObjectOpenHashMap<>(this.cache);
        child.entrySet().removeIf(entry -> {
            if (!filterNodes.contains(entry.getKey())) return true;
            SecondaryCache cache = entry.getValue();
            cache.keySet().retainAll(filterNodes);
            return cache.isEmpty();
        });
        return new DuctGroupData(iteratorSupplier, child);
    }
}
