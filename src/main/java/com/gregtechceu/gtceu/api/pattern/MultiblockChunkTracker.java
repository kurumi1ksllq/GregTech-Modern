package com.gregtechceu.gtceu.api.pattern;

import net.minecraft.world.level.ChunkPos;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Tracks which chunks contain potentially partial multiblock structures
 */
@ApiStatus.Internal
public class MultiblockChunkTracker {

    private final Map<ChunkPos, Set<MultiblockState>> multiblockStates = new Object2ObjectOpenHashMap<>();

    /**
     * @param state the state to start tracking
     */
    public void trackPositions(@NotNull MultiblockState state) {
        state.streamCacheChunks().forEach(pos -> {
            // use CopyOnWriteArraySet to prevent ConcurrentModificationExceptions with cascading BlockState changes
            var set = multiblockStates.computeIfAbsent(pos, c -> new CopyOnWriteArraySet<>());
            set.add(state);
        });
    }

    /**
     * @param state the state to stop tracking
     */
    public void stopTracking(@NotNull MultiblockState state) {
        var iterator = multiblockStates.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var set = entry.getValue();
            set.remove(state);
            if (set.isEmpty()) {
                iterator.remove();
            }
        }
    }

    /**
     * @param chunkPos the position of the chunk
     * @return all the structures which are contained by or overlap with the chunk
     */
    public @Nullable Set<MultiblockState> getStructuresInChunk(@NotNull ChunkPos chunkPos) {
        return multiblockStates.get(chunkPos);
    }

    /**
     * Clears the stored data, to prevent Level and BE leaks
     */
    public void clear() {
        multiblockStates.clear();
    }
}
