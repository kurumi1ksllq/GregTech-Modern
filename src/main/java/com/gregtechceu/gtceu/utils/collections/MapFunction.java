package com.gregtechceu.gtceu.utils.collections;

import java.util.Map;

public interface MapFunction {

    <K, V> Map<K, V> createMap(int size);
}
