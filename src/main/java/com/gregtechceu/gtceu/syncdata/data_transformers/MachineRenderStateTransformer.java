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
    public void writeToBuffer(MachineRenderState value, FriendlyByteBuf buf) {
        buf.writeId(MachineDefinition.RENDER_STATE_REGISTRY, value);
    }

    @Override
    public MachineRenderState readFromBuffer(FriendlyByteBuf buf, MachineRenderState currentValue) {
        return buf.readById(MachineDefinition.RENDER_STATE_REGISTRY);
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
