package com.gregtechceu.gtceu.api.mui.drawable;

import com.gregtechceu.gtceu.api.mui.base.drawable.IIcon;
import com.gregtechceu.gtceu.api.mui.base.widget.IGuiAction;
import com.gregtechceu.gtceu.api.mui.base.widget.Interactable;
import org.jetbrains.annotations.NotNull;

public class InteractableIcon extends DelegateIcon implements Interactable {

    private IGuiAction.MousePressed mousePressed;
    private IGuiAction.MouseReleased mouseReleased;
    private IGuiAction.MousePressed mouseTapped;
    private IGuiAction.MouseScroll mouseScroll;
    private IGuiAction.KeyPressed keyPressed;
    private IGuiAction.KeyReleased keyReleased;
    private IGuiAction.KeyPressed keyTapped;

    public InteractableIcon(IIcon icon) {
        super(icon);
    }

    public void playClickSound() {
        //if (this.playClickSound) {
        Interactable.playButtonClickSound();
        //}
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (this.mousePressed != null && this.mousePressed.press(mouseButton)) {
            playClickSound();
            return Result.SUCCESS;
        }
        return Result.ACCEPT;
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        return this.mouseReleased != null && this.mouseReleased.release(mouseButton);
    }

    @NotNull
    @Override
    public Result onMouseTapped(int mouseButton) {
        if (this.mouseTapped != null && this.mouseTapped.press(mouseButton)) {
            playClickSound();
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }

    @Override
    public @NotNull Result onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.keyPressed != null && this.keyPressed.press(typedChar, keyCode)) {
            return Result.SUCCESS;
        }
        return Result.ACCEPT;
    }

    @Override
    public boolean onKeyRelease(int keyCode, int scanCode, int modifiers) {
        return this.keyReleased != null && this.keyReleased.release(typedChar, keyCode);
    }

    @NotNull
    @Override
    public Result onKeyTapped(int keyCode, int scanCode, int modifiers) {
        if (this.keyTapped != null && this.keyTapped.press(typedChar, keyCode)) {
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double delta) {
        return this.mouseScroll != null && this.mouseScroll.scroll(mouseX, delta, );
    }

    public InteractableIcon onMousePressed(IGuiAction.MousePressed mousePressed) {
        this.mousePressed = mousePressed;
        return this;
    }

    public InteractableIcon onMouseReleased(IGuiAction.MouseReleased mouseReleased) {
        this.mouseReleased = mouseReleased;
        return this;
    }

    public InteractableIcon onMouseTapped(IGuiAction.MousePressed mouseTapped) {
        this.mouseTapped = mouseTapped;
        return this;
    }

    public InteractableIcon onMouseScrolled(IGuiAction.MouseScroll mouseScroll) {
        this.mouseScroll = mouseScroll;
        return this;
    }

    public InteractableIcon onKeyPressed(IGuiAction.KeyPressed keyPressed) {
        this.keyPressed = keyPressed;
        return this;
    }

    public InteractableIcon onKeyReleased(IGuiAction.KeyReleased keyReleased) {
        this.keyReleased = keyReleased;
        return this;
    }

    public InteractableIcon onKeyTapped(IGuiAction.KeyPressed keyTapped) {
        this.keyTapped = keyTapped;
        return this;
    }
}
