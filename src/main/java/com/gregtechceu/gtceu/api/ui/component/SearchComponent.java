package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.ScrollContainer;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.texture.TextTexture;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;

import com.lowdragmc.lowdraglib.utils.ISearch;
import com.lowdragmc.lowdraglib.utils.SearchEngine;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import lombok.Setter;

public class SearchComponent<T> extends FlowLayout {

    public final SearchEngine<T> engine;
    public final IComponentSearch<T> search;
    public final ScrollContainer<UIComponent> popUp;
    public final TextBoxComponent textBoxComponent;
    private int capacity = 10;
    protected boolean isShow;
    @Setter
    protected boolean showUp = false;

    public SearchComponent(Sizing horizontalSizing, Sizing verticalSizing, IComponentSearch<T> search) {
        this(horizontalSizing, verticalSizing, search, false);
    }

    public SearchComponent(Sizing horizontalSizing, Sizing verticalSizing, IComponentSearch<T> search,
                           boolean isServer) {
        super(horizontalSizing, verticalSizing, Algorithm.VERTICAL);
        this.child(textBoxComponent = (TextBoxComponent) new TextBoxComponent(horizontalSizing) {

            @Override
            public void onFocusGained(FocusSource source, UIComponent lastFocus) {
                if (lastFocus != null && lastFocus.parent() == this.parent()) {
                    return;
                }
                super.onFocusGained(source, lastFocus);
            }
        }.verticalSizing(verticalSizing));
        this.child(popUp = new ScrollContainer<>(ScrollContainer.ScrollDirection.VERTICAL,
                Sizing.fill(), Sizing.content(15),
                null) {

            @Override
            public void onFocusGained(FocusSource source, UIComponent lastFocus) {
                if (lastFocus != null && lastFocus.parent() == this.parent()) {
                    return;
                }
                super.onFocusGained(source, lastFocus);
                setShow(true);
            }

            @Override
            public void onFocusLost() {
                super.onFocusLost();
                setShow(false);
            }
        });
        popUp.enabled(false);
        this.search = search;
        this.engine = new SearchEngine<>(search, (r) -> {
            int size = popUp.children().size();
            if (showUp) {
                popUp.moveTo(this.x(), this.y() - Math.min(size + 1, capacity) * 15);
            } else {
                popUp.moveTo(this.x(), this.y() + height);
            }
            UITexture text = UITextures.text(search.resultDisplay(r)).width(width).textType(TextTexture.TextType.ROLL);
            popUp.child(UIComponents.button(Component.empty(), cd -> {
                search.selectResult(r);
                setShow(false);
                textBoxComponent.text(search.resultDisplay(r).getString());
            }).renderer(ButtonComponent.Renderer.texture(
                    text,
                    UITextures.group(text, UITextures.colorBorder(Color.BLACK, -1)),
                    text))
                    .positioning(Positioning.absolute(0, size * 15))
                    .sizing(Sizing.fill(), Sizing.fixed(15)));
            if (isServer) {
                // sendMessage(-2, buf -> search.serialize(r, buf));
            }
        });

        textBoxComponent.onChanged().subscribe(value -> {
            popUp.child(null);
            popUp.height(0);
            if (showUp) {
                popUp.moveTo(x(), y());
            } else {
                popUp.moveTo(x(), y() + height());
            }
            setShow(true);
            this.engine.searchWord(value);
            if (isServer) {
                // sendMessage(-1, buffer -> {});
            }
        });
    }

    /*
     * @Override
     * public void receiveMessage(int id, FriendlyByteBuf buffer) {
     * if (id == -1) {
     * popUp.child(null);
     * popUp.height(0);
     * if (showUp) {
     * popUp.moveTo(x(), y());
     * } else {
     * popUp.moveTo(x(), y() + height());
     * }
     * } else if (id == -2) {
     * T r = search.deserialize(buffer);
     * int size = popUp.children().size();
     * int width = width();
     * if (showUp) {
     * popUp.moveTo(this.x(), this.y() - Math.min(size + 1, capacity) * 15);
     * } else {
     * popUp.moveTo(this.x(), this.y() + height);
     * }
     * 
     * UITexture text = UITextures.text(search.resultDisplay(r)).width(width).textType(TextTexture.TextType.ROLL);
     * popUp.child(UIComponents.button(Component.empty(), cd -> {
     * search.selectResult(r);
     * setShow(false);
     * textBoxComponent.text(search.resultDisplay(r).getString());
     * }).renderer(ButtonComponent.Renderer.texture(
     * text,
     * UITextures.group(text, UITextures.colorBorder(Color.BLACK, -1)),
     * text))
     * .positioning(Positioning.absolute(0, size * 15))
     * .sizing(Sizing.fill(), Sizing.fixed(15)));
     * } else {
     * super.receiveMessage(id, buffer);
     * }
     * }
     */

    public SearchComponent<T> setCapacity(int capacity) {
        this.capacity = capacity;
        if (showUp) {
            popUp.moveTo(this.x(), this.y() - Math.min(popUp.children().size() + 1, capacity) * 15);
        } else {
            popUp.moveTo(this.x(), this.y() + height);
        }
        return this;
    }

    public SearchComponent<T> text(String currentString) {
        textBoxComponent.text(currentString);
        return this;
    }

    public String getValue() {
        return textBoxComponent.getValue();
    }

    public void setShow(boolean isShow) {
        this.isShow = isShow;
        popUp.enabled(isShow);
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        boolean lastVisible = popUp.enabled();
        popUp.enabled(false);
        super.draw(graphics, mouseX, mouseY, partialTicks, delta);
        popUp.enabled(lastVisible);

        if (isShow) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            popUp.draw(graphics, mouseX, mouseY, partialTicks, delta);
            graphics.pose().popPose();
        }
    }

    public interface IComponentSearch<T> extends ISearch<T> {

        Component resultDisplay(T value);

        void selectResult(T value);

        /**
         * just used for server side
         */
        default void serialize(T value, FriendlyByteBuf buf) {
            buf.writeComponent(resultDisplay(value));
        }

        /**
         * just used for server side
         */
        default T deserialize(FriendlyByteBuf buf) {
            return (T) buf.readComponent();
        }
    }
}
