package com.gregtechceu.gtceu.api.graphnet.logic;

import net.minecraft.nbt.ByteTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class AbstractByteLogicData<T extends AbstractByteLogicData<T>> extends NetLogicEntry<T, ByteTag> {

    private byte value;

    protected AbstractByteLogicData() {}

    protected AbstractByteLogicData(byte init) {
        this.value = init;
    }

    protected T setValue(byte value) {
        this.value = value;
        return (T) this;
    }

    public byte getValue() {
        return this.value;
    }

    @Override
    public ByteTag serializeNBT() {
        return ByteTag.valueOf(this.value);
    }

    @Override
    public void deserializeNBT(ByteTag nbt) {
        this.value = nbt.getAsByte();
    }

    @Override
    public void encode(FriendlyByteBuf buf, boolean fullChange) {
        buf.writeByte(this.value);
    }

    @Override
    public void decode(FriendlyByteBuf buf, boolean fullChange) {
        this.value = buf.readByte();
    }

    @Override
    public abstract @NotNull AbstractByteLogicData.ByteLogicType<T> getType();

    public static class ByteLogicType<T extends AbstractByteLogicData<T>> extends NetLogicType<T> {

        public ByteLogicType(@NotNull ResourceLocation name, @NotNull Supplier<@NotNull T> supplier,
                             @NotNull T defaultable) {
            super(name, supplier, defaultable);
        }

        public T getWith(byte value) {
            return getNew().setValue(value);
        }
    }
}
