package com.gregtechceu.gtceu.api.pattern;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Checks multiblock structures periodically, off-thread.
 */
@ApiStatus.Internal
public final class ConcurrentStructureChecker {

    private static final AtomicInteger activelyChecking = new AtomicInteger();

    private final Map<IMultiController, Future<?>> futures = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor((r) -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("GTCEu Multiblock Structure Checker");
        return thread;
    });

    /**
     * Schedule the controller for structure checks
     *
     * @param controller the controller to schedule
     */
    public void scheduleChecking(@NotNull IMultiController controller) {
        futures.computeIfAbsent(controller, this::scheduleCheck);
    }

    /**
     * Schedules the periodic check task for a multiblock
     */
    private @NotNull Future<?> scheduleCheck(@NotNull IMultiController controller) {
        return executor.scheduleAtFixedRate(() -> checkStructure(controller), 0, 1, TimeUnit.SECONDS);
    }

    /**
     * @param controller the controller to check
     */
    private void checkStructure(@NotNull IMultiController controller) {
        if (!GTCEu.canGetServerLevel()) {
            return;
        }
        activelyChecking.getAndIncrement();
        try {
            controller.checkPatternOffThread();
        } finally {
            activelyChecking.getAndDecrement();
        }
    }

    /**
     * Cancels structure checks for the controller
     *
     * @param controller the controller to cancel
     */
    public void cancelChecking(@NotNull IMultiController controller) {
        var future = futures.remove(controller);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * @return if the checker is currently checking a structure
     */
    public static boolean isCurrentlyChecking() {
        return activelyChecking.get() > 0;
    }

    /**
     * Shuts down the executor
     */
    public void shutdown() {
        executor.shutdownNow();
        activelyChecking.set(0);
    }
}
