package com.gregtechceu.gtceu.integration.kjs.events;

import com.gregtechceu.gtceu.api.events.RegisterGTMachineEvent;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;

import dev.latvian.mods.kubejs.event.EventJS;

public class RegisterGTMachineEventJS extends EventJS {

    private final RegisterGTMachineEvent event;

    public RegisterGTMachineEventJS(RegisterGTMachineEvent event) {
        this.event = event;
    }

    public MachineBuilder<?> getBuilder() {
        return this.event.getBuilder();
    }
}
