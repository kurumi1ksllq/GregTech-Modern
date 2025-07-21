package com.gregtechceu.gtceu.common.pipelike.net.duct;

import com.gregtechceu.gtceu.api.graphnet.group.PathCacheGroupData;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.path.NetPath;
import com.gregtechceu.gtceu.api.graphnet.path.PathBuilder;
import com.gregtechceu.gtceu.api.graphnet.traverse.NetIteratorSupplier;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class DuctGroupData extends PathCacheGroupData {

    public DuctGroupData(NetIteratorSupplier iteratorSupplier) {
        super(iteratorSupplier);
    }

    public DuctGroupData(NetIteratorSupplier iteratorSupplier,
                         @NotNull Reference2ReferenceOpenHashMap<NetNode, SecondaryCache> cache) {
        super(iteratorSupplier, cache);
    }

    @Override
    protected PathBuilder createBuilder(@NotNull NetNode origin) {
        return new StandardDuctPath.Builder(origin);
    }

    @Override
    protected NetPath buildSingleton(@NotNull NetNode singleton) {
        return new StandardDuctPath.SingletonDuctPath(singleton);
    }

    @Override
    protected @NotNull PathCacheGroupData buildFilteredCache(@NotNull Set<NetNode> filterNodes) {
        Reference2ReferenceOpenHashMap<NetNode, SecondaryCache> child = new Reference2ReferenceOpenHashMap<>(
                this.cache);
        child.entrySet().removeIf(entry -> {
            if (!filterNodes.contains(entry.getKey())) return true;
            SecondaryCache cache = entry.getValue();
            cache.keySet().retainAll(filterNodes);
            return cache.isEmpty();
        });
        return new DuctGroupData(iteratorSupplier, child);
    }
}
