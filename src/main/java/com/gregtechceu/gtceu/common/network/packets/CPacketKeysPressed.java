package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.utils.input.KeyBind;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import it.unimi.dsi.fastutil.booleans.BooleanBooleanPair;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Deprecated
@SuppressWarnings("unchecked")
@NoArgsConstructor
public class CPacketKeysPressed implements CustomPacketPayload {

    public static final ResourceLocation ID = GTCEu.id("keys_pressed");
    public static final Type<CPacketKeysPressed> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, CPacketKeysPressed> CODEC = StreamCodec
            .ofMember(CPacketKeysPressed::encode, CPacketKeysPressed::new);

    private Object updateKeys;

    public CPacketKeysPressed(List<KeyBind> updateKeys) {
        this.updateKeys = updateKeys;
    }

    public CPacketKeysPressed(FriendlyByteBuf buf) {
        BooleanBooleanPair[] updateKeys = new BooleanBooleanPair[KeyBind.VALUES.length];
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            updateKeys[buf.readVarInt()] = BooleanBooleanPair.of(buf.readBoolean(), buf.readBoolean());
        }
        this.updateKeys = updateKeys;
    }

    public void encode(FriendlyByteBuf buf) {
        // noinspection unchecked
        List<KeyBind> updateKeys = (List<KeyBind>) this.updateKeys;
        buf.writeVarInt(updateKeys.size());
        for (KeyBind keyBind : updateKeys) {
            buf.writeVarInt(keyBind.ordinal());
            buf.writeBoolean(keyBind.isPressed());
            buf.writeBoolean(keyBind.isKeyDown());
        }
    }

    public void execute(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            KeyBind[] keybinds = KeyBind.VALUES;
            BooleanBooleanPair[] updateKeys = (BooleanBooleanPair[]) this.updateKeys;
            for (int i = 0; i < updateKeys.length; i++) {
                BooleanBooleanPair pair = updateKeys[i];
                if (pair != null) {
                    keybinds[i].update(pair.firstBoolean(), pair.secondBoolean(), player);
                }
            }
        }
    }

    @Override
    public @NotNull Type<CPacketKeysPressed> type() {
        return TYPE;
    }
}
