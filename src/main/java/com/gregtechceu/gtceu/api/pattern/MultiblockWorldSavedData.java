package com.gregtechceu.gtceu.api.pattern;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * World-associated data to manage multiblock structure checks
 */
@ApiStatus.Internal
public final class MultiblockWorldSavedData extends SavedData {

    @Getter
    private final MultiblockChunkTracker tracker = new MultiblockChunkTracker();
    @Getter
    private final ConcurrentStructureChecker checker = new ConcurrentStructureChecker();

    private MultiblockWorldSavedData() {}

    private MultiblockWorldSavedData(@NotNull CompoundTag tag) {
        this();
    }

    /**
     * @param serverLevel the level associated with the data
     * @return the existing data, or new data if it did not exist
     */
    public static @NotNull MultiblockWorldSavedData getOrCreate(@NotNull ServerLevel serverLevel) {
        return serverLevel.getDataStorage()
                .computeIfAbsent(MultiblockWorldSavedData::new, MultiblockWorldSavedData::new,
                        String.format("%s_multiblock", GTCEu.MOD_ID));
    }

    /**
     * Clears out the in-memory data, and shuts down executors
     */
    public void shutdown() {
        tracker.clear();
        checker.shutdown();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compound) {
        return compound;
    }
}
