package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;

import net.minecraft.network.FriendlyByteBuf;

import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.BooleanSupplier;

@Accessors(fluent = true, chain = true)
public class PredicatedTextureComponent extends TextureComponent {

    @Setter
    private BooleanSupplier predicate;
    private boolean isVisible = true;

    public PredicatedTextureComponent(UITexture area) {
        super(area);
    }

    /*
     * @Override
     * public void writeInitialData(FriendlyByteBuf buffer) {
     * super.writeInitialData(buffer);
     * isVisible = predicate == null || predicate.getAsBoolean();
     * buffer.writeBoolean(isVisible);
     * }
     * 
     * @Override
     * public void readInitialData(FriendlyByteBuf buffer) {
     * super.readInitialData(buffer);
     * isVisible = buffer.readBoolean();
     * }
     * 
     * @Override
     * public void detectAndSendChanges() {
     * super.detectAndSendChanges();
     * if (predicate != null) {
     * if (isVisible != predicate.getAsBoolean()) {
     * isVisible = !isVisible;
     * writeUpdateInfo(1, buf -> buf.writeBoolean(isVisible));
     * }
     * }
     * }
     */

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        isVisible = predicate.getAsBoolean();
    }

    /*
    @Override
    public void receiveMessage(int id, FriendlyByteBuf buf) {
        if (id == 1) {
            isVisible = buf.readBoolean();
        } else {
            super.receiveMessage(id, buf);
        }
    }
    */

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        if (isVisible) {
            super.draw(graphics, mouseX, mouseY, partialTicks, delta);
        }
    }
}
