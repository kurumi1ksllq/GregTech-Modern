package com.gregtechceu.gtceu.api.mui.factory;

import com.gregtechceu.gtceu.api.mui.base.IGuiHolder;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

public class GuiFactories {

    public static BlockEntityGuiFactory blockEntity() {
        return BlockEntityGuiFactory.INSTANCE;
    }

    public static SidedBlockEntityGuiFactory sidedBlockEntity() {
        return SidedBlockEntityGuiFactory.INSTANCE;
    }

    public static ItemGuiFactory item() {
        return ItemGuiFactory.INSTANCE;
    }

    public static SimpleGuiFactory createSimple(ResourceLocation name, IGuiHolder<GuiData> holder) {
        return new SimpleGuiFactory(name, holder);
    }

    public static SimpleGuiFactory createSimple(ResourceLocation name, Supplier<IGuiHolder<GuiData>> holder) {
        return new SimpleGuiFactory(name, holder);
    }

    @ApiStatus.Internal
    public static void init() {
        GuiManager.registerFactory(blockEntity());
        GuiManager.registerFactory(sidedBlockEntity());
        GuiManager.registerFactory(item());
    }

    private GuiFactories() {}
}
