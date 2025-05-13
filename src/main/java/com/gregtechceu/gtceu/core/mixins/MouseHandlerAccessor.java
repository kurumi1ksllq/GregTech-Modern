package com.gregtechceu.gtceu.core.mixins;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {

    @Accessor
    int getTouchValue();

    @Accessor
    void setTouchValue(int value);

    @Accessor
    int getEventButton();

    @Accessor
    void setEventButton(int button);

    @Accessor
    double getMousePressedTime();

    @Accessor
    void setMousePressedTime(double mousePressedTime);
}
