---
title: Spoilage
---

**Spoilage** is a mechanic that allows items to *spoil*.<br>
Spoilable items spoil based on the amount of ticks that passed from their creation,
or, more specifically, from one of these events (due to Minecraft's limitations):

- The item was crafted in a GregTech recipe
- The item was crafted in a crafting table
- The item was in a GregTech inventory at any point in time
- The item was in a player's inventory for at least 1 tick
- The item was dropped
- `ISpoilableItem.update(ItemStack)` was called

If you want to make an item spoil, you need to attack the `ISpoilableItem` capability to it.
Please note that the spoilage timer still decrements even if the stack is in an unloaded chunk.

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = ExampleMod.MOD_ID)
public class Example {
        
        // Make diamonds spoil into dirt in 100 seconds, apples into jigsaws in 35 seconds
        @SubscribeEvent
        public static void attachSpoilables(AttachCapabilitiesEvent<ItemStack> event) {
            ResourceLocation id = GTCEu.id("spoilable");
            ItemStack stack = event.getObject();
            if (stack.is(Items.DIAMOND)) {
                event.addCapability(id, new SpoilableBehaviour(Items.DIRT, 20 * 100).toCapProvider(stack));
            } else if (stack.is(Items.APPLE)) {
                event.addCapability(id, new SpoilableBehaviour(Items.JIGSAW, 20 * 35).toCapProvider(stack));
            }
        }
        
        public void getAndSetValuesAndStuff(ItemStack stack) {
            ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(stack);
            // If spoilable is null, it means the stack cannot spoil
            if (spoilable != null) {
                // Get amount of ticks until a completely fresh stack spoils
                long totalTicks = spoilable.getSpoilTicks(stack);
                // Get amount of ticks until this stack spoils (may be more than the previous value in some cases)
                long ticksRemaining = spoilable.getTicksUntilSpoiled(stack);
                // Get the stack this stack spoils into
                ItemStack spoilResult = spoilable.spoilResult(stack);
                // Get whether this stack should START spoiling
                boolean shouldStartSpoiling = spoilable.shouldSpoil(stack);
                // Get the amount of ticks until this stack spoils (may be more than spoilable.getSpoilTicks(stack))
                spoilable.setTicksUntilSpoiled(stack, 12345);
                // Freeze the spoiling progress of this stack
                spoilable.freezeSpoiling(stack);
                // Unfreeze the spoiling progress of this stack
                spoilable.unfreezeSpoiling(stack);
            }
        }
        
        public void makeStackStartSpoiling(ItemStack stack) {
            // If for some reason the stack still hasn't started spoiling, you can start the spoiling progress using this
            // That may happen if it is the result of a non-GT recipe and not a crafting result for example
            ISpoilableItem.update(stack);
        }

        public void disableFrozenAndNonFrozenEquality() {
            // If you want the player to have frozen stacks in their inventory, do this
            // A side effect of this is that filtering by ticks remaining until spoiled will no longer work
            ISpoilableItem.FROZEN_EQUALITY = false;
        }
    }
    ```

!!! warning "Items may spoil in other mods' filters (even if they are phantom slots)."

### Frozen stacks

To freeze a stack's spoiling progress, you can use `spoilable.freezeSpoiling(stack)`, and `spoilable.unfreezeSpoiling(stack)`
to unfreeze. A frozen stack's freshness will never be changed unless `spoilable.setTicksUntilSpoiled(stack, value)` is called.
Currently, a stack is frozen only if it is in a phantom slot (in a GregTech filter).
!!! warning "`ItemStack.isSameItemSameTag` behaviour is completely different for spoilables"

    `ItemStack.isSameItemSameTag` returns `true` if:

    - Both items are not frozen and:
        - They are the same item
        - They have the same NBT
        - **Their ticks until spoiling are averaged before the equality check**
    - One of the items is frozen and:
        - They are the same item
        - They have the same non-spoilage-related NBT
        - **One of them MAY be not frozen, and they will still be equal if `ISpoilableItem.FROZEN_EQUALITY` is `true`**
        - **Their ticks until spoiling are NOT modified in any way in this method**

    !!! info "The following only applies if `ISpoilableItem.FROZEN_EQUALITY` is `true`:"
        That means that frozen and non-frozen spoilables may stack, this is done mostly to make filtering by remaining ticks possible.
        **Please prevent the player from having direct access to frozen stacks, as they could use them to bypass the spoiling system entirely.**

### Spoilables in recipes

If a GT recipe that does not have spoilable ingredients outputs a spoilable, it is outputted at full freshness.
If a GT recipe that has spoilable ingredients outputs a spoilable, it outputs it at the freshness level equal to the average
freshness of the ingredients. This can be overridden by setting `keepSpoilingProgress` (a parameter of the GTRecipe) to `false`.
<br><br>
Results of crafts in a crafting table are always outputted fully fresh.

!!! note

    Items will spoil in machine inputs, and there's no way to automatically remove items from inputs.
