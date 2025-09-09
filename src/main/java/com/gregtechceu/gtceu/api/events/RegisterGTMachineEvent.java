package com.gregtechceu.gtceu.api.events;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;

import net.minecraftforge.eventbus.api.Event;

import lombok.Getter;

public class RegisterGTMachineEvent<T extends MachineDefinition> extends Event {

    @Getter
    private final MachineBuilder<T> builder;

    public RegisterGTMachineEvent(MachineBuilder<T> builder) {
        this.builder = builder;
    }
}
