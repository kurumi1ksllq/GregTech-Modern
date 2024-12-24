package com.gregtechceu.gtceu.api.ui.texture;

import com.gregtechceu.gtceu.api.ui.core.Color;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.parsing.UIModel;
import com.gregtechceu.gtceu.api.ui.parsing.UIModelParsingException;
import com.gregtechceu.gtceu.api.ui.parsing.UIParsing;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.w3c.dom.Element;

import java.util.Map;

@Accessors(fluent = true, chain = true)
public class ItemStackTexture extends TransformTexture {

    @Getter
    @Setter
    protected ItemStack stack;
    @Getter
    @Setter
    private Color color = Color.WHITE;

    public ItemStackTexture(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    protected void drawInternal(UIGuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width,
                                float height) {
        RenderSystem.setShaderColor(this.color().red(), this.color().green(), this.color().blue(),
                this.color().alpha());

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        graphics.pose().pushPose();
        graphics.pose().scale(width / 16f, height / 16f, 1);
        graphics.pose().translate(x * 16 / width, y * 16 / height, 32);
        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0);
        graphics.pose().popPose();

        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
    }

    @Override
    public void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        super.parseProperties(model, element, children);

        UIParsing.apply(children, "stack", $ -> $.getTextContent().strip(), stackString -> {
            try {
                var result = ItemParser.parseForItem(BuiltInRegistries.ITEM.asLookup(), new StringReader(stackString));

                var stack = new ItemStack(result.item());
                stack.setTag(result.nbt());

                this.stack(stack);
            } catch (CommandSyntaxException cse) {
                throw new UIModelParsingException("Invalid item stack", cse);
            }
        });
    }
}
