package com.gregtechceu.gtceu.api.ui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.ui.factory.UIFactory;
import com.gregtechceu.gtceu.core.mixins.ui.accessor.AbstractContainerMenuAccessor;
import com.gregtechceu.gtceu.core.mixins.ui.accessor.SlotAccessor;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;

import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

public class UIContainerMenu<T> extends AbstractContainerMenu {

    public final static MenuType<UIContainerMenu<?>> MENU_TYPE = GTRegistries.register(BuiltInRegistries.MENU,
            GTCEu.id("ui_container"), IForgeMenuType.create(UIContainerMenu::initClient));

    @Getter
    private final Set<Slot> slotSet = new LinkedHashSet<>();

    @Getter
    private final Inventory playerInventory;
    @Getter
    private final UIFactory<T> factory;
    @Getter
    @Setter
    private T holder;
    private final boolean isClient;

    @Setter
    private Consumer<Player> closeCallback;

    public static <T> UIContainerMenu<T> initClient(int containerId, Inventory playerInventory,
                                                    @Nullable FriendlyByteBuf data) {
        if (data != null) {
            ResourceLocation uiFactoryId = data.readResourceLocation();
            // noinspection unchecked
            var factory = (UIFactory<T>) UIFactory.FACTORIES.get(uiFactoryId);

            T holder = factory.readClientHolder(data);
            return new UIContainerMenu<>(containerId, playerInventory, factory, holder, true);
        }
        // should actually never happen.
        return null;
    }

    public UIContainerMenu(int containerId, Inventory playerInventory, UIFactory<T> factory, T holder,
                           boolean isClient) {
        super(MENU_TYPE, containerId);
        this.playerInventory = playerInventory;
        this.factory = factory;
        this.holder = holder;
        this.isClient = isClient;

        // clear all old data before adding anything new
        clear();
        factory.loadServerUI(playerInventory.player, this, holder);
        init();

        this.addServerboundMessage(ServerboundSetCarriedUpdate.class, msg -> this.setCarried(msg.newCarried()));
        this.addServerboundMessage(ServerboundRemoveSyncPropertyMessage.class, msg -> super.removeProperty(msg.name()));
        this.addServerboundMessage(ClientboundRemoveSyncPropertyMessage.class, msg -> super.removeProperty(msg.name()));
    }

    @Override
    public void removeProperty(String name) {
        super.removeProperty(name);
        if (LDLib.isRemote()) {
            this.sendMessage(new ServerboundRemoveSyncPropertyMessage(name));
        } else {
            this.sendMessage(new ClientboundRemoveSyncPropertyMessage(name));
        }
    }

    /**
     * Initialize all container data like slots here.
     * Separate method from the constructors to avoid duplicate code.
     */
    public void init() {
        // don't init anything if we don't have a valid adapter.
        if (holder == null) {
            return;
        }
    }

    public void clear() {
        this.slots.clear();
        ((AbstractContainerMenuAccessor) this).gtceu$getLastSlots().clear();
        ((AbstractContainerMenuAccessor) this).gtceu$getRemoteSlots().clear();
    }

    @Override
    public void removed(Player player) {
        if (closeCallback != null) {
            closeCallback.accept(player);
        }
        super.removed(player);
    }

    // WARNING! WIDGET CHANGES SHOULD BE *STRICTLY* SYNCHRONIZED BETWEEN SERVER AND CLIENT,
    // OTHERWISE ID MISMATCH CAN HAPPEN BETWEEN ASSIGNED SLOTS!
    @Nonnull
    @Override
    public Slot addSlot(@Nonnull Slot slot) {
        var emptySlotIndex = this.slots.stream()
                .filter(it -> it instanceof EmptySlotPlaceholder)
                .mapToInt(s -> s.index)
                .findFirst();
        if (emptySlotIndex.isPresent()) {
            ((SlotAccessor) slot).gtceu$setSlotIndex(emptySlotIndex.getAsInt());
            this.slots.set(slot.index, slot);
            ((AbstractContainerMenuAccessor) this).gtceu$getLastSlots().set(slot.index, ItemStack.EMPTY);
            ((AbstractContainerMenuAccessor) this).gtceu$getRemoteSlots().set(slot.index, ItemStack.EMPTY);
            return slot;
        }
        return super.addSlot(slot);
    }

