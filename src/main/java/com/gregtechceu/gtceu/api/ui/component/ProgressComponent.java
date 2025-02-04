package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.base.BaseUIComponent;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.parsing.UIModel;
import com.gregtechceu.gtceu.api.ui.texture.ProgressTexture;
import com.gregtechceu.gtceu.api.ui.texture.ResourceTexture;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.w3c.dom.Element;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;

@Accessors(fluent = true, chain = true)
public class ProgressComponent extends BaseUIComponent {

    public final static DoubleSupplier JEIProgress = () -> Math.abs(System.currentTimeMillis() % 2000) / 2000.;
    @Setter
    public DoubleSupplier progressSupplier;
    @Setter
    private Function<Double, Component> dynamicHoverTips;
    @Setter
    private UITexture progressTexture;
    @Setter
    private UITexture overlayTexture;
    @Getter
    private double lastProgressValue;

    private List<Component> serverTooltips = new ArrayList<>();
    @Setter
    private Consumer<List<Component>> serverTooltipSupplier;

    public ProgressComponent() {
        this(JEIProgress, ProgressTexture.EMPTY);
    }

    public ProgressComponent(DoubleSupplier progressSupplier, ResourceTexture fullImage) {
        this.progressSupplier = progressSupplier;
        this.progressTexture = UITextures.progress(fullImage.getSubTexture(0.0, 0.0, 1.0, 0.5),
                fullImage.getSubTexture(0.0, 0.5, 1.0, 0.5));
        this.lastProgressValue = -1;
    }

    protected ProgressComponent(DoubleSupplier progressSupplier, ProgressTexture progressBar) {
        this.progressSupplier = progressSupplier;
        this.progressTexture = progressBar;
        this.lastProgressValue = -1;
    }

    protected ProgressComponent(DoubleSupplier progressSupplier) {
        this.progressSupplier = progressSupplier;
    }

    public ProgressComponent progressTexture(UITexture emptyBarArea, UITexture filledBarArea) {
        this.progressTexture = UITextures.progress(emptyBarArea, filledBarArea);
        return this;
    }

    public ProgressComponent fillDirection(ProgressTexture.FillDirection fillDirection) {
        if (this.progressTexture instanceof ProgressTexture progressTexture) {
            progressTexture.fillDirection(fillDirection);
        }
        return this;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.progressTexture != null) {
            this.progressTexture.updateTick();
        }
        if (this.overlayTexture != null) {
            this.overlayTexture.updateTick();
        }
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        if (((tooltip() != null && !tooltip().isEmpty()) || dynamicHoverTips != null)) {
            var tips = new ArrayList<>(Objects.requireNonNullElseGet(tooltip(), Collections::emptyList));
            if (dynamicHoverTips != null) {
                tips.add(ClientTooltipComponent.create(dynamicHoverTips.apply(lastProgressValue).getVisualOrderText()));
            }
            this.tooltip(tips);
        }
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        if (progressSupplier == JEIProgress) {
            lastProgressValue = progressSupplier.getAsDouble();
        }
        if (progressTexture instanceof ProgressTexture texture) {
            texture.setProgress(lastProgressValue);
        }
        if (progressTexture != null) {
            progressTexture.draw(graphics, mouseX, mouseY, x(), y(), width(), height());
        }
        if (overlayTexture != null) {
            overlayTexture.draw(graphics, mouseX, mouseY, x(), y(), width(), height());
        }
    }

    @Override
    public void init() {
        super.init();
        this.lastProgressValue = progressSupplier.getAsDouble();
    }

    @Override
    public int determineHorizontalContentSize(Sizing sizing) {
        return width();
    }

    @Override
    public int determineVerticalContentSize(Sizing sizing) {
        return height();
    }

    /*
     * @Override
     * public void writeInitialData(FriendlyByteBuf buffer) {
     * super.writeInitialData(buffer);
     * buffer.writeDouble(lastProgressValue);
     * }
     * 
     * @Override
     * public void readInitialData(FriendlyByteBuf buffer) {
     * super.readInitialData(buffer);
     * lastProgressValue = buffer.readDouble();
     * }
     * 
     * @Override
     * public void detectAndSendChanges() {
     * double actualValue = progressSupplier.getAsDouble();
     * if (actualValue - lastProgressValue != 0) {
     * this.lastProgressValue = actualValue;
     * sendMessage(0, buffer -> buffer.writeDouble(actualValue));
     * }
     * }

    @Override
    public void receiveMessage(int id, FriendlyByteBuf buf) {
        if (id == 0) {
            this.lastProgressValue = buf.readDouble();
        }
    }
    */

    @Override
    public void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        super.parseProperties(model, element, children);

        var textureElement = children.get("progress-texture");
        this.progressTexture(model.parseTexture(ProgressTexture.class, textureElement));

        var overlayElement = children.get("overlay-texture");
        this.overlayTexture(model.parseTexture(UITexture.class, overlayElement));
    }
}
