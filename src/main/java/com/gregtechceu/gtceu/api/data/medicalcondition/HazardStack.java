package com.gregtechceu.gtceu.api.data.medicalcondition;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class HazardStack {

    @Getter
    private final MedicalCondition medicalCondition;
    @Getter
    private float amount;

    public void grow(float amount) {
        this.amount += amount;
    }

    public HazardStack copy() {
        return new HazardStack(medicalCondition, amount);
    }
}
