package com.gregtechceu.gtceu.api.mui.utils.fakeworld;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.NotNull;

public class DummyLevelData implements WritableLevelData {

    @Getter
    private final boolean hardcore;
    @Getter
    private final GameRules gameRules;
    private final boolean isFlat;
    @Getter
    @Setter
    private int xSpawn;
    @Getter
    @Setter
    private int ySpawn;
    @Getter
    @Setter
    private int zSpawn;
    @Getter
    @Setter
    private float spawnAngle;
    @Getter
    @Setter
    private long gameTime;
    @Getter
    @Setter
    private long dayTime;
    @Getter
    @Setter
    private boolean raining;
    @Getter
    @Setter
    private Difficulty difficulty;
    @Getter
    @Setter
    private boolean difficultyLocked;

    public DummyLevelData() {
        this.difficulty = Difficulty.NORMAL;
        this.hardcore = false;
        this.isFlat = false;
        this.gameRules = new GameRules();
    }

    public void setSpawn(BlockPos spawnPoint, float angle) {
        this.xSpawn = spawnPoint.getX();
        this.ySpawn = spawnPoint.getY();
        this.zSpawn = spawnPoint.getZ();
        this.spawnAngle = angle;
    }

    public boolean isThundering() {
        return false;
    }

}
