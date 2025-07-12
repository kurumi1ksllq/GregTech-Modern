package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import org.jetbrains.annotations.Nullable;

public class MachineRenderStateTransformer implements IValueTransformer<MachineRenderState> {

    @Override
    public void writeBufferPayload(FriendlyByteBuf buffer, MachineRenderState value) {
        buffer.writeInt(MachineDefinition.RENDER_STATE_REGISTRY.getId(value));
    }

    @Override
    public MachineRenderState readBufferPayload(FriendlyByteBuf buffer, @Nullable MachineRenderState currentVal) {
        return MachineDefinition.RENDER_STATE_REGISTRY.byIdOrThrow(buffer.readInt());
    }

    @Override
    public Tag serializeNBT(MachineRenderState value) {
        return MachineRenderState.CODEC.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, GTCEu.LOGGER::error);
    }

    @Override
    public MachineRenderState deserializeNBT(Tag tag, @Nullable MachineRenderState currentVal) {
        return MachineRenderState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(false, GTCEu.LOGGER::error);
    }
}
