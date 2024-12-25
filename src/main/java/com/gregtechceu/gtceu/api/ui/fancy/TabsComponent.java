package com.gregtechceu.gtceu.api.ui.fancy;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.base.BaseUIComponent;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
public class TabsComponent extends BaseUIComponent {

    protected final Consumer<IFancyUIProvider> onTabClick;
    protected IFancyUIProvider mainTab;
    protected List<IFancyUIProvider> subTabs;
    @Nullable
    protected IFancyUIProvider selectedTab;
    @Setter
    protected UITexture leftButtonTexture = UITextures.group(GuiTextures.BUTTON, GuiTextures.BUTTON_LEFT),
            leftButtonHoverTexture = UITextures.group(GuiTextures.BUTTON,
                    GuiTextures.BUTTON_LEFT.copy().color(0xffaaaaaa));
    @Setter
    protected UITexture rightButtonTexture = UITextures.group(GuiTextures.BUTTON, GuiTextures.BUTTON_RIGHT),
            rightButtonHoverTexture = UITextures.group(GuiTextures.BUTTON,
                    GuiTextures.BUTTON_RIGHT.copy().color(0xffaaaaaa));
    @Setter
    protected UITexture tabTexture = UITextures.resource(GTCEu.id("textures/gui/tab/tabs_top.png"))
            .getSubTexture(1 / 3f, 0, 1 / 3f, 0.5f);
    @Setter
    protected UITexture tabHoverTexture = UITextures.resource(GTCEu.id("textures/gui/tab/tabs_top.png"))
            .getSubTexture(1 / 3f, 0.5f, 1 / 3f, 0.5f);
    @Setter
    protected UITexture tabPressedTexture = tabHoverTexture;
    @Getter
    protected int offset;
    /**
     * (old tab, new tab)
     */
    @Setter
    @Nullable
    protected BiConsumer<IFancyUIProvider, IFancyUIProvider> onTabSwitch;

    public TabsComponent(Consumer<IFancyUIProvider> onTabClick) {
        this.subTabs = new ArrayList<>();
        this.onTabClick = onTabClick;
    }

    public void setMainTab(IFancyUIProvider mainTab) {
        this.mainTab = mainTab;
        if (this.selectedTab == null) {
            this.selectedTab = this.mainTab;
        }
    }

    public void clearSubTabs() {
        subTabs.clear();
    }

    public void attachSubTab(IFancyUIProvider subTab) {
        subTabs.add(subTab);
    }

    public boolean hasButton() {
        return (subTabs.size() + 1) * 24 + 16 > width;
    }

    /*
    @Override
    public void receiveMessage(int id, FriendlyByteBuf buf) {
        super.receiveMessage(id, buf);
        if (id == 0) {
            var index = buf.readVarInt();
            var old = selectedTab;
            if (index < 0) {
                selectedTab = mainTab;
            } else if (index < subTabs.size()) {
                selectedTab = subTabs.get(index);
            } else {
                return;
            }
            if (onTabSwitch != null) {
                onTabSwitch.accept(old, selectedTab);
            }
            onTabClick.accept(selectedTab);
        }
    }
    */

