---
title: Spoilage
---

**Spoilage** is a mechanic that allows items to, well, *spoil*.\
Spoilable items spoil based on the amount of ticks that passed from their creation,
or, more specifically, from one of these events (due to Minecraft's limitations):
 - The item was crafted in a GregTech recipe
 - The item was crafted in a crafting table
 - The item was in a GregTech inventory at any point in time
 - The item was in a player's inventory for at least 1 tick
 - The item was dropped
 - `ISpoilableItem.update(ItemStack, null)` was called

If you want to make an item spoil, you can either make it implement the `ISpoilableItem` interface, or
attach an `ISpoilableItem` to it. Here's some examples:
```java
public class Example {
    public void attachSpoilables() {
        // make diamonds spoil into dirt in 100 seconds
        new SpoilableBehaviour(Items.DIRT, 20*100).attachTo(Items.DIAMOND);
    }
    
    public void removeSpoilables() {
        // make diamonds not spoil anymore
        ISpoilableItem.unspoil(Items.DIAMOND);
    }
    
    public void everythingSpoilsRandomly() {
        GTValues.DEFAULT_SPOIL_BEHAVIOUR = item -> {
            List<Item> allItems = ForgeRegistries.ITEMS.getValues().stream().toList();
            Item randomItem = allItems.get(GTValues.RNG.nextIntBetweenInclusive(0, allItems.size() - 1));
            // make everything spoil every 50 seconds
            return new SpoilableBehaviour(randomItem, 50*20);
        };
    }
}
```