package com.gregtechceu.gtceu.api.capability;

import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;

import net.minecraft.core.Direction;

public interface IHazardParticleContainer {

    /**
     * @return if this container accepts particles from the given side
     */
    boolean inputsHazard(Direction side, MedicalCondition condition);

    /**
     * @return if this container can output particles to the given side
     */
    default boolean outputsHazard(Direction side, MedicalCondition condition) {
        return false;
    }

    /**
     * This changes the amount stored.
     *
     * @param amount   amount of particles to add (>0) or remove (<0)
     * @param simulate If true, the insertion is only simulated
     * @return amount of particles added or removed
     */
    float changeHazard(MedicalCondition condition, float amount, boolean simulate);

    /**
     * Adds specified amount of particles to this particles container
     *
     * @param particlesToAdd amount of particles to add
     * @param simulate       If true, the insertion is only simulated
     * @return amount of particles added
     */
    default float addHazard(MedicalCondition condition, float particlesToAdd, boolean simulate) {
        return changeHazard(condition, particlesToAdd, simulate);
    }

    /**
     * Removes specified amount of particles from this particles container
     *
     * @param particlesToRemove amount of particles to remove
     * @param simulate          If true, the insertion is only simulated
     * @return amount of particles removed
     */
    default float removeHazard(MedicalCondition condition, float particlesToRemove, boolean simulate) {
        return -changeHazard(condition, -particlesToRemove, simulate);
    }

    /**
     * @return amount of currently stored particles
     */
    float getHazardStored(MedicalCondition condition);

    /**
     * @return maximum amount of storable particles
     */
    float getHazardCapacity(MedicalCondition condition);

    IHazardParticleContainer DEFAULT = new IHazardParticleContainer() {

        @Override
        public boolean inputsHazard(Direction side, MedicalCondition condition) {
            return false;
        }

        @Override
        public float changeHazard(MedicalCondition condition, float amount, boolean simulate) {
            return 0;
        }

        @Override
        public float getHazardStored(MedicalCondition condition) {
            return 0;
        }

        @Override
        public float getHazardCapacity(MedicalCondition condition) {
            return 0;
        }
    };
}
