package com.gregtechceu.gtceu.api.graphnet.logic;

import net.minecraft.nbt.FloatTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class AbstractFloatLogicData<T extends AbstractFloatLogicData<T>> extends NetLogicEntry<T, FloatTag> {

    private float value;

    protected AbstractFloatLogicData() {}

    protected AbstractFloatLogicData(float init) {
        this.value = init;
    }

    public T getWith(float value) {
        return getType().getNew().setValue(value);
    }

    @Contract("_ -> this")
    public T setValue(float value) {
        this.value = value;
        return (T) this;
    }

    public float getValue() {
        return this.value;
    }

    @Override
    public FloatTag serializeNBT() {
        return FloatTag.valueOf(this.value);
    }

    @Override
    public void deserializeNBT(FloatTag nbt) {
        this.value = nbt.getAsFloat();
    }

    @Override
    public void encode(FriendlyByteBuf buf, boolean fullChange) {
        buf.writeFloat(this.value);
    }

    @Override
    public void decode(FriendlyByteBuf buf, boolean fullChange) {
        this.value = buf.readFloat();
    }

    @Override
    public abstract @NotNull AbstractFloatLogicData.FloatLogicType<T> getType();

    public static class FloatLogicType<T extends AbstractFloatLogicData<T>> extends NetLogicType<T> {

        public FloatLogicType(@NotNull ResourceLocation name, @NotNull Supplier<@NotNull T> supplier,
                              @NotNull T defaultable) {
            super(name, supplier, defaultable);
        }

        public T getWith(float value) {
            return getNew().setValue(value);
        }
    }
}
