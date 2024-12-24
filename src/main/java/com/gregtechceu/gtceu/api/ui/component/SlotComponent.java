package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.base.BaseUIComponent;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.ingredient.ClickableIngredientSlot;
import com.gregtechceu.gtceu.api.ui.parsing.UIModel;
import com.gregtechceu.gtceu.api.ui.parsing.UIParsing;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.util.pond.UISlotExtension;
import com.gregtechceu.gtceu.core.mixins.ui.accessor.AbstractContainerMenuAccessor;
import com.gregtechceu.gtceu.core.mixins.ui.accessor.SlotAccessor;
import com.gregtechceu.gtceu.integration.xei.entry.EntryList;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemStackList;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemEntryHandler;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemStackHandler;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.lwjgl.opengl.GL11;
import org.w3c.dom.Element;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@Accessors(fluent = true, chain = true)
public class SlotComponent extends BaseUIComponent implements ClickableIngredientSlot<ItemStack> {

    @Getter
    @Setter
    private int index;
    @Getter
    @Setter
    protected MutableSlotWrapper slot;

    /**
     * Override insertion logic. null for the slot's default.
     */
    @Nullable
    @Getter
    @Setter
    protected Boolean canInsert;

    /**
     * Override extraction logic. null for the slot's default.
     */
    @Nullable
    @Getter
    @Setter
    protected Boolean canExtract;
    @Setter
    protected Function<ItemStack, ItemStack> itemHook;
    @Setter
    protected Runnable changeListener;
    @Getter
    @Setter
    protected IO ingredientIO;
    @Getter
    @Setter
    protected UITexture backgroundTexture = GuiTextures.SLOT;
    @Getter
    @Setter
    protected UITexture overlayTexture;
    @Setter
    @Getter
    protected float recipeViewerChance = 1f;

    @Setter
    protected boolean drawContents = true;
    @Setter
    protected boolean drawTooltip = true;

    private boolean slotFinalized = false;

    private SlotComponent() {
        this.sizing(Sizing.fixed(18));
    }

    protected SlotComponent(int index) {
        this();
        this.index = index;
        this.slot = new MutableSlotWrapper(new UIContainerMenu.EmptySlotPlaceholder());
    }

    protected SlotComponent(IItemHandlerModifiable itemHandler, int index) {
        this();
        this.index = index;
        this.slot = new MutableSlotWrapper(new SlotItemHandler(itemHandler, index, x, y));
    }

    protected SlotComponent(Container container, int index) {
        this();
        this.index = index;
        this.slot = new MutableSlotWrapper(new Slot(container, index, x, y));
    }

    protected SlotComponent(Slot slot) {
        this();
        this.index = slot.getSlotIndex();
        this.slot = new MutableSlotWrapper(slot);
    }

    public SlotComponent setSlot(IItemHandlerModifiable handler, int index) {
        int freeIndex = this.slot.index;
        setSlot(new SlotItemHandler(handler, index, x, y), freeIndex);
        return this;
    }

    public SlotComponent setSlot(Container handler, int index) {
        int freeIndex = this.slot.index;
        setSlot(new Slot(handler, index, x, y), freeIndex);
        return this;
    }

    public SlotComponent setSlot(IItemHandlerModifiable handler) {
        int freeIndex = this.slot.index;
        setSlot(new SlotItemHandler(handler, index, x, y), freeIndex);
        return this;
    }

    public SlotComponent setSlot(Container handler) {
        int freeIndex = this.slot.index;
        setSlot(new Slot(handler, index, x, y), freeIndex);
        return this;
    }

    private void setSlot(Slot slot, int index) {
        this.slot.setInner(slot);
        this.slot.gtceu$setSlotIndex(index);
    }

    public boolean slotClick(int button, ClickType clickTypeIn, Player player) {
        return false;
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        if (backgroundTexture != null) {
            backgroundTexture.draw(graphics, mouseX, mouseY, x(), y(), width(), height());
        }

        int[] scissor = new int[4];
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);

