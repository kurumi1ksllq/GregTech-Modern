package com.gregtechceu.gtceu.api.mui.factory;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * See {@link GuiData} for an explanation for what this is for.
 */
public class PosGuiData extends GuiData {

    private static final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    @Getter
    private final int x, y, z;

    public PosGuiData(Player player, int x, int y, int z) {
        super(player);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getSquaredDistance(double x, double y, double z) {
        double dx = this.x + 0.5 - x;
        double dy = this.y + 0.5 - y;
        double dz = this.z + 0.5 - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double getSquaredDistance(Vec3 pos) {
        return getSquaredDistance(pos.x, pos.y, pos.z);
    }

    public double getDistance(double x, double y, double z) {
        return Math.sqrt(getSquaredDistance(x, y, z));
    }

    public double getSquaredDistance(Entity entity) {
        return getSquaredDistance(entity.position());
    }

    public double getDistance(Entity entity) {
        return Math.sqrt(getSquaredDistance(entity));
    }

    public BlockPos getBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }

    public BlockEntity getBlockEntity() {
        pos.set(this.x, this.y, this.z);
        return getLevel().getBlockEntity(pos);
    }
}
