package com.gregtechceu.gtceu.common.pipelike.net.duct;

import com.gregtechceu.gtceu.api.data.medicalcondition.HazardStack;
import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.IPredicateTestObject;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

public final class DuctTestObject implements IPredicateTestObject, Predicate<MedicalCondition> {

    public final MedicalCondition condition;

    private final int hash;

    public DuctTestObject(@NotNull MedicalCondition condition) {
        this.condition = condition;
        this.hash = this.condition.hashCode();
    }

    @Override
    @Contract(" -> new")
    public @NotNull HazardStack recombine() {
        return new HazardStack(condition, 1);
    }

    @Contract("_ -> new")
    public @NotNull HazardStack recombine(float amount) {
        return new HazardStack(condition, amount);
    }

    @Override
    public boolean test(@Nullable MedicalCondition c) {
        return c != null && c == condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuctTestObject that = (DuctTestObject) o;
        return Objects.equals(condition, that.condition);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
