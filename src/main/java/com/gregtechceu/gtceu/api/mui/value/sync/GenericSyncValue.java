package com.gregtechceu.gtceu.api.mui.value.sync;

import com.gregtechceu.gtceu.api.mui.utils.serialization.IByteBufAdapter;
import com.gregtechceu.gtceu.api.mui.utils.serialization.IByteBufDeserializer;
import com.gregtechceu.gtceu.api.mui.utils.serialization.IByteBufSerializer;
import com.gregtechceu.gtceu.api.mui.utils.serialization.IEquals;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GenericSyncValue<T> extends ValueSyncHandler<T> {

    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final IByteBufDeserializer<T> deserializer;
    private final IByteBufSerializer<T> serializer;
    private final IEquals<T> equals;
    private T cache;

    public GenericSyncValue(@NotNull Supplier<T> getter,
                            @Nullable Consumer<T> setter,
                            @NotNull IByteBufAdapter<T> adapter) {
        this(getter, setter, adapter, adapter, adapter);
    }

    public GenericSyncValue(@NotNull Supplier<T> getter,
                            @Nullable Consumer<T> setter,
                            @NotNull IByteBufDeserializer<T> deserializer,
                            @NotNull IByteBufSerializer<T> serializer) {
        this(getter, setter, deserializer, serializer, null);
    }

    public GenericSyncValue(@NotNull Supplier<T> getter,
                            @NotNull IByteBufAdapter<T> adapter) {
        this(getter, null, adapter, adapter, adapter);
    }

    public GenericSyncValue(@NotNull Supplier<T> getter,
                            @NotNull IByteBufDeserializer<T> deserializer,
                            @NotNull IByteBufSerializer<T> serializer) {
        this(getter, null, deserializer, serializer, null);
    }

    public GenericSyncValue(@NotNull Supplier<T> getter,
                            @Nullable Consumer<T> setter,
                            @NotNull IByteBufDeserializer<T> deserializer,
                            @NotNull IByteBufSerializer<T> serializer,
                            @Nullable IEquals<T> equals) {
        this.getter = Objects.requireNonNull(getter);
        this.cache = getter.get();
        this.setter = setter;
        this.deserializer = Objects.requireNonNull(deserializer);
        this.serializer = Objects.requireNonNull(serializer);
        this.equals = equals == null ? Objects::equals : IEquals.wrapNullSafe(equals);
    }

    @Override
    public T getValue() {
        return this.cache;
    }

    @Override
    public void setValue(T value, boolean setSource, boolean sync) {
        this.cache = value;
        if (setSource && this.setter != null) {
            this.setter.accept(value);
        }
        if (sync) {
            sync(0, this::write);
        }
    }

    @Override
    public boolean updateCacheFromSource(boolean isFirstSync) {
        T t = this.getter.get();
        if (isFirstSync || !this.equals.areEqual(this.cache, t)) {
            setValue(t, false, false);
            return true;
        }
        return false;
    }

    @Override
    public void write(FriendlyByteBuf buffer) throws IOException {
        this.serializer.serialize(buffer, this.cache);
    }

    @Override
    public void read(FriendlyByteBuf buffer) throws IOException {
        setValue(this.deserializer.deserialize(buffer), true, false);
    }
}
