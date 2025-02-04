package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.util.ClickData;

import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class PredicatedButtonComponent extends ButtonComponent {

    private final BooleanSupplier predicate;

    public PredicatedButtonComponent(UITexture buttonTexture, Consumer<ClickData> onPressed,
                                     BooleanSupplier predicate, boolean defaultVisibility) {
        super(Component.empty(), onPressed);
        this.predicate = predicate;
        this.renderer(Renderer.texture(buttonTexture));
        visible(defaultVisibility);
    }

    public PredicatedButtonComponent(UITexture buttonTexture, Consumer<ClickData> onPressed,
                                     BooleanSupplier predicate) {
        this(buttonTexture, onPressed, predicate, false);
    }

    public PredicatedButtonComponent(Consumer<ClickData> onPressed,
                                     BooleanSupplier predicate) {
        super(Component.empty(), onPressed);
        this.predicate = predicate;
    }

    /*
     * @Override
     * public void writeInitialData(FriendlyByteBuf buffer) {
     * super.writeInitialData(buffer);
     * var result = predicate == null || predicate.getAsBoolean();
     * visible(result);
     * buffer.writeBoolean(result);
     * }
     * 
     * @Override
     * public void readInitialData(FriendlyByteBuf buffer) {
     * super.readInitialData(buffer);
     * setVisible(buffer.readBoolean());
     * }
     * 
     * @Override
     * public void detectAndSendChanges() {
     * super.detectAndSendChanges();
     * if (predicate != null) {
     * if (isVisible() != predicate.getAsBoolean()) {
     * setVisible(!isVisible());
     * writeUpdateInfo(1, buf -> buf.writeBoolean(isVisible()));
     * }
     * }
     * }
     */

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        if (visible() != predicate.getAsBoolean()) {
            visible(!visible());
            // sendMessage(1, buf -> buf.writeBoolean(visible()));
        }
    }

    /*
     * @Override
     * public void receiveMessage(int id, FriendlyByteBuf buf) {
     * if (id == 1) {
     * visible(buf.readBoolean());
     * } else {
     * super.receiveMessage(id, buf);
     * }
     * }
     */
}