    public int getSubTabsWidth() {
        return width - 8 - 24 - 4 - 16 - 8 - 16;
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            var hoveredTab = getHoveredTab(mouseX, mouseY);
            // click tab
            if (hoveredTab != null && hoveredTab != selectedTab) {
                if (onTabSwitch != null) {
                    onTabSwitch.accept(selectedTab, hoveredTab);
                }
                selectedTab = hoveredTab;
                //sendMessage(0, buf -> buf.writeVarInt(selectedTab == mainTab ? -1 : subTabs.indexOf(selectedTab)));
                onTabClick.accept(selectedTab);
                UIComponent.playButtonClickSound();
            }
            // click button
            if (hasButton()) {
                if (isHoverLeftButton(mouseX, mouseY)) {
                    offset = Mth.clamp(offset - 24, 0, subTabs.size() * 24 - getSubTabsWidth());
                    UIComponent.playButtonClickSound();
                } else if (isHoverRightButton(mouseX, mouseY)) {
                    offset = Mth.clamp(offset + 24, 0, subTabs.size() * 24 - getSubTabsWidth());
                    UIComponent.playButtonClickSound();
                }
            }
        }
        return super.onMouseDown(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        var sx = x + 8 + 24 + 4 + 16;
        if (UIComponent.isMouseOver(sx, y, getSubTabsWidth(), 24, mouseX, mouseY)) {
            offset = Mth.clamp(offset + 5 * (amount > 0 ? -1 : 1), 0, subTabs.size() * 24 - getSubTabsWidth());
        }
        return super.onMouseScroll(mouseX, mouseY, amount);
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        var hoveredTab = getHoveredTab(mouseX, mouseY);
        if (hoveredTab == null) {
            return;
        }
        updateTooltip(hoveredTab);
        // main tab
        drawTab(mainTab, graphics, mouseX, mouseY, x + 8, y, 24, 24, hoveredTab);
        // render sub tabs
        if (hasButton()) { // need a scrollable bar
            // render buttons
            if (isHoverLeftButton(mouseX, mouseY)) {
                leftButtonHoverTexture.draw(graphics, mouseX, mouseY, x + 8 + 24 + 4, y, 16, 24);
            } else {
                leftButtonTexture.draw(graphics, mouseX, mouseY, x + 8 + 24 + 4, y, 16, 24);
            }
            if (isHoverRightButton(mouseX, mouseY)) {
                rightButtonHoverTexture.draw(graphics, mouseX, mouseY, x + width - 8 - 16, y, 16,
                        24);
            } else {
                rightButtonTexture.draw(graphics, mouseX, mouseY, x + width - 8 - 16, y, 16, 24);
            }
            // render sub tabs
            var sx = x + 8 + 24 + 4 + 16;
            graphics.enableScissor(sx, y - 1, sx + getSubTabsWidth(), y - 1 + 24 + 2);
            for (int i = 0; i < subTabs.size(); i++) {
                drawTab(subTabs.get(i), graphics, mouseX, mouseY, sx + i * 24 - offset, y, 24, 24, hoveredTab);
            }
            graphics.disableScissor();
        } else {
            for (int i = subTabs.size() - 1; i >= 0; i--) {
                drawTab(subTabs.get(i), graphics, mouseX, mouseY,
                        x + width - 8 - 24 * (subTabs.size() - i), y, 24, 24, hoveredTab);
            }
        }
    }

    protected void updateTooltip(IFancyUIProvider tab) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tab.getTabTooltips().stream()
                .map(c -> ClientTooltipComponent.create(c.getVisualOrderText()))
                .forEach(tooltip::add);
        if (tab.getTabTooltipComponent() != null) {
            tooltip.add(ClientTooltipComponent.create(tab.getTabTooltipComponent()));
        }
        this.tooltip(tooltip);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isHoverLeftButton(double mouseX, double mouseY) {
        return UIComponent.isMouseOver(x + 8 + 24 + 4, y, 16, 24, mouseX, mouseY);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isHoverRightButton(double mouseX, double mouseY) {
        return UIComponent.isMouseOver(x + width - 8 - 16, y, 16, 24, mouseX, mouseY);
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public IFancyUIProvider getHoveredTab(double mouseX, double mouseY) {
        if (isMouseOverElement(mouseX, mouseY)) {
            // main tab
            if (UIComponent.isMouseOver(x + 8, y, 24, 24, mouseX, mouseY)) {
                return mainTab;
            }
            // others
            if (hasButton()) { // need a scrollable bar
                var sx = x + 8 + 24 + 4 + 16;
                if (UIComponent.isMouseOver(sx, y, getSubTabsWidth(), 24, mouseX, mouseY)) {
                    var i = ((int) mouseX - sx + getOffset()) / 24;
                    if (i < subTabs.size()) {
                        return subTabs.get(i);
                    }
                }
            } else {
                int i = (x + width - 8 - (int) mouseX) / 24;
                if (i < subTabs.size()) {
                    return subTabs.get(subTabs.size() - 1 - i);
                }
            }
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public void drawTab(IFancyUIProvider tab, @NotNull UIGuiGraphics graphics, int mouseX, int mouseY,
                        int x, int y, int width, int height,
                        IFancyUIProvider hoveredTab) {
        // render background
        if (tab == selectedTab) {
            tabPressedTexture.draw(graphics, mouseX, mouseY, x, y, width, height);
        } else if (tab == hoveredTab) {
            tabHoverTexture.draw(graphics, mouseX, mouseY, x, y, width, height);
        } else {
            tabTexture.draw(graphics, mouseX, mouseY, x, y, width, height);
        }
        // render icon
        tab.getTabIcon().draw(graphics, mouseX, mouseY, x + (width - 16) / 2f, y + (height - 16) / 2f, 16, 16);
    }

    public void selectTab(IFancyUIProvider selectedTab) {
        this.selectedTab = selectedTab;
        // TODO
        // this.detectAndSendChanges();
    }
}
