package com.gregtechceu.gtceu.api.events;

import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import lombok.Getter;

public class RegisterGTMachineEvent extends Event implements IModBusEvent {

    @Getter
    private final MachineBuilder<?> builder;

    public RegisterGTMachineEvent(MachineBuilder<?> builder) {
        this.builder = builder;
    }
}
