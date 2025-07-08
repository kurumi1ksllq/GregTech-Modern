package com.gregtechceu.gtceu.syncdata.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FieldDataModifier {

    enum ModifyTarget {
        SAVE_NBT,
        LOAD_NBT
    }

    String fieldName();

    ModifyTarget target();
}
