package com.gregtechceu.gtceu.integration.kjs.events;

import com.gregtechceu.gtceu.api.events.RegisterGTMachineEvent;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;

import dev.latvian.mods.kubejs.event.EventJS;

public class RegisterGTMachineEventJS<T extends MachineDefinition> extends EventJS {

    private final RegisterGTMachineEvent<T> event;

    public RegisterGTMachineEventJS(RegisterGTMachineEvent<T> event) {
        this.event = event;
    }

    public MachineBuilder<T> getBuilder() {
        return this.event.getBuilder();
    }
}
