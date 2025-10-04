package com.gregtechceu.gtceu.api.misc;

import appeng.api.stacks.AEKey;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StockingHatchList implements Iterable<StockingHatchList.AEStack> {

    private final List<AEStack> storage;
    private final int MAX_SIZE;

    public StockingHatchList(int size) {
        MAX_SIZE = size;
        storage = new ArrayList<>(size);
    }

    public boolean insert(AEKey key, long amount) {
        for (int i = 0; i < storage.size(); i++) {
            AEStack stack = storage.get(i);
            if (!key.equals(stack.key)) {
                continue;
            }
            if (amount > stack.amount) {
                // stack grew, resort the area before it
                stack.amount = amount;
                if (i > 0 && storage.get(i - 1).amount < amount) {
                    for (int j = 0; j < i; j++) {
                        if (amount > storage.get(j).amount) {
                            storage.add(j, stack);
                            storage.remove(i + 1); // remove old stack which is now shifted
                            return true;
                        }
                    }
                }
                // didn't need to move, already updated in place
                return true;
            } else if (amount < stack.amount) {
                // stack shrunk, resort the area after it
                stack.amount = amount;
                if (i + 1 < storage.size() && storage.get(i + 1).amount > amount) {
                    for (int j = i + 1; j < storage.size(); j++) {
                        if (amount > storage.get(j).amount) {
                            storage.add(j, stack);
                            storage.remove(i); // remove old stack
                            return true;
                        }
                    }
                }
                // didn't need to move, already updated in place
                return true;
            }
            // no change, shouldn't happen
            return false;
        }
        // unseen item
        for (int i = 0; i < storage.size(); i++) {
            AEStack stack = storage.get(i);
            if (amount > stack.amount && storage.size() < MAX_SIZE) {
                storage.add(i, new AEStack(key, amount));
                return true;
            }
        }
        if (storage.size() < MAX_SIZE) {
            storage.add(new AEStack(key, amount));
            return true;
        }
        // too small, no need to do anything
        return false;
    }

    public boolean remove(AEKey key) {
        for (int i = 0; i < storage.size(); i++) {
            AEStack stack = storage.get(i);
            if (stack.key.equals(key)) {
                storage.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean contains(AEKey what) {
        for (var stack : storage) {
            if (stack.getKey().equals(what)) return true;
        }
        return false;
    }

    @Override
    public Iterator<AEStack> iterator() {
        return storage.iterator();
    }

    public int size() {
        return storage.size();
    }

    public void clear() {
        storage.clear();
    }

    public static class AEStack {

        @Getter
        @Setter
        public @NotNull AEKey key;
        @Getter
        @Setter
        public long amount;

        public AEStack(AEKey key, long amount) {
            this.key = key;
            this.amount = amount;
        }
    }
}
