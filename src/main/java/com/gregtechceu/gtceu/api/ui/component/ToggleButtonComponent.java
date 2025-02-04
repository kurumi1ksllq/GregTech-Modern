package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.texture.ResourceTexture;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;

import net.minecraft.network.chat.Component;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * @author KilaBash
 * @date 2023/2/22
 * @implNote ToggleButtonWidget
 */
@Accessors(chain = true)
public class ToggleButtonComponent extends SwitchComponent {

    private final UITexture texture;
    @Setter
    private String tooltipText;

    protected ToggleButtonComponent(BooleanSupplier isPressedCondition,
                                    BooleanConsumer setPressedExecutor) {
        this(GuiTextures.VANILLA_BUTTON, isPressedCondition, setPressedExecutor);
    }

    protected ToggleButtonComponent(UITexture buttonTexture,
                                    BooleanSupplier isPressedCondition, BooleanConsumer setPressedExecutor) {
        super((clickData, aBoolean) -> setPressedExecutor.accept(aBoolean.booleanValue()));
        texture = buttonTexture;
        if (buttonTexture instanceof ResourceTexture resourceTexture) {
            texture(resourceTexture.getSubTexture(0, 0, 1, 0.5),
                    resourceTexture.getSubTexture(0, 0.5, 1, 0.5));
        } else {
            texture(buttonTexture, buttonTexture);
        }

        supplier(isPressedCondition);
    }

    public ToggleButtonComponent shouldUseBaseBackground() {
        if (texture != null) {
            texture(
                    UITextures.group(GuiTextures.TOGGLE_BUTTON_BACK.getSubTexture(0, 0, 1, 0.5), texture),
                    UITextures.group(GuiTextures.TOGGLE_BUTTON_BACK.getSubTexture(0, 0.5, 1, 0.5), texture));
        }
        return this;
    }

    @Override
    public int determineHorizontalContentSize(Sizing sizing) {
        return width();
    }

    @Override
    public int determineVerticalContentSize(Sizing sizing) {
        return height();
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        if (tooltipText != null) {
            tooltip(List.of(Component.translatable(tooltipText + (pressed ? ".enabled" : ".disabled"))));
        }
    }
}
