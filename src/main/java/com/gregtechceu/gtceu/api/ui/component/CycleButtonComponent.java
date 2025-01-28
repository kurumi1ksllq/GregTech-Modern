package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.base.BaseUIComponent;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;

import net.minecraft.network.FriendlyByteBuf;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

@Accessors(fluent = true, chain = true)
public class CycleButtonComponent extends BaseUIComponent {

    @Setter
    protected Int2ObjectFunction<UITexture> texture;
    @Setter
    protected UITexture current;
    @Setter
    protected IntConsumer onChanged;
    @Setter
    protected IntSupplier indexSupplier;
    protected int range, index;

    public CycleButtonComponent(int range, Int2ObjectFunction<UITexture> texture, IntConsumer onChanged) {
        this.texture = texture;
        this.onChanged = onChanged;
        this.range = range;
        current = texture.get(0);
    }

    public void setIndex(int index) {
        this.index = index;
        current = texture.get(index);
    }

    /*
     * @Override
     * public void writeInitialData(FriendlyByteBuf buffer) {
     * super.writeInitialData(buffer);
     * if (indexSupplier != null) {
     * index = indexSupplier.getAsInt();
     * }
     * buffer.m_130130_(index);
     * }
     * 
     * @Override
     * public void readInitialData(FriendlyByteBuf buffer) {
     * super.readInitialData(buffer);
     * index = buffer.m_130242_();
     * setBackground(current.get(index));
     * }
     */

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        current.draw(graphics, mouseX, mouseY, x(), y(), width(), height());
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        if (indexSupplier != null) {
            var newIndex = indexSupplier.getAsInt();
            if (newIndex != index) {
                index = newIndex;
                //sendMessage(1, buf -> buf.writeVarInt(index));
            }
        }
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            if(button == MOUSE_BUTTON_RIGHT) {
                index--;
            }
            else if(button == MOUSE_BUTTON_LEFT) {
                index++;
            }
            index = (index + range) % range;

            current = texture.get(index);
            if (onChanged != null) {
                onChanged.accept(index);
            }
            // sendMessage(1, buf -> buf.writeVarInt(index));
            UIComponent.playButtonClickSound();
            return true;
        }
        return false;
    }

    // FIXME
    /*
    @Override
    public void receiveMessage(int id, FriendlyByteBuf buf) {
        if (id == 1) {
            index = buf.readVarInt();
            if (onChanged != null) {
                onChanged.accept(index);
            }
        } else {
            super.receiveMessage(id, buf);
        }
    }
    */
}