        ((UISlotExtension) this.slot).gtceu$setScissorArea(PositionedRectangle.of(
                scissor[0], scissor[1], scissor[2], scissor[3]));
    }

    @Override
    public void tick() {
        super.tick();
        if (backgroundTexture != null) {
            backgroundTexture.updateTick();
        }
        if (overlayTexture != null) {
            overlayTexture.updateTick();
        }

        this.slot.canInsertOverride = this.canInsert;
        this.slot.canExtractOverride = this.canExtract;
        ((UISlotExtension) this.slot).gtceu$setDisabledOverride(!this.drawContents);
    }

    @Override
    public boolean shouldDrawTooltip(double mouseX, double mouseY) {
        return drawTooltip && this.slot.hasItem() && super.shouldDrawTooltip(mouseX, mouseY);
    }

    @Override
    public int determineHorizontalContentSize(Sizing sizing) {
        return 18;
    }

    @Override
    public int determineVerticalContentSize(Sizing sizing) {
        return 18;
    }

    @Override
    public void mount(ParentUIComponent parent, int x, int y) {
        super.mount(parent, x, y);
        finalizeSlot();

        do {
            // go up the component tree, subscribe to disable, mount & dismount of parents at all levels except root
            parent.enabledEvent().subscribe((component, newEnabled) -> {
                if (!this.enabled()) {
                    return;
                }
                // enable this slot if a parent is enabled and vice versa.
                if (newEnabled) {
                    finalizeSlot();
                } else {
                    removeSlot();
                }
            });
            parent.mount().subscribe((component, x1, y1) -> {
                finalizeSlot();
            });
            parent.dismount().subscribe((component, reason) -> {
                if (reason == DismountReason.REMOVED) {
                    removeSlot();
                }
            });
            parent = parent.parent();
        } while (parent != null && parent != root());
    }

    @Override
    public void dismount(DismountReason reason) {
        if (reason == DismountReason.REMOVED && containerAccess().screen() != null) {
            removeSlot();
        }
        super.dismount(reason);
    }

    @Override
    public void dispose() {
        if (containerAccess().screen() != null) {
            removeSlot();
        }
        super.dispose();
    }

    private void removeSlot() {
        if (!slotFinalized || containerAccess() == null || containerAccess().screen() == null) {
            return;
        }

        var menu = containerAccess().screen().getMenu();
        UIContainerMenu.EmptySlotPlaceholder placeholder = new UIContainerMenu.EmptySlotPlaceholder();
        menu.slots.set(this.slot.index, placeholder);
        placeholder.index = this.slot.index;
        ((AbstractContainerMenuAccessor) menu).gtceu$getLastSlots().set(this.slot.index, ItemStack.EMPTY);
        ((AbstractContainerMenuAccessor) menu).gtceu$getRemoteSlots().set(this.slot.index, ItemStack.EMPTY);

        slotFinalized = false;
    }

    public void finalizeSlot() {
        if (slotFinalized || containerAccess() == null || containerAccess().screen() == null) {
            return;
        }

        var menu = containerAccess().screen().getMenu();

        if (!menu.slots.contains(this.slot)) {
            Slot innerSlot = this.slot.getInner();
            int foundIndex = -1;

            for (Slot menuSlot : menu.slots) {
                if (menuSlot.getContainerSlot() != innerSlot.getContainerSlot()) {
                    continue;
                }
                if (menuSlot instanceof SlotItemHandler menuHandler &&
                        innerSlot instanceof SlotItemHandler innerHandler) {
                    if (menuHandler.getItemHandler() == innerHandler.getItemHandler()) {
                        foundIndex = menuSlot.index;
                        break;
                    }
                } else {
                    if (menuSlot.container == innerSlot.container) {
                        foundIndex = menuSlot.index;
                        break;
                    }
                }
            }
            if (foundIndex != -1) {
                menu.slots.set(foundIndex, this.slot);
                ((SlotAccessor) this.slot).gtceu$setSlotIndex(foundIndex);
                ((AbstractContainerMenuAccessor) menu).gtceu$getLastSlots().set(foundIndex, this.slot.getItem());
                ((AbstractContainerMenuAccessor) menu).gtceu$getRemoteSlots().set(foundIndex, this.slot.getItem());
            }
        }
        slotFinalized = true;
    }

    @Override
    public BaseUIComponent x(int x) {
        if (containerAccess() != null && containerAccess().adapter() != null) {
            ((SlotAccessor) slot).gtceu$setX(x + 1 - containerAccess().adapter().leftPos());
        }
        return super.x(x);
    }

    @Override
    public BaseUIComponent y(int y) {
        if (containerAccess() != null && containerAccess().adapter() != null) {
            ((SlotAccessor) slot).gtceu$setY(y + 1 - containerAccess().adapter().topPos());
        }
        return super.y(y);
    }

    public ItemStack getRealStack(ItemStack itemStack) {
        if (itemHook != null) return itemHook.apply(itemStack);
        return itemStack;
    }

    @Override
    public @UnknownNullability("Nullability depends on the type of ingredient") EntryList<ItemStack> getIngredients() {
        if (slot.getInner() instanceof SlotItemHandler slotHandler) {
            if (slotHandler.getItemHandler() instanceof CycleItemStackHandler stackHandler) {
                return stackHandler.getStackList(slot.getContainerSlot());
            } else if (slotHandler.getItemHandler() instanceof CycleItemEntryHandler entryHandler) {
                return entryHandler.getEntry(slot.getContainerSlot());
            }
        }

        return ItemStackList.of(getRealStack(this.slot.getItem()));
    }

    @Override
    public UnaryOperator<ItemStack> renderMappingFunction() {
        return this::getRealStack;
    }

    @Override
    public @NotNull Class<ItemStack> ingredientClass() {
        return ItemStack.class;
    }

    @Override
    public float chance() {
        return recipeViewerChance;
    }

    @Override
    public void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        super.parseProperties(model, element, children);
        UIParsing.apply(children, "can-insert", UIParsing::parseBool, this::canInsert);
        UIParsing.apply(children, "can-extract", UIParsing::parseBool, this::canExtract);
        UIParsing.apply(children, "ingredient-io", UIParsing.parseEnum(IO.class), this::ingredientIO);

        if (children.containsKey("background-texture")) {
            this.backgroundTexture = model.parseTexture(UITexture.class, children.get("background-texture"));
        }
        if (children.containsKey("overlay-texture")) {
            this.overlayTexture = model.parseTexture(UITexture.class, children.get("overlay-texture"));
        }
        UIParsing.apply(children, "chance", UIParsing::parseFloat, this::recipeViewerChance);
        UIParsing.apply(children, "draw-contents", UIParsing::parseBool, this::drawContents);
        UIParsing.apply(children, "draw-tooltip", UIParsing::parseBool, this::drawTooltip);
    }

    public static SlotComponent parse(Element element) {
        UIParsing.expectAttributes(element, "index");
        int index = UIParsing.parseUnsignedInt(element.getAttributeNode("index"));
        return new SlotComponent(index);
    }

    @Accessors(fluent = false, chain = false)
    public static class MutableSlotWrapper extends Slot {

        @Getter
        private Slot inner;
        @Nullable
        @Getter
        @Setter
        protected Boolean canInsertOverride;
        @Nullable
        @Getter
        @Setter
        protected Boolean canExtractOverride;

        public MutableSlotWrapper(Slot inner) {
            super(inner.container, inner.getSlotIndex(), inner.x, inner.y);
            this.inner = inner;
        }

        public void setInner(Slot slot) {
            if (slot == this) {
                return;
            }
            this.inner = slot;
            ((SlotAccessor) inner).gtceu$setX(x);
            ((SlotAccessor) inner).gtceu$setY(y);
        }

        @Override
        public int getSlotIndex() {
            return inner.getSlotIndex();
        }

        @Override
        public int getContainerSlot() {
            return inner.getContainerSlot();
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return Objects.requireNonNullElseGet(canInsertOverride, () -> inner.isActive());
        }

        @Override
        @NotNull
        public ItemStack getItem() {
            return inner.getItem();
        }

        @Override
        public boolean hasItem() {
            return inner.hasItem();
        }

        @Override
        public void setByPlayer(@NotNull ItemStack stack) {
            inner.setByPlayer(stack);
        }

        @Override
        public void set(@NotNull ItemStack stack) {
            inner.set(stack);
        }

        @Override
        public void setChanged() {
            inner.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return inner.getMaxStackSize();
        }

        @Override
        public int getMaxStackSize(@NotNull ItemStack stack) {
            return inner.getMaxStackSize(stack);
        }

        @Nullable
        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return inner.getNoItemIcon();
        }

        @Override
        @NotNull
        public ItemStack remove(int amount) {
            return inner.remove(amount);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return Objects.requireNonNullElseGet(canExtractOverride, () -> inner.mayPickup(player));
        }

        @Override
        public void onQuickCraft(ItemStack stack, ItemStack newStack) {
            inner.onQuickCraft(stack, newStack);
        }

        @Override
        public boolean isActive() {
            return inner.isActive();
        }

        @SuppressWarnings("unused") // it actually overrides an accessor mixin's method.
        public void gtceu$setX(int x) {
            this.x = x;
            ((SlotAccessor) inner).gtceu$setX(x);
        }

        @SuppressWarnings("unused") // it actually overrides an accessor mixin's method.
        public void gtceu$setY(int y) {
            this.y = y;
            ((SlotAccessor) inner).gtceu$setY(y);
        }

        @SuppressWarnings("unused") // it actually overrides an accessor mixin's method.
        public void gtceu$setSlotIndex(int index) {
            this.index = index;
            ((SlotAccessor) inner).gtceu$setSlotIndex(index);
        }
    }
}
