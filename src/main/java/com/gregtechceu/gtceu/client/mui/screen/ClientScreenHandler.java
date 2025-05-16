package com.gregtechceu.gtceu.client.mui.screen;

import com.gregtechceu.gtceu.api.mui.base.IMuiScreen;
import com.gregtechceu.gtceu.api.mui.base.MCHelper;
import com.gregtechceu.gtceu.api.mui.base.widget.IGuiElement;
import com.gregtechceu.gtceu.api.mui.base.widget.IVanillaSlot;
import com.gregtechceu.gtceu.api.mui.drawable.GuiDraw;
import com.gregtechceu.gtceu.api.mui.drawable.Stencil;
import com.gregtechceu.gtceu.api.mui.overlay.OverlayStack;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Rectangle;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;
import com.gregtechceu.gtceu.client.mui.screen.viewport.LocatedWidget;
import com.gregtechceu.gtceu.client.mui.screen.viewport.ModularGuiContext;
import com.gregtechceu.gtceu.api.mui.utils.Animator;
import com.gregtechceu.gtceu.api.mui.utils.Color;
import com.gregtechceu.gtceu.api.mui.utils.FpsCounter;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Area;
import com.gregtechceu.gtceu.common.mui.widgets.RichTextWidget;
import com.gregtechceu.gtceu.common.mui.widgets.slot.ItemSlot;
import com.gregtechceu.gtceu.common.mui.widgets.slot.ModularSlot;
import com.gregtechceu.gtceu.common.mui.widgets.slot.SlotGroup;
import com.gregtechceu.gtceu.core.mixins.AbstractContainerScreenAccessor;
import com.gregtechceu.gtceu.core.mixins.ScreenAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;

@ApiStatus.Internal
@OnlyIn(Dist.CLIENT)
public class ClientScreenHandler {

    private static final GuiContext defaultContext = new GuiContext();

    private static ModularScreen currentScreen = null;
    private static final FpsCounter fpsCounter = new FpsCounter();
    private static long ticks = 0L;

    @SubscribeEvent
    public static void onGuiOpen(ScreenEvent.Opening event) {
        defaultContext.reset();
        if (event.getNewScreen() instanceof IMuiScreen muiScreen) {
            Objects.requireNonNull(muiScreen.getScreen(), "ModularScreen must not be null!");
            if (currentScreen != muiScreen.getScreen()) {
                if (hasScreen()) {
                    currentScreen.onCloseParent();
                    currentScreen = null;
                }
                currentScreen = muiScreen.getScreen();
                fpsCounter.reset();
            }
        } else if (hasScreen() && getMCScreen() != null && event.getNewScreen() != getMCScreen()) {
            currentScreen.onCloseParent();
            currentScreen = null;
        }
    }

    @SubscribeEvent
    public static void onGuiInit(ScreenEvent.Init.Post event) {
        defaultContext.updateScreenArea(event.getScreen().width, event.getScreen().height);
        if (checkGui(event.getScreen())) {
            currentScreen.onResize(event.getScreen().width, event.getScreen().height);
        }
        OverlayStack.foreach(ms -> ms.onResize(event.getScreen().width, event.getScreen().height), false);
    }

    // before JEI
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onGuiInputHigh(ScreenEvent.KeyPressed.Pre event) {
        defaultContext.updateLatestKey(event.getKeyCode(), event.getScanCode(), event.getModifiers());
        inputPressedEvent(event, InputPhase.EARLY);
    }

    // after JEI
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onGuiInputLow(ScreenEvent.KeyPressed.Pre event) {
        inputPressedEvent(event, InputPhase.LATE);
    }

