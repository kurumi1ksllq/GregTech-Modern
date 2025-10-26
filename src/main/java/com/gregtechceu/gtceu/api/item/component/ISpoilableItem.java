package com.gregtechceu.gtceu.api.item.component;

import com.gregtechceu.gtceu.api.item.ISpoilableItemStackMixin;
import com.gregtechceu.gtceu.common.item.SpoilableBehaviour;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * The interface items that spoil should implement.
 * <p>
 * If you are developing an addon, and want to make an {@link Item}
 * that does not extend this interface spoilable, use {@link ISpoilableItem#attachSpoilable(ISpoilableItem, ItemLike)}.
 * </p>
 * <p>
 * If you want to make an item that spoils not spoil, use {@link ISpoilableItem#unspoil(ItemLike)}.
 * </p>
 * <p>
 * Spoilable items will, as the name implies, spoil (who could've thought).
 * Due to Minecraft's limitations, items will only start spoiling only if:
 * <ul>
 * <li>It is an output of a {@link com.gregtechceu.gtceu.api.recipe.GTRecipe}</li>
 * <li>It is crafted in a crafting table</li>
 * <li>It is put into a {@link com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler} (almost any GregTech
 * container)</li>
 * <li>It enters a player's inventory and gets ticked at least once</li>
 * <li>It is dropped (exists as an entity)</li>
 * <li>Any other mod calls {@link ISpoilableItem#update(ItemStack)} on the item</li>
 * </ul>
 * If you are a developer of a mod that adds any other way to obtain items, that doesn't involve
 * any of the conditions above being true at any tick, consider adding compatibility with this feature :)
 * <br>
 * Items that don't start spoiling will simply not have spoilable NBT, and as a result, won't spoil,
 * until any of the above conditions become true.
 * <p>
 * Also due to Minecraft's limitations, merging stacks with different freshness requires overriding
 * {@link ItemStack#isSameItemSameTags(ItemStack, ItemStack)} to make the stacks have the same freshness.
 * The only exception is if one of the stacks is frozen, in which case it works as normal, except that
 * frozen stacks will be equal to non-frozen stacks if all else is equal. This is done to make filtering work correctly.
 * </p>
 * <p>
 * Spoilable stacks will be frozen if they enter a {@link com.gregtechceu.gtceu.api.gui.widget.PhantomSlotWidget},
 * to prevent stacks spoiling in filters. If you are a developer of a mod that adds filters, consider calling
 * {@link ISpoilableItem#freezeSpoiling()} on stacks entering these filters for compatibility :)
 * </p>
 * <p>
 * Note that if an item is spoilable, it does not mean that it spoils in all cases, as you can override
 * {@link ISpoilableItem#shouldSpoil()}
 * in your own implementation of the {@link ISpoilableItem} interface.
 * </p>
 * <p>
 * You do not have to make your own implementation of {@link ISpoilableItem} to use this mechanic.<br>
 * Instead, you can use {@link SpoilableBehaviour} and {@link SpoilableBehaviour#attachTo(ItemLike)} to
 * make any item from any mod spoil quite easily. This is especially useful for KubeJS devs.
 * </p>
 */
public interface ISpoilableItem {

    /**
     * Consider frozen and non-frozen spoilables equal. This is done to allow filtering by ticks remaining until
     * spoiled.<br>
     * If you want the player to have frozen stacks in their inventory, set this to {@code false} to prevent players
     * from
     * entirely bypassing the spoilage system.
     */
    boolean FROZEN_EQUALITY = true;

    /**
     * Initializes this ItemStack's spoilage timer if it wasn't initialized before.
     * Should be called when it finishes crafting, for example.
     */
    static void update(ItemStack stack) {
        ((ISpoilableItemStackMixin) (Object) stack).gtceu$updateFreshness(null, true);
    }

    /**
     * Should return the amount of ticks that this item can stay fresh.
     * The result of this method shouldn't be based on the freshness of the provided stack
     */
    long getSpoilTicks();

    /**
     * Please refrain from overriding this method unless absolutely necessary (I have no idea what will happen)
     * 
     * @return the amount of ticks left until the provided {@link ItemStack} spoils.
     *         The ticks still reduce even when the item is unloaded, and only pause if the
     *         overworld time pauses, as all tick calculations are done with overworld tick time
     * @see ISpoilableItem#setTicksUntilSpoiled(long)
     */
    long getTicksUntilSpoiled();

    /**
     * Please refrain from overriding this method unless absolutely necessary (I have no idea what will happen).
     * <br>
     * Sets the amount of ticks left until the provided {@link ItemStack} spoils.
     * This modifies the provided stack's NBT data.
     * The provided value may be more than {@link ISpoilableItem#getSpoilTicks()}
     * 
     * @see ISpoilableItem#getTicksUntilSpoiled()
     */
    void setTicksUntilSpoiled(long value);

    /**
     * Freezes the provided stack's spoiling progress until it is unfrozen by
     * {@link ISpoilableItem#unfreezeSpoiling()}.
     * Frozen stacks will NOT spoil, even if {@link ISpoilableItem#getTicksUntilSpoiled()} is {@code <= 0}.
     * This method modifies the provided stack's NBT data.
     * Calls to {@link ItemStack#isSameItemSameTags(ItemStack, ItemStack)} with a frozen stack as one of the arguments
     * will check equality of both stacks' {@link ISpoilableItem#getTicksUntilSpoiled()} values, as well as all
     * non-spoilage
     * related tags and the equality of the item itself.
     * 
     * @see ISpoilableItem#unfreezeSpoiling()
     */
    void freezeSpoiling();

    /**
     * Please refrain from overriding this method unless absolutely necessary (I have no idea what will happen).
     * <br>
     * Unfreezes the provided stack's spoiling progress. If the stack's
     * {@link ISpoilableItem#getTicksUntilSpoiled()} is {@code <= 0}, it will spoil
     * immediately after this method call.
     * This method modifies the provided stack's NBT data.
     * 
     * @see ISpoilableItem#freezeSpoiling()
     */
    void unfreezeSpoiling();

    boolean isFrozen();

    /**
     * Should return the stack to replace the provided stack with when it spoils
     */
    ItemStack spoilResult();

    /**
     * Note: returning {@code false} in this method won't stop the item from spoiling if the spoiling NBT has already
     * been initialized
     */
    boolean shouldSpoil();

    long getCreationTick();

    void setCreationTick(long tick);
}
