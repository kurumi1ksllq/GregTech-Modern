package com.gregtechceu.gtceu.common.pipelike.net.duct;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.medicalcondition.HazardStack;
import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.graphnet.logic.AbstractTransientLogicData;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicType;
import com.gregtechceu.gtceu.utils.GTUtil;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class DuctFlowLogic extends AbstractTransientLogicData<DuctFlowLogic> {

    public static final NetLogicType<DuctFlowLogic> TYPE = new NetLogicType<>(GTCEu.MOD_ID, "DuctFlow",
            DuctFlowLogic::new, new DuctFlowLogic());

    public static final int MEMORY_TICKS = WorldDuctNet.getBufferTicks();

    private final Long2ObjectOpenHashMap<Object2FloatMap<DuctTestObject>> memory = new Long2ObjectOpenHashMap<>();
    @Getter
    private HazardStack last;

    @Override
    public @NotNull NetLogicType<DuctFlowLogic> getType() {
        return TYPE;
    }

    public @NotNull Long2ObjectOpenHashMap<Object2FloatMap<DuctTestObject>> getMemory() {
        updateMemory(GTUtil.getCurrentServerTick());
        return memory;
    }

    public @NotNull Object2FloatMap<DuctTestObject> getSum() {
        Object2FloatMap<DuctTestObject> sum = new Object2FloatArrayMap<>();
        for (Object2FloatMap<DuctTestObject> list : getMemory().values()) {
            for (var entry : list.object2FloatEntrySet()) {
                sum.put(entry.getKey(), sum.getFloat(entry.getKey()) + entry.getFloatValue());
            }
        }
        return sum;
    }

    public @NotNull Object2FloatMap<DuctTestObject> getFlow(long tick) {
        updateMemory(tick);
        return memory.getOrDefault(tick, Object2FloatMaps.emptyMap());
    }

    public void recordFlow(long tick, @NotNull MedicalCondition condition, float amount) {
        recordFlow(tick, new DuctTestObject(condition), amount);
    }

    public void recordFlow(long tick, @NotNull DuctTestObject testObject, float amount) {
        updateMemory(tick);
        Object2FloatMap<DuctTestObject> map = memory.computeIfAbsent(tick, k -> new Object2FloatArrayMap<>());
        map.put(testObject, map.getFloat(testObject) + amount);
        last = testObject.recombine(amount);
    }

    private void updateMemory(long tick) {
        var iter = memory.long2ObjectEntrySet().fastIterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            if (entry.getLongKey() + MEMORY_TICKS < tick) {
                iter.remove();
            }
        }
    }
}