    private static void inputPressedEvent(ScreenEvent.KeyPressed.Pre event, InputPhase phase) {
        if (checkGui(event.getScreen())) {
            currentScreen.getContext().updateLatestKey(event.getKeyCode(), event.getScanCode(), event.getModifiers());
        }
        if (handleKeyboardInput(currentScreen, event.getScreen(), true, phase,
                event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    // before JEI
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void inputReleasedHigh(ScreenEvent.KeyReleased.Pre event) {
        defaultContext.updateLatestKey(event.getKeyCode(), event.getScanCode(), event.getModifiers());
        inputReleasedEvent(event, InputPhase.EARLY);
    }

    // after JEI
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void inputReleasedLow(ScreenEvent.KeyReleased.Pre event) {
        inputReleasedEvent(event, InputPhase.LATE);
    }

    private static void inputReleasedEvent(ScreenEvent.KeyReleased.Pre event, InputPhase phase) {
        if (checkGui(event.getScreen())) {
            currentScreen.getContext().updateLatestKey(event.getKeyCode(), event.getScanCode(), event.getModifiers());
        }
        if (handleKeyboardInput(currentScreen, event.getScreen(), true, phase,
                event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    public static boolean shouldUnfocusRecipeViewer = false;

    // before JEI
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void handleMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        int button = event.getButton();
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        defaultContext.updateMouseButton(button);
        if (checkGui(event.getScreen())) currentScreen.getContext().updateMouseButton(button);

        if (button == -1) {
            shouldUnfocusRecipeViewer = false;
            return;
        }
        if (currentScreen != null && currentScreen.handleDraggableInput(mouseX, mouseY, button, true) ||
                doAction(currentScreen, ms -> ms.onMouseReleased(mouseX, mouseY, button))) {
            shouldUnfocusRecipeViewer = true;
            event.setCanceled(true);
        } else {
            shouldUnfocusRecipeViewer = false;
        }
    }

    // before JEI
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void handleMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        int button = event.getButton();
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        defaultContext.updateMouseButton(button);
        if (checkGui(event.getScreen())) currentScreen.getContext().updateMouseButton(button);

        if (currentScreen != null && currentScreen.handleDraggableInput(mouseX, mouseY, button, false) ||
                doAction(currentScreen, ms -> ms.mouseReleased(mouseX, mouseY, button))) {
            shouldUnfocusRecipeViewer = true;
            event.setCanceled(true);
        } else {
            shouldUnfocusRecipeViewer = false;
        }
    }

    // before JEI
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void handleMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        double w = event.getScrollDelta();
        if (w == 0) return;
        defaultContext.updateMouseWheel(w);
        if (checkGui(event.getScreen())) currentScreen.getContext().updateMouseWheel(w);
        checkGui(event.getScreen());
        if (doAction(currentScreen, ms -> ms.mouseScrolled(event.getMouseX(), event.getMouseY(), w))) {
            event.setCanceled(true);
        }
    }

    // before JEI
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void handleMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        checkGui(event.getScreen());
        if (event.getMouseButton() == -1) {
            return;
        }
        if (doAction(currentScreen, ms -> ms.mouseDragged(event.getMouseX(), event.getMouseY(),
                event.getMouseButton(), event.getDragX(), event.getDragY()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onGuiDraw(ScreenEvent.Render.Pre event) {
        int mx = event.getMouseX(), my = event.getMouseY();
        float pt = event.getPartialTick();
        GuiGraphics gc = event.getGuiGraphics();
        defaultContext.setGraphics(gc);
        defaultContext.updateState(mx, my, pt);
        defaultContext.reset();
        if (checkGui(event.getScreen())) {
            currentScreen.getContext().updateState(mx, my, pt);
            currentScreen.getContext().setGraphics(gc);
            drawScreen(gc, currentScreen, currentScreen.getScreenWrapper().getWrappedScreen(), mx, my, pt);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGuiDraw(ScreenEvent.Render.Post event) {
        OverlayStack.draw(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            OverlayStack.onTick();
            defaultContext.tick();
            if (checkGui()) {
                currentScreen.onUpdate();
            }
            ticks++;
        }
    }

    @SubscribeEvent
    public static void preDraw(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            GL11.glEnable(GL11.GL_STENCIL_TEST);
        }
        Stencil.reset();
    }

    public static long getTicks() {
        return ticks;
    }

    public static void onFrameUpdate() {
        OverlayStack.foreach(ModularScreen::onFrameUpdate, true);
        if (currentScreen != null) currentScreen.onFrameUpdate();
        Animator.advance();
    }

    private static boolean doAction(@Nullable ModularScreen muiScreen, Predicate<ModularScreen> action) {
        return OverlayStack.interact(action, true) || (muiScreen != null && action.test(muiScreen));
    }

    /**
     * This replicates vanilla behavior while also injecting custom behavior for consistency
     */
    private static boolean handleKeyboardInput(@Nullable ModularScreen muiScreen, Screen mcScreen,
                                               boolean isPress, InputPhase inputPhase,
                                               int keyCode, int scanCode, int modifiers) {
        if (isPress) {
            // pressing a key
            return inputPhase.isEarly() ? doAction(muiScreen, ms -> ms.keyPressed(keyCode, scanCode, modifiers)) : keyTyped(mcScreen, keyCode, scanCode, modifiers);
        } else {
            // releasing a key
            if (inputPhase.isEarly() && doAction(muiScreen, ms -> ms.keyReleased(keyCode, scanCode, modifiers))) {
                return true;
            }
            if (inputPhase.isLate() && keyCode >= ' ') {
                return keyTyped(mcScreen, keyCode, scanCode, modifiers);
            }
        }
        return false;
    }

    private static boolean keyTyped(Screen screen, int keyCode, int scanCode, int modifiers) {
        if (currentScreen == null) return false;
        // debug mode C + CTRL + SHIFT + ALT
        if (keyCode == 'c' && Screen.hasControlDown() && Screen.hasShiftDown() && Screen.hasAltDown()) {
            ModularUIConfig.guiDebugMode = !ModularUIConfig.guiDebugMode;
            return true;
        }
        boolean isInventoryKey = Minecraft.getInstance().options
                .keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
        if (keyCode == 1 || isInventoryKey) {
            if (currentScreen.getContext().hasDraggable()) {
                currentScreen.getContext().dropDraggable();
            } else {
                currentScreen.getPanelManager().closeTopPanel(true);
            }
            return true;
        }
        return false;
    }

    public static void dragSlot(double mouseX, double mouseY, int button, double dragX, double dragY) {
        getMCScreen().mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public static void clickSlot(ModularScreen ms, Slot slot) {
        Screen screen = ms.getScreenWrapper().getWrappedScreen();
        if (screen instanceof ScreenAccessor acc && screen instanceof IClickableGuiContainer clickableGuiContainer && checkGui(screen)) {
            ModularGuiContext ctx = ms.getContext();
            var buttonList = screen.children();
            try {
                // remove buttons to make sure they are not clicked
                acc.setChildren(Collections.emptyList());
                // set clicked slot to make sure the container clicks the desired slot
                clickableGuiContainer.gtceu$setClickedSlot(slot);
                screen.mouseClicked(ctx.getMouseX(), ctx.getMouseY(), ctx.getMouseButton());
            } finally {
                // undo modifications
                clickableGuiContainer.gtceu$setClickedSlot(null);
                acc.setChildren(buttonList);
            }
        }
    }

    public static void releaseSlot() {
        if (hasScreen() && getMCScreen() != null) {
            ModularGuiContext ctx = currentScreen.getContext();
            getMCScreen().mouseReleased(ctx.getMouseX(), ctx.getMouseY(), ctx.getMouseButton());
        }
    }

    public static boolean shouldDrawWorldBackground() {
        return /*ModularUI.isBlurLoaded() || */Minecraft.getInstance().level == null;
    }

    public static void drawDarkBackground(Screen screen, GuiGraphics guiGraphics) {
        if (hasScreen()) {
            float alpha = currentScreen.getMainPanel().getAlpha();
            // vanilla color values as hex
            int color = 0x101010;
            int startAlpha = 0xc0;
            int endAlpha = 0xd0;
            GuiDraw.drawVerticalGradientRect(0, 0, screen.width, screen.height, Color.withAlpha(color, (int) (startAlpha * alpha)), Color.withAlpha(color, (int) (endAlpha * alpha)));
        }
    }

    public static void drawScreen(GuiGraphics graphics, ModularScreen muiScreen, Screen mcScreen, int mouseX, int mouseY, float partialTicks) {
        if (mcScreen instanceof AbstractContainerScreen<?> container) {
            drawContainer(graphics, muiScreen, container, mouseX, mouseY, partialTicks);
        } else {
            drawScreenInternal(graphics, muiScreen, mcScreen, mouseX, mouseY, partialTicks);
        }
    }

    public static void drawScreenInternal(GuiGraphics graphics, ModularScreen muiScreen, Screen mcScreen, int mouseX, int mouseY, float partialTicks) {
        Stencil.reset();
        Stencil.apply(muiScreen.getScreenArea(), null);
        muiScreen.drawScreen(graphics, mouseX, mouseY, partialTicks);
        RenderSystem.disableDepthTest();
        drawVanillaElements(graphics, mcScreen, mouseX, mouseY, partialTicks);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Lighting.setupForFlatItems();
        muiScreen.drawForeground(graphics, partialTicks);
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        Stencil.remove();
    }

    public static void drawContainer(GuiGraphics graphics, ModularScreen muiScreen, AbstractContainerScreen<?> mcScreen, int mouseX, int mouseY, float partialTicks) {
        AbstractContainerScreenAccessor acc = (AbstractContainerScreenAccessor) mcScreen;

        Stencil.reset();
        Stencil.apply(muiScreen.getScreenArea(), null);
        mcScreen.renderBackground(graphics);
        int x = mcScreen.getGuiLeft();
        int y = mcScreen.getGuiTop();

        acc.invokeRenderBg(graphics, partialTicks, mouseX, mouseY);
        muiScreen.drawScreen(graphics, mouseX, mouseY, partialTicks);

        RenderSystem.disableDepthTest();
        // mainly for invtweaks compat
        drawVanillaElements(graphics, mcScreen, mouseX, mouseY, partialTicks);
        graphics.pose().pushPose();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        acc.setHoveredSlot(null);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        Lighting.setupForFlatItems();
        acc.invokeRenderLabels(graphics, mouseX, mouseY);
        muiScreen.drawForeground(graphics, partialTicks);
        Lighting.setupFor3DItems();

        acc.setHoveredSlot(null);
        IGuiElement hovered = muiScreen.getContext().getHovered();
        if (hovered instanceof IVanillaSlot vanillaSlot && vanillaSlot.handleAsVanillaSlot()) {
            acc.setHoveredSlot(vanillaSlot.getVanillaSlot());
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        MinecraftForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Foreground(mcScreen, graphics, mouseX, mouseY));
        graphics.pose().popPose();

        InventoryMenu inventory = Minecraft.getInstance().player.inventoryMenu;
        ItemStack itemstack = acc.getDraggingItem().isEmpty() ? inventory.getCarried() : acc.getDraggingItem();
        graphics.pose().translate((float) x, (float) y, 0.0F);
        if (!itemstack.isEmpty()) {
            int k2 = acc.getDraggingItem().isEmpty() ? 8 : 16;
            String s = null;

            if (!acc.getDraggingItem().isEmpty() && acc.getIsSplittingStack()) {
                itemstack = itemstack.copy();
                itemstack.setCount(Mth.ceil((float) itemstack.getCount() / 2.0F));
            } else if (acc.getIsQuickCrafting() && acc.getQuickCraftSlots().size() > 1) {
                itemstack = itemstack.copy();
                itemstack.setCount(acc.getQuickCraftingRemainder());

                if (itemstack.isEmpty()) {
                    s = ChatFormatting.YELLOW + "0";
                }
            }

            drawItemStack(mcScreen, graphics, itemstack, mouseX - x - 8, mouseY - y - k2, s);
        }

        if (!acc.getSnapbackItem().isEmpty()) {
            float f = (float) (Util.getMillis() - acc.getSnapbackTime()) / 100.0F;

            if (f >= 1.0F) {
                f = 1.0F;
                acc.setSnapbackItem(ItemStack.EMPTY);
            }

            int l2 = acc.getSnapbackEnd().x - acc.getSnapbackStartX();
            int i3 = acc.getSnapbackEnd().y - acc.getSnapbackStartY();
            int l1 = acc.getSnapbackStartX() + (int) ((float) l2 * f);
            int i2 = acc.getSnapbackStartY() + (int) ((float) i3 * f);
            drawItemStack(mcScreen, graphics, acc.getSnapbackItem(), l1, i2, null);
        }
        graphics.pose().popPose();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        Stencil.remove();
    }

    private static void drawItemStack(AbstractContainerScreen<?> mcScreen, GuiGraphics graphics, ItemStack stack, int x, int y, String altText) {
        graphics.pose().translate(0.0F, 0.0F, 32.0F);
        // ((AbstractContainerScreenAccessor) mcScreen).setZLevel(200f);
        // ((MouseHandlerAccessor) mcScreen).getItemRender().zLevel = 200.0F;
        Font font = null;// stack.getItem().getFont(stack);
        if (font == null) font = ((ScreenAccessor) mcScreen).getFont();
        RenderSystem.enableDepthTest();
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(font, stack, x,
                y - (((AbstractContainerScreenAccessor) mcScreen).getDraggingItem().isEmpty() ? 0 : 8), altText);
        RenderSystem.disableDepthTest();
        // ((GuiAccessor) mcScreen).setZLevel(0f);
        // ((MouseHandlerAccessor) mcScreen).getItemRender().zLevel = 0.0F;
    }

    private static void drawVanillaElements(GuiGraphics graphics, Screen mcScreen, int mouseX, int mouseY, float partialTicks) {
        for (Renderable renderable : mcScreen.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    public static void drawDebugScreen(GuiGraphics graphics, @Nullable ModularScreen muiScreen, @Nullable ModularScreen fallback) {
        fpsCounter.onDraw();
        if (!ModularUIConfig.guiDebugMode) return;
        if (muiScreen == null) {
            if (checkGui()) {
                muiScreen = currentScreen;
            } else {
                if (fallback == null) return;
                muiScreen = fallback;
            }
        }
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        ModularGuiContext context = muiScreen.getContext();
        int mouseX = context.getMouseX(), mouseY = context.getMouseY();
        int screenH = muiScreen.getScreenArea().height;
        int color = Color.argb(180, 40, 115, 220);
        int lineY = screenH - 13;
        graphics.drawString(Minecraft.getInstance().font, "Mouse Pos: " + mouseX + ", " + mouseY, 5, lineY, color, true);
        lineY -= 11;
        graphics.drawString(Minecraft.getInstance().font, "FPS: " + fpsCounter.getFps(), 5, screenH - 24, color);
        LocatedWidget locatedHovered = muiScreen.getPanelManager().getTopWidgetLocated(true);
        if (locatedHovered != null) {
            drawSegmentLine(lineY -= 4, color);
            lineY -= 10;

            IGuiElement hovered = locatedHovered.getElement();
            locatedHovered.applyMatrix(context);
            graphics.pose().pushPose();
            context.applyTo(graphics.pose());

            Area area = hovered.getArea();
            IGuiElement parent = hovered.getParent();

            GuiDraw.drawBorder(0, 0, area.width, area.height, color, 1f);
            if (hovered.hasParent()) {
                GuiDraw.drawBorder(-area.rx, -area.ry, parent.getArea().width, parent.getArea().height, Color.withAlpha(color, 0.3f), 1f);
            }
            graphics.pose().popPose();
            locatedHovered.unapplyMatrix(context);
            GuiDraw.drawText(graphics, "Pos: " + area.x + ", " + area.y + "  Rel: " + area.rx + ", " + area.ry, 5, lineY, 1, color, false);
            lineY -= 11;
            GuiDraw.drawText(graphics, "Size: " + area.width + ", " + area.height, 5, lineY, 1, color, false);
            lineY -= 11;
            GuiDraw.drawText(graphics, "Class: " + hovered, 5, lineY, 1, color, false);
            if (hovered.hasParent()) {
                drawSegmentLine(lineY -= 4, color);
                lineY -= 10;
                area = parent.getArea();
                GuiDraw.drawText(graphics, "Parent size: " + area.width + ", " + area.height, 5, lineY, 1, color, false);
                lineY -= 11;
                GuiDraw.drawText(graphics, "Parent: " + parent, 5, lineY, 1, color, false);
            }
            if (hovered instanceof ItemSlot slotWidget) {
                drawSegmentLine(lineY -= 4, color);
                lineY -= 10;
                ModularSlot slot = slotWidget.getSlot();
                GuiDraw.drawText(graphics, "Slot Index: " + slot.getSlotIndex(), 5, lineY, 1, color, false);
                lineY -= 11;
                GuiDraw.drawText(graphics, "Slot Number: " + ((Slot) slot).index, 5, lineY, 1, color, false);
                lineY -= 11;
                if (slotWidget.isSynced()) {
                    SlotGroup slotGroup = slot.getSlotGroup();
                    boolean allowShiftTransfer = slotGroup != null && slotGroup.allowShiftTransfer();
                    GuiDraw.drawText(graphics, "Shift-Click Priority: " + (allowShiftTransfer ? slotGroup.getShiftClickPriority() : "DISABLED"), 5, lineY, 1, color, false);
                }
            } else if (hovered instanceof RichTextWidget richTextWidget) {
                drawSegmentLine(lineY -= 4, color);
                lineY -= 10;
                Object hoveredElement = richTextWidget.getHoveredElement();
                GuiDraw.drawText(graphics, "Hovered: " + hoveredElement, 5, lineY, 1, color, false);
            }
        }
        // dot at mouse pos
        GuiDraw.drawRect(mouseX, mouseY, 1, 1, Color.withAlpha(Color.GREEN.main, 0.8f));
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    private static void drawSegmentLine(int y, int color) {
        GuiDraw.drawRect(5, y, 140, 1, color);
    }

    public static boolean hasScreen() {
        return currentScreen != null;
    }

    @Nullable
    public static Screen getMCScreen() {
        return MCHelper.getCurrentScreen();
    }

    @Nullable
    public static ModularScreen getMuiScreen() {
        return currentScreen;
    }

    private static boolean checkGui() {
        return MCHelper.hasMc() && checkGui(Minecraft.getInstance().screen);
    }

    private static boolean checkGui(Screen screen) {
        if (!MCHelper.hasMc() || currentScreen == null || !(screen instanceof IMuiScreen muiScreen)) return false;
        if (screen != Minecraft.getInstance().screen || muiScreen.getScreen() != currentScreen) {
            defaultContext.reset();
            currentScreen = null;
            return false;
        }
        return true;
    }

    public static GuiContext getDefaultContext() {
        return defaultContext;
    }

    public static GuiContext getBestContext() {
        if (checkGui()) {
            return currentScreen.getContext();
        }
        return defaultContext;
    }

    private enum InputPhase {
        // for mui interactions
        EARLY,
        // for mc interactions (like E and ESC)
        LATE;

        public boolean isEarly() {
            return this == EARLY;
        }

        public boolean isLate() {
            return this == LATE;
        }
    }
}