    // WARNING! WIDGET CHANGES SHOULD BE *STRICTLY* SYNCHRONIZED BETWEEN SERVER AND CLIENT,
    // OTHERWISE ID MISMATCH CAN HAPPEN BETWEEN ASSIGNED SLOTS!
    public void removeSlot(Slot slot) {
        // replace removed slot with empty placeholder to avoid list index shift
        EmptySlotPlaceholder emptySlotPlaceholder = new EmptySlotPlaceholder();
        emptySlotPlaceholder.index = slot.index;
        this.slots.set(slot.index, emptySlotPlaceholder);
        ((AbstractContainerMenuAccessor) this).gtceu$getLastSlots().set(slot.index, ItemStack.EMPTY);
        ((AbstractContainerMenuAccessor) this).gtceu$getRemoteSlots().set(slot.index, ItemStack.EMPTY);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        if (playerInventory.player.level().isClientSide) {
            return ItemStack.EMPTY;
        }

        final Slot clickedSlot = this.slots.get(i);
        boolean playerSide = isPlayerSideSlot(clickedSlot);

        // if(clickedSlot.isActive()) // todo disabled slots

        if (clickedSlot.hasItem()) {
            ItemStack stack = clickedSlot.getItem();

            final List<Slot> selectedSlots = new ArrayList<>();

            if (playerSide) {
                for (Slot c : this.slots) {
                    if (!isPlayerSideSlot(c) && c.mayPlace(stack)) {
                        selectedSlots.add(c);
                    }
                }
            } else {
                for (Slot c : this.slots) {
                    if (isPlayerSideSlot(c) && c.mayPlace(stack)) {
                        selectedSlots.add(c);
                    }
                }
            }

            if (!stack.isEmpty()) {
                for (Slot d : selectedSlots) {
                    if (d.mayPlace(stack) && d.hasItem() && movedFullStack(clickedSlot, stack, d)) {
                        return ItemStack.EMPTY;
                    }
                }

                for (Slot d : selectedSlots) {
                    if (d.mayPlace(stack)) {
                        if (d.hasItem()) {
                            if (movedFullStack(clickedSlot, stack, d)) {
                                return ItemStack.EMPTY;
                            }
                        } else {
                            int maxSize = stack.getMaxStackSize();
                            if (maxSize > d.getMaxStackSize()) {
                                maxSize = d.getMaxStackSize();
                            }

                            final ItemStack tmp = stack.copy();
                            if (tmp.getCount() > maxSize) {
                                tmp.setCount(maxSize);
                            }

                            stack.setCount(stack.getCount() - tmp.getCount());
                            d.set(tmp);
                            if (stack.getCount() <= 0) {
                                clickedSlot.set(ItemStack.EMPTY);
                                d.setChanged();

                                broadcastChanges();
                                return ItemStack.EMPTY;
                            } else {
                                broadcastChanges();
                            }
                        }
                    }
                }
            }
            clickedSlot.set(!stack.isEmpty() ? stack : ItemStack.EMPTY);
        }
        broadcastChanges();
        return ItemStack.EMPTY;
    }

    private boolean isPlayerSideSlot(Slot s) {
        return s.container == this.playerInventory;
    }

    private boolean movedFullStack(Slot clickedSlot, ItemStack stack, Slot dest) {
        final ItemStack t = dest.getItem().copy();

        if (ItemStack.isSameItemSameTags(t, stack)) {
            int maxSize = t.getMaxStackSize();
            if (maxSize > dest.getMaxStackSize()) {
                maxSize = dest.getMaxStackSize();
            }

            int placeable = maxSize - t.getCount();
            if (placeable > 0) {
                if (stack.getCount() < placeable) {
                    placeable = stack.getCount();
                }

                t.setCount(t.getCount() + placeable);
                stack.setCount(stack.getCount() - placeable);

                dest.set(t);

                if (stack.getCount() <= 0) {
                    clickedSlot.set(ItemStack.EMPTY);
                    dest.setChanged();

                    broadcastChanges();
                    return true;
                } else {
                    broadcastChanges();
                }
            }
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public Player player() {
        return playerInventory.player;
    }

    public static class EmptySlotPlaceholder extends Slot {

        public static final Container EMPTY_INVENTORY = new SimpleContainer(0);

        public EmptySlotPlaceholder() {
            super(EMPTY_INVENTORY, 0, -100000, -100000);
        }

        @Nonnull
        @Override
        public ItemStack getItem() {
            return ItemStack.EMPTY;
        }

        @Override
        public void set(@Nonnull ItemStack stack) {}

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(@Nonnull Player playerIn) {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }
    }

    public static void initType() {}

    public record ServerboundSetCarriedUpdate(ItemStack newCarried) {}

    public record ServerboundRemoveSyncPropertyMessage(String name) {}
    public record ClientboundRemoveSyncPropertyMessage(String name) {}
}
