package com.gregtechceu.gtceu.api.mui.holoui;

import com.gregtechceu.gtceu.client.mui.screen.ContainerScreenWrapper;
import com.gregtechceu.gtceu.client.mui.screen.ModularContainerMenu;
import com.gregtechceu.gtceu.client.mui.screen.ModularScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Highly experimental
 */
@ApiStatus.Experimental
public class HoloScreenEntity extends Entity {

    private ContainerScreenWrapper wrapper;
    private ModularScreen screen;
    private final Plane3D plane3D;
    private static final EntityDataAccessor<Byte> ORIENTATION = SynchedEntityData.defineId(HoloScreenEntity.class, EntityDataSerializers.BYTE);

    public HoloScreenEntity(EntityType<? extends HoloScreenEntity> type, Level level, Plane3D plane3D) {
        super(type, level);
        this.plane3D = plane3D;
    }

    public HoloScreenEntity(EntityType<? extends HoloScreenEntity> type, Level level) {
        this(type, level, new Plane3D());
    }

    public void setScreen(ModularScreen screen) {
        this.screen = screen;
        this.wrapper = new ContainerScreenWrapper(new ModularContainerMenu(-1), screen);
        this.wrapper.init(Minecraft.getInstance(), (int) this.plane3D.getWidth(), (int) this.plane3D.getHeight());
    }

    public ModularScreen getScreen() {
        return this.screen;
    }

    public ContainerScreenWrapper getWrapper() {
        return this.wrapper;
    }

    public void spawnInWorld() {
        level().addFreshEntity(this);
    }

    public void setOrientation(ScreenOrientation orientation) {
        this.getEntityData().set(ORIENTATION, (byte) orientation.ordinal());
    }

    public ScreenOrientation getOrientation() {
        return ScreenOrientation.values()[this.getEntityData().get(ORIENTATION)];
    }

    public Plane3D getPlane3D() {
        return this.plane3D;
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(ORIENTATION, (byte) ScreenOrientation.TO_PLAYER.ordinal());
    }

    @Override
    public void baseTick() {
        this.level().getProfiler().push("entityBaseTick");
        this.walkDistO = this.walkDist;
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();

        if (this.level().isClientSide) {
            this.clearFire();
        }
        this.checkBelowWorld();

        if (this.level().isClientSide) {
            int w = (int) this.plane3D.getWidth(), h = (int) this.plane3D.getHeight();
            if (w != this.wrapper.width || h != this.wrapper.height) {
                this.wrapper.resize(Minecraft.getInstance(), w, h);
            }
        }

        this.firstTick = false;
        this.level().getProfiler().pop();
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compound) {}

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compound) {}

    @Override
    protected void checkInsideBlocks() {}

    @Override
    public boolean ignoreExplosion() {
        return true;
    }

    @Override
    public boolean canTrample(@NotNull BlockState state, @NotNull BlockPos pos, float fallDistance) {
        return false;
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        return false;
    }
}
