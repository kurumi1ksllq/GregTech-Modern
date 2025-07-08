package com.gregtechceu.gtceu.api.machine.trait;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.syncdata.annotations.SaveField;
import com.gregtechceu.gtceu.syncdata.annotations.SyncToClient;
import com.gregtechceu.gtceu.utils.ISubscription;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public abstract class NotifiableRecipeHandlerTrait<T> extends MachineTrait implements IRecipeHandlerTrait<T> {

    protected List<Runnable> listeners = new ArrayList<>();

    @SaveField
    @SyncToClient
    @Getter
    @Setter
    protected boolean isDistinct;

    public NotifiableRecipeHandlerTrait(MetaMachine machine) {
        super(machine);
    }

    @Override
    public ISubscription addChangedListener(Runnable listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
