package com.gregtechceu.gtceu.api.ui.fancy;

import com.gregtechceu.gtceu.api.ui.component.LabelComponent;
import com.gregtechceu.gtceu.api.ui.component.TextureComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.util.ClickData;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TitleBarComponent extends StackLayout {

    private static final int BORDER_SIZE = 3;
    public static final int HORIZONTAL_MARGIN = 8;
    public static final int HEIGHT = 16;
    private static final int BUTTON_WIDTH = 18;

    private static final float ROLL_SPEED = 0.7f;

    private boolean showBackButton = false;
    private boolean showMenuButton = false;
    private final int innerHeight;

    /**
     * The button group is rendered behind the main section and contains the back and menu buttons.
     * <p>
     * For easier texture reuse, the background is applied to the group itself, instead of the individual buttons.<br>
     * The button group therefore needs to be rendered behind the main section.
     */
    private final FlowLayout buttonGroup;
    private final UIComponent backButton;
    private final UIComponent menuButton;

    /**
     * The main section contains the current tab's icon and title text
     */
    private final FlowLayout mainSection;
    private final TextureComponent tabIcon;
    private final LabelComponent tabTitle;

    private boolean hasInit = false;

    protected TitleBarComponent(Consumer<ClickData> onBackClicked, Consumer<ClickData> onMenuClicked) {
        super(Sizing.fill(), Sizing.fixed(HEIGHT));
        this.padding(Insets.both(HORIZONTAL_MARGIN, 0));
        // this.margins(Insets.both(HORIZONTAL_MARGIN, 0));
        this.positioning(Positioning.absolute(HORIZONTAL_MARGIN, 0));
        this.innerHeight = HEIGHT - BORDER_SIZE;

        this.buttonGroup = UIContainers.horizontalFlow(Sizing.fill(), Sizing.fixed(innerHeight));
        buttonGroup.positioning(Positioning.relative(50, 100));
        buttonGroup.surface(Surface.TITLE_BAR_BACKGROUND);
        buttonGroup.child(this.backButton = UIComponents.button(Component.literal(" <"), onBackClicked)
                .positioning(Positioning.absolute(0, BORDER_SIZE))
                .sizing(Sizing.fixed(BUTTON_WIDTH), Sizing.fixed(innerHeight)));
        buttonGroup.child(this.menuButton = UIComponents.button(Component.literal("+"), onMenuClicked)
                .positioning(Positioning.relative(100, 100))
                // .margins(Insets.both(BUTTON_WIDTH, BORDER_SIZE))
                .sizing(Sizing.fixed(BUTTON_WIDTH), Sizing.fixed(innerHeight)));
        child(buttonGroup);

        this.mainSection = UIContainers.horizontalFlow(Sizing.fill().andThen(Sizing.fixed(-BUTTON_WIDTH * 2)),
                Sizing.fill());
        mainSection.positioning(Positioning.absolute(BUTTON_WIDTH, 0));
        mainSection.surface(Surface.TITLE_BAR_BACKGROUND);
        mainSection.child(this.tabIcon = (TextureComponent) UIComponents.texture(UITexture.EMPTY)
                .sizing(Sizing.fixed(innerHeight - 2))
                .positioning(Positioning.absolute(BORDER_SIZE + 1, BORDER_SIZE + 1)))
                .child(this.tabTitle = (LabelComponent) UIComponents.label(Component.empty())
                        .rollSpeed(ROLL_SPEED)
                        // .textType(TextTexture.TextType.LEFT_ROLL)
                        .positioning(Positioning.absolute(HEIGHT, BORDER_SIZE)));
        child(mainSection);

        hasInit = true;
        updateLayout();
    }

    public void updateState(IFancyUIProvider currentPage, boolean showBackButton, boolean showMenuButton) {
        this.showBackButton = showBackButton;
        this.showMenuButton = showMenuButton;

        tabTitle.text(currentPage.getTitle().copy().withStyle(ChatFormatting.BLACK))
                .maxWidth(this.width());

        tabIcon.texture(currentPage.getTabIcon());

        backButton.enabled(showBackButton);
        menuButton.enabled(showMenuButton);

        updateLayout();
    }

    @Override
    protected void updateLayout() {
        super.updateLayout();
        if (!hasInit) return;

        var hiddenButtons = 2;
        if (showBackButton) hiddenButtons--;
        if (showMenuButton) hiddenButtons--;

        int buttonGroupWidth = this.width - (BUTTON_WIDTH * hiddenButtons);
        buttonGroup.sizing(Sizing.fixed(buttonGroupWidth), Sizing.fixed(innerHeight));
        // buttonGroup.mount(this, showBackButton ? 0 : BUTTON_WIDTH, BORDER_SIZE);
        // menuButton.mount(this, buttonGroupWidth - BUTTON_WIDTH, BORDER_SIZE);

        int mainSectionWidth = this.width - (BUTTON_WIDTH * 2);
        int titleWidth = mainSectionWidth - (2 * BORDER_SIZE) - innerHeight;
        mainSection.sizing(Sizing.fixed(mainSectionWidth), Sizing.fill());
        tabTitle.maxWidth(titleWidth);
        tabTitle.sizing(Sizing.fixed(titleWidth), Sizing.fill());

        super.updateLayout();
    }
}
