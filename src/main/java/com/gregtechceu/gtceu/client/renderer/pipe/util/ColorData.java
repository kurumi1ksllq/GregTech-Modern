package com.gregtechceu.gtceu.client.renderer.pipe.util;

import java.util.Arrays;

public record ColorData(int... colorsARGB) {

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ColorData) obj;
        return Arrays.equals(this.colorsARGB, that.colorsARGB);
    }
}
