package com.gregtechceu.gtceu.api.pipenet.property;

import org.apache.commons.lang3.NotImplementedException;

import java.util.Objects;

public abstract class PipeSegmentProperty<T> {

    public abstract T getValue();

    public T min(T other) {
        throw new NotImplementedException("Property behaviour not implemented");
    }

    public T max(T other) {
        throw new NotImplementedException("Property behaviour not implemented");
    }

    public T sum(T other) {
        throw new NotImplementedException("Property behaviour not implemented");
    }

    public abstract PipeSegmentProperty<T> copy();

    public abstract String toString();

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PipeSegmentProperty<?> segmentProperty) {
            return Objects.equals(segmentProperty.getValue(), getValue());
        }
        return false;
    }
}
