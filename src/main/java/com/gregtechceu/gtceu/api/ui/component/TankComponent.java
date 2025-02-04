package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.base.BaseUIComponent;
import com.gregtechceu.gtceu.api.ui.core.Color;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.ingredient.ClickableIngredientSlot;
import com.gregtechceu.gtceu.api.ui.parsing.UIModel;
import com.gregtechceu.gtceu.api.ui.parsing.UIParsing;
import com.gregtechceu.gtceu.api.ui.texture.ProgressTexture;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.util.Observable;
import com.gregtechceu.gtceu.integration.xei.entry.EntryList;
import com.gregtechceu.gtceu.integration.xei.entry.fluid.FluidStackList;
import com.gregtechceu.gtceu.integration.xei.handlers.fluid.CycleFluidEntryHandler;
import com.gregtechceu.gtceu.integration.xei.handlers.fluid.CycleFluidStackHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.EmptyFluidHandler;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;

@Accessors(fluent = true, chain = true)
public class TankComponent extends BaseUIComponent implements ClickableIngredientSlot<FluidStack> {

    protected static final int SET_FLUID = 1;

    @Getter
    protected IFluidHandler handler;
    @Getter
    protected int tank;
    protected Observable<FluidStack> lastFluidInTank = Observable.of(FluidStack.EMPTY);
    protected int lastTankCapacity;
    @Setter
    protected boolean showAmount = true;
    @Setter
    protected boolean canInsert = true;
    @Setter
    protected boolean canExtract = true;

    @Setter
    protected Runnable changeListener;
    @Getter
    @Setter
    protected IO ingredientIO;
    @Getter
    @Setter
    protected UITexture backgroundTexture = GuiTextures.FLUID_SLOT;
    @Getter
    @Setter
    protected UITexture overlayTexture;
    @Getter
    @Setter
    protected ProgressTexture.FillDirection fillDirection = ProgressTexture.FillDirection.ALWAYS_FULL;
    @Setter
    @Getter
    protected float recipeViewerChance = 1f;

    @Setter
    protected boolean drawContents = true;
    @Setter
    protected boolean drawTooltip = true;

    protected TankComponent(IFluidHandler fluidHandler, int tank) {
        this.handler = fluidHandler;
        this.tank = tank;
        this.sizing(Sizing.fixed(18));
        Observable.observeAll(this::updateListener, this.lastFluidInTank);
    }

    public TankComponent setFluidTank(IFluidHandler handler) {
        this.handler = handler;
        this.tank = 0;
        lastFluidInTank(this.handler.getFluidInTank(this.tank));
        return this;
    }

    public TankComponent setFluidTank(IFluidHandler handler, int tank) {
        this.handler = handler;
        this.tank = tank;
        lastFluidInTank(this.handler.getFluidInTank(this.tank));
        return this;
    }

