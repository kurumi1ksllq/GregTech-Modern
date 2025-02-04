package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.base.BaseUIComponent;
import com.gregtechceu.gtceu.api.ui.core.Color;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;
import com.gregtechceu.gtceu.api.ui.util.ClickData;

import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

@Accessors(fluent = true, chain = true)
public class SwitchComponent extends BaseUIComponent {

    @Setter
    protected UITexture baseTexture;
    @Setter
    protected UITexture pressedTexture;
    @Setter
    protected UITexture hoverTexture;
    protected boolean pressed;

    @Setter
    protected BiConsumer<ClickData, Boolean> onPressCallback;
    @Setter
    protected BooleanSupplier supplier;

    protected SwitchComponent(BiConsumer<ClickData, Boolean> onPressed) {
        this.onPressCallback = onPressed;
    }

    public SwitchComponent texture(UITexture baseTexture, UITexture pressedTexture) {
        this.baseTexture = baseTexture;
        this.pressedTexture = pressedTexture;
        return this;
    }

    @Tolerate
    public SwitchComponent baseTexture(UITexture... baseTexture) {
        this.baseTexture = UITextures.group(baseTexture);
        return this;
    }

    @Tolerate
    public SwitchComponent pressedTexture(UITexture... pressedTexture) {
        this.pressedTexture = UITextures.group(pressedTexture);
        return this;
    }

    @Tolerate
    public SwitchComponent hoverTexture(UITexture... hoverTexture) {
        this.hoverTexture = UITextures.group(hoverTexture);
        return this;
    }

    public SwitchComponent hoverBorderTexture(int border, int color) {
        this.hoverTexture = UITextures.colorBorder(Color.ofArgb(color), border);
        return this;
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        if (baseTexture != null) {
            baseTexture.updateTick();
        }
        if (pressedTexture != null) {
            pressedTexture.updateTick();
        }
        if (hoverTexture != null) {
            hoverTexture.updateTick();
        }
        if (supplier != null) {
            pressed(supplier.getAsBoolean());
        }
    }

    /*
     * @Override
     * public void writeInitialData(FriendlyByteBuf buffer) {
     * buffer.writeBoolean(isPressed);
     * }
     * 
     * @Override
     * public void readInitialData(FriendlyByteBuf buffer) {
     * isPressed = buffer.readBoolean();
     * }
     * 
     * @Override
     * public void detectAndSendChanges() {
     * super.detectAndSendChanges();
     * if (!isClientSideWidget && supplier != null) {
     * setPressed(supplier.get());
     * }
     * }
     */

    public SwitchComponent pressed(boolean isPressed) {
        if (this.pressed == isPressed) return this;
        this.pressed = isPressed;
        // sendMessage(2, buf -> buf.writeBoolean(isPressed));
        return this;
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        if (baseTexture != null && !pressed) {
            baseTexture.draw(graphics, mouseX, mouseY, x(), y(), width(), height());

        } else if (pressedTexture != null && pressed) {
            pressedTexture.draw(graphics, mouseX, mouseY, x(), y(), width(), height());

        }
        if (isMouseOverElement(mouseX, mouseY) && hoverTexture != null) {
            hoverTexture.draw(graphics, mouseX, mouseY, x(), y(), width(), height());
        }
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            pressed = !pressed;
            ClickData clickData = new ClickData(button);
            /*
             * sendMessage(1, buffer -> {
             * clickData.writeToBuf(buffer);
             * buffer.writeBoolean(pressed);
             * });
             */
            if (onPressCallback != null) {
                onPressCallback.accept(clickData, pressed);
            }
            UIComponent.playButtonClickSound();
            return true;
        }
        return false;
    }

    /*
     * @Override
     * public void receiveMessage(int id, FriendlyByteBuf buf) {
     * if (id == 1) {
     * if (onPressCallback != null) {
     * ClickData clickData = ClickData.readFromBuf(buf);
     * onPressCallback.accept(clickData, pressed = buf.readBoolean());
     * }
     * } else if (id == 2) {
     * pressed = buf.readBoolean();
     * } else {
     * super.receiveMessage(id, buf);
     * }
     * }
     */
}
