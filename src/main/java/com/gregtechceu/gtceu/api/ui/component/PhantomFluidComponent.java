package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.ingredient.GhostIngredientSlot;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(fluent = true, chain = true)
public class PhantomFluidComponent extends TankComponent implements GhostIngredientSlot<FluidStack> {

    private final Supplier<FluidStack> phantomFluidGetter;
    private final Consumer<FluidStack> phantomFluidSetter;

    @Nullable
    @Getter
    protected FluidStack lastPhantomStack;

    public PhantomFluidComponent(@Nullable IFluidHandler fluidTank, int tank,
                                 Supplier<FluidStack> phantomFluidGetter, Consumer<FluidStack> phantomFluidSetter) {
        super(fluidTank, tank);
        this.canInsert = false;
        this.canExtract = false;
        this.phantomFluidGetter = phantomFluidGetter;
        this.phantomFluidSetter = phantomFluidSetter;
    }

    public PhantomFluidComponent canExtract(boolean v) {
        // you cant modify it
        return this;
    }

    public PhantomFluidComponent canInsert(boolean v) {
        // you can't modify it
        return this;
    }

    protected void lastPhantomStack(FluidStack fluid) {
        if (fluid != null) {
            this.lastPhantomStack = fluid.copy();
            this.lastPhantomStack.setAmount(1);
        } else {
            this.lastPhantomStack = null;
        }
    }

    public static FluidStack drainFrom(Object ingredient) {
        if (ingredient instanceof Ingredient ing) {
            var items = ing.getItems();
            if (items.length > 0) {
                ingredient = items[0];
            }
        }
        if (ingredient instanceof ItemStack itemStack) {
            return FluidUtil.getFluidHandler(itemStack)
                    .map(h -> h.drain(Integer.MAX_VALUE, FluidAction.SIMULATE))
                    .orElse(FluidStack.EMPTY);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public void setGhostIngredient(@NotNull FluidStack ingredient) {
        if (!ingredient.isEmpty()) {
            //sendMessage(2, ingredient::writeToPacket);
        }

        if (phantomFluidSetter != null) {
            phantomFluidSetter.accept(ingredient);
        }
    }

    @Override
    public Class<FluidStack> ghostIngredientClass() {
        return FluidStack.class;
    }

    @Override
    public void receiveMessage(int id, FriendlyByteBuf buf) {
        if (id == 1) {
            handlePhantomClick();
        } else if (id == 2) {
            if (phantomFluidSetter != null) {
                phantomFluidSetter.accept(FluidStack.readFromPacket(buf));
            }
        } else if (id == 4) {
            phantomFluidSetter.accept(FluidStack.EMPTY);
        } else if (id == 5) {
            phantomFluidSetter.accept(FluidStack.readFromPacket(buf));
        }
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        FluidStack stack = phantomFluidGetter.get();
        if (stack == null || stack.isEmpty()) {
            if (lastPhantomStack != null) {
                lastPhantomStack(null);
                //sendMessage(4, buf -> {});
            }
        } else if (lastPhantomStack == null || !stack.isFluidEqual(lastPhantomStack)) {
            lastPhantomStack(stack);
            //sendMessage(5, stack::writeToPacket);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            handlePhantomClick();
            return true;
        }
        return false;
    }

    private void handlePhantomClick() {
        ItemStack itemStack = getCarried();
        FluidStack fluid = FluidUtil.getFluidContained(itemStack)
                .map(f -> new FluidStack(f, FluidType.BUCKET_VOLUME))
                .orElse(FluidStack.EMPTY);
        if (phantomFluidSetter != null) phantomFluidSetter.accept(fluid);
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        if (this.lastFluidInTank != null) {
            super.draw(graphics, mouseX, mouseY, partialTicks, delta);
            return;
        }
        FluidStack stack = phantomFluidGetter.get();
        if (stack != null && !stack.isEmpty()) {
            RenderSystem.disableBlend();

            double progress = stack.getAmount() * 1.0 / Math.max(Math.max(stack.getAmount(), lastTankCapacity), 1);
            float drawnU = (float) fillDirection.getDrawnU(progress);
            float drawnV = (float) fillDirection.getDrawnV(progress);
            float drawnWidth = (float) fillDirection.getDrawnWidth(progress);
            float drawnHeight = (float) fillDirection.getDrawnHeight(progress);
            int width = width() - 2;
            int height = height() - 2;
            int x = x() + 1;
            int y = y() + 1;
            graphics.drawFluid(lastFluidInTank(), stack.getAmount(),
                    (int) (x + drawnU * width), (int) (y + drawnV * height),
                    ((int) (width * drawnWidth)), ((int) (height * drawnHeight)));
            if (showAmount) {
                graphics.pose().pushPose();
                graphics.pose().scale(0.5F, 0.5F, 1);
                String s = FormattingUtil.formatBuckets(stack.getAmount());
                Font fontRenderer = Minecraft.getInstance().font;
                graphics.drawString(fontRenderer, s,
                        (int) ((x() + (width() / 3f)) * 2 - fontRenderer.width(s) + 21),
                        (int) ((y() + (height() / 3f) + 6) * 2), 0xFFFFFF, true);
                graphics.pose().popPose();
            }

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }
}