    public void receiveMessage(int id, FriendlyByteBuf buf) {
        if (id == SET_FLUID) {
            lastFluidInTank(FluidStack.readFromPacket(buf));
        }
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        if (backgroundTexture != null) {
            backgroundTexture.draw(graphics, mouseX, mouseY, x(), y(), width(), height());
        }

        if (!drawContents) {
            return;
        }

        if (handler != null) {
            FluidStack stack = handler.getFluidInTank(tank);
            int capacity = handler.getTankCapacity(tank);
            if (capacity != lastTankCapacity) {
                lastTankCapacity = capacity;
            }
            if (lastFluidInTank().isEmpty()) {
                lastFluidInTank(stack);
            }
            if (!stack.isFluidEqual(lastFluidInTank())) {
                lastFluidInTank(stack);
            } else if (stack.getAmount() != lastFluidInTank().getAmount()) {
                lastFluidInTank().setAmount(stack.getAmount());
            }
        }

        if (!lastFluidInTank().isEmpty()) {
            RenderSystem.disableBlend();
            if (!lastFluidInTank().isEmpty()) {
                double progress = lastFluidInTank().getAmount() * 1.0 /
                        Math.max(Math.max(lastFluidInTank().getAmount(), lastTankCapacity), 1);
                float drawnU = (float) fillDirection.getDrawnU(progress);
                float drawnV = (float) fillDirection.getDrawnV(progress);
                float drawnWidth = (float) fillDirection.getDrawnWidth(progress);
                float drawnHeight = (float) fillDirection.getDrawnHeight(progress);

                int width = width() - 2;
                int height = height() - 2;
                int x = x() + 1;
                int y = y() + 1;
                graphics.drawFluid(lastFluidInTank(), lastTankCapacity,
                        (int) (x + drawnU * width), (int) (y + drawnV * height),
                        ((int) (width * drawnWidth)), ((int) (height * drawnHeight)));

                if (showAmount && !lastFluidInTank().isEmpty()) {
                    graphics.pose().pushPose();
                    graphics.pose().scale(0.5f, 0.5f, 1.0f);
                    String s = FormattingUtil.formatBuckets(lastFluidInTank().getAmount());
                    Font f = Minecraft.getInstance().font;
                    graphics.drawString(f, s,
                            (int) ((x + width / 3.0f)) * 2 - f.width(s) + 21,
                            (int) ((y + (height / 3.0f) + 6) * 2), Color.WHITE.argb(), true);
                    graphics.pose().popPose();
                }
            }
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        }

        if (overlayTexture != null) {
            overlayTexture.draw(graphics, mouseX, mouseY, x(), y(), width(), height());
        }

        if (hovered) {
            RenderSystem.colorMask(true, true, true, false);
            graphics.drawSolidRect(x + 1, y + 1, width - 2, height - 2, Color.HOVER_GRAY.argb());
            RenderSystem.colorMask(true, true, true, true);
        }
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
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        Player player = player();
        if (player == null) return false;
        boolean isShift = player.isShiftKeyDown();
        ItemStack currentStack = getCarried();
        var handler = FluidUtil.getFluidHandler(currentStack).resolve().orElse(null);
        if (handler == null) return false;
        int maxAttempts = isShift ? currentStack.getCount() : 1;
        FluidStack initialFluid = this.handler.getFluidInTank(tank).copy();
        if (canExtract && initialFluid.getAmount() > 0) {
            boolean performedFill = false;
            ItemStack filledResult = ItemStack.EMPTY;
            for (int i = 0; i < maxAttempts; i++) {
                FluidActionResult res = FluidUtil.tryFillContainer(currentStack, this.handler, Integer.MAX_VALUE, null,
                        false);
                if (!res.isSuccess()) break;
                ItemStack remaining = FluidUtil
                        .tryFillContainer(currentStack, this.handler, Integer.MAX_VALUE, null, true).getResult();
                performedFill = true;

                currentStack.shrink(1);

                if (filledResult.isEmpty()) {
                    filledResult = remaining.copy();
                } else if (ItemStack.isSameItemSameTags(filledResult, remaining)) {
                    if (filledResult.getCount() < filledResult.getMaxStackSize())
                        filledResult.grow(1);
                    else
                        sendMenuUpdate(new UIContainerMenu.ServerboundPlaceItemBackUpdate(remaining));
                } else {
                    sendMenuUpdate(new UIContainerMenu.ServerboundPlaceItemBackUpdate(filledResult));
                    filledResult = remaining.copy();
                }
            }
            if (performedFill) {
                if (currentStack.isEmpty()) {
                    setCarried(filledResult);
                } else {
                    setCarried(currentStack);
                    sendMenuUpdate(new UIContainerMenu.ServerboundPlaceItemBackUpdate(filledResult));
                }

                // TODO do some checking on server to not just accept any stack
                sendMenuUpdate(new UIContainerMenu.ServerboundSetCarriedUpdate(getCarried()));
                return true;
            }
        }

        if (canInsert) {
            boolean performedEmptying = false;
            ItemStack drainedResult = ItemStack.EMPTY;
            for (int i = 0; i < maxAttempts; i++) {
                FluidActionResult result = FluidUtil.tryEmptyContainer(currentStack, this.handler, Integer.MAX_VALUE,
                        player,
                        false);
                if (!result.isSuccess()) break;
                ItemStack remainingStack = FluidUtil
                        .tryEmptyContainer(currentStack, this.handler, Integer.MAX_VALUE, player, true).getResult();
                performedEmptying = true;

                currentStack.shrink(1);

                if (drainedResult.isEmpty()) {
                    drainedResult = remainingStack.copy();
                } else if (ItemStack.isSameItemSameTags(drainedResult, remainingStack)) {
                    if (drainedResult.getCount() < drainedResult.getMaxStackSize())
                        drainedResult.grow(1);
                    else
                        sendMenuUpdate(new UIContainerMenu.ServerboundPlaceItemBackUpdate(remainingStack));
                } else {
                    sendMenuUpdate(new UIContainerMenu.ServerboundPlaceItemBackUpdate(drainedResult));
                    drainedResult = remainingStack.copy();
                }
            }
            if (performedEmptying) {
                if (currentStack.isEmpty()) {
                    setCarried(drainedResult);
                } else {
                    setCarried(currentStack);
                    sendMenuUpdate(new UIContainerMenu.ServerboundPlaceItemBackUpdate(drainedResult));
                }

                // TODO do some checking on server to not just accept any stack
                sendMenuUpdate(new UIContainerMenu.ServerboundSetCarriedUpdate(getCarried()));
                return true;
            }
        }
        return super.onMouseDown(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldDrawTooltip(double mouseX, double mouseY) {
        return drawTooltip && !this.lastFluidInTank().isEmpty() && super.shouldDrawTooltip(mouseX, mouseY);
    }

    @Override
    public int determineHorizontalContentSize(Sizing sizing) {
        return 18;
    }

    @Override
    public int determineVerticalContentSize(Sizing sizing) {
        return 18;
    }

    @NotNull
    public FluidStack lastFluidInTank() {
        return lastFluidInTank.get();
    }

    public TankComponent lastFluidInTank(FluidStack fluidStack) {
        this.lastFluidInTank.set(fluidStack);
        return this;
    }

    protected void updateListener() {
        if (changeListener != null) {
            changeListener.run();
        }
        if (!this.lastFluidInTank().isEmpty()) {
            this.tooltip(FluidComponent.tooltipFromFluid(this.lastFluidInTank(), Minecraft.getInstance().player, null));
        } else {
            this.tooltip((List<ClientTooltipComponent>) null);
        }
    }

    @Override
    public @UnknownNullability("Nullability depends on the type of ingredient") EntryList<FluidStack> getIngredients() {
        if (handler instanceof CycleFluidStackHandler stackHandler) {
            return stackHandler.getStackList(this.tank);
        } else if (handler instanceof CycleFluidEntryHandler entryHandler) {
            return entryHandler.getEntry(this.tank);
        }

        return FluidStackList.of(this.lastFluidInTank());
    }

    @Override
    public @NotNull Class<FluidStack> ingredientClass() {
        return FluidStack.class;
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
        UIParsing.apply(children, "fill-direction", UIParsing.parseEnum(ProgressTexture.FillDirection.class),
                this::fillDirection);
        UIParsing.apply(children, "chance", UIParsing::parseFloat, this::recipeViewerChance);
        UIParsing.apply(children, "draw-contents", UIParsing::parseBool, this::drawContents);
        UIParsing.apply(children, "draw-tooltip", UIParsing::parseBool, this::drawTooltip);
    }

    public static TankComponent parse(Element element) {
        UIParsing.expectAttributes(element, "tank");
        int tank = UIParsing.parseUnsignedInt(element.getAttributeNode("tank"));

        return new TankComponent(EmptyFluidHandler.INSTANCE, tank);
    }
}
