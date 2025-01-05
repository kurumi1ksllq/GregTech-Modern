package com.gregtechceu.gtceu.common.pipelike.net.fluid;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.graphnet.logic.AbstractTransientLogicData;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicType;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.FluidTestObject;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraftforge.fluids.FluidStack;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class FluidFlowLogic extends AbstractTransientLogicData<FluidFlowLogic> {

    public static final NetLogicType<FluidFlowLogic> TYPE = new NetLogicType<>(GTCEu.MOD_ID, "FluidFlow",
            FluidFlowLogic::new, new FluidFlowLogic());

    public static final int MEMORY_TICKS = WorldFluidNet.getBufferTicks();

    private final Long2ObjectOpenHashMap<Object2LongMap<FluidTestObject>> memory = new Long2ObjectOpenHashMap<>();
    @Getter
    private FluidStack last;

    @Override
    public @NotNull NetLogicType<FluidFlowLogic> getType() {
        return TYPE;
    }

    public @NotNull Long2ObjectOpenHashMap<Object2LongMap<FluidTestObject>> getMemory() {
        updateMemory(GTUtil.getCurrentServerTick());
        return memory;
    }

    public @NotNull Object2LongMap<FluidTestObject> getSum() {
        Object2LongMap<FluidTestObject> sum = new Object2LongArrayMap<>();
        for (Object2LongMap<FluidTestObject> list : getMemory().values()) {
            for (var entry : list.object2LongEntrySet()) {
                sum.put(entry.getKey(), sum.getLong(entry.getKey()) + entry.getLongValue());
            }
        }
        return sum;
    }

    public @NotNull Object2LongMap<FluidTestObject> getFlow(long tick) {
        updateMemory(tick);
        return memory.getOrDefault(tick, Object2LongMaps.emptyMap());
    }

    public void recordFlow(long tick, @NotNull FluidStack flow) {
        recordFlow(tick, new FluidTestObject(flow), flow.getAmount());
    }

    public void recordFlow(long tick, @NotNull FluidTestObject testObject, int amount) {
        updateMemory(tick);
        Object2LongMap<FluidTestObject> map = memory.computeIfAbsent(tick, k -> new Object2LongArrayMap<>());
        map.put(testObject, map.getLong(testObject) + amount);
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
