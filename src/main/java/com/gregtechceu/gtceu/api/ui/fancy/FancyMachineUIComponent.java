package com.gregtechceu.gtceu.api.ui.fancy;

import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.component.PlayerInventoryComponent;
import com.gregtechceu.gtceu.api.ui.component.TextureComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.util.ClickData;
import com.gregtechceu.gtceu.core.mixins.ui.accessor.AbstractContainerScreenAccessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

@ApiStatus.Internal
@Accessors(fluent = true, chain = true)
@Getter
public class FancyMachineUIComponent extends StackLayout {

    protected final TitleBarComponent titleBar;
    protected final VerticalTabsComponent sideTabsComponent;
    protected final FlowLayout pageContainer;
    protected final TextureComponent background;
    protected final PageSwitcherComponent pageSwitcher;
    @Getter
    protected final ConfiguratorPanelComponent configuratorPanel;
    protected final TooltipsPanelComponent tooltipsPanel;

    @Nullable
    protected final PlayerInventoryComponent playerInventory;
    @Setter
    protected int border = 4;

    protected final IFancyUIProvider mainPage;

    /*
     * Current Page: The page visible in the UI
     * Current Home Page: The currently selected multiblock part's home page.
     */
    protected IFancyUIProvider currentPage;
    protected IFancyUIProvider currentHomePage;

    protected List<IFancyUIProvider> allPages;

    protected Deque<NavigationEntry> previousPages = new ArrayDeque<>();

    protected record NavigationEntry(IFancyUIProvider page, IFancyUIProvider homePage, Runnable onNavigation) {

    }

    public FancyMachineUIComponent(IFancyUIProvider mainPage,
                                   Sizing horizontalSizing, Sizing verticalSizing) {
        super(horizontalSizing.andThen(Sizing.fixed(20)), verticalSizing.andThen(Sizing.fixed(16)));
        // this.margins(Insets.of(-border));
        this.mainPage = mainPage;

        child(this.background = UIComponents.texture(GuiTextures.BACKGROUND)
                .configure(c -> {
                    c.positioning(Positioning.absolute(20, 16))
                            .sizing(horizontalSizing, verticalSizing);
                }));
        child(this.pageContainer = UIContainers.horizontalFlow(horizontalSizing, verticalSizing)
                .configure(c -> {
                    c.positioning(Positioning.absolute(20, 16));
                }));

        if (mainPage.hasPlayerInventory()) {
            child(this.playerInventory = UIComponents.playerInventory()
                    .configure(c -> {
                        c.margins(Insets.of(5, 5, 20 + 5, 5))
                                .positioning(Positioning.relative(0, 100));
                    }));
        } else {
            playerInventory = null;
        }

        child(this.titleBar = new TitleBarComponent(this::navigateBack, this::openPageSwitcher)
                .configure(c -> {
                    c.positioning(Positioning.absolute(20, 0))
                            .horizontalSizing(horizontalSizing);
                }));
        child(this.sideTabsComponent = (VerticalTabsComponent) new VerticalTabsComponent(this::navigate)
                .positioning(Positioning.absolute(0, TitleBarComponent.HEIGHT))
                .sizing(Sizing.fixed(24), Sizing.fill()));
        child(this.tooltipsPanel = new TooltipsPanelComponent());
        child(this.configuratorPanel = new ConfiguratorPanelComponent()
                .configure(c -> {
                    c.positioning(Positioning.absolute(-(4 + 2), height));
                }));
        this.pageSwitcher = new PageSwitcherComponent(this::switchPage);

        // surface(GuiTextures.BACKGROUND.copy()
        // .setColor(Long.decode(ConfigHolder.INSTANCE.client.defaultUIColor).intValue() | 0xFF000000));
    }

    @Override
    public void init() {
        super.init();

        if (this.playerInventory != null) {
            this.playerInventory.setByInventory(player().getInventory(), GuiTextures.SLOT);
        }

        this.allPages = Stream.concat(Stream.of(this.mainPage), this.mainPage.getSubTabs().stream()).toList();

        performNavigation(this.mainPage, this.mainPage);
    }

    ////////////////////////////////////////
    // ********* NAVIGATION *********//

    /// /////////////////////////////////////

    protected void navigate(IFancyUIProvider newPage) {
        navigate(newPage, this.currentHomePage);
    }

    protected void navigate(IFancyUIProvider nextPage, IFancyUIProvider nextHomePage) {
        if (nextPage != mainPage) {
            if (!this.previousPages.isEmpty() && this.previousPages.peek().page == nextPage) {
                // In case the user manually navigates back one step, just remove it from the navigation stack
                this.previousPages.pop();
            } else if (this.currentPage != null) {
                this.previousPages.push(new NavigationEntry(this.currentPage, this.currentHomePage, () -> {}));
            }
        } else {
            this.previousPages.clear();
        }

        performNavigation(nextPage, nextHomePage);
    }

    protected void navigateBack(ClickData clickData) {
        NavigationEntry navigationEntry = previousPages.pollFirst();
        if (navigationEntry == null) return;

        performNavigation(navigationEntry.page, navigationEntry.homePage);
        navigationEntry.onNavigation.run();
    }

    protected void performNavigation(IFancyUIProvider nextPage, IFancyUIProvider nextHomePage) {
        if (currentHomePage != nextHomePage)
            setupSideTabs(nextHomePage);

        this.currentPage = nextPage;
        this.currentHomePage = nextHomePage;

        if (currentPage != currentHomePage) {
            // Ensure the home page's basic layout is applied before navigating to another page:
            setupFancyUI(currentHomePage);
        }

        setupFancyUI(nextPage, nextPage.hasPlayerInventory());
    }

    ///////////////////////////////////////////////
    // *********** PAGE SWITCHER ***********//

    /// ////////////////////////////////////////////

    protected void openPageSwitcher(ClickData clickData) {
        pageSwitcher.setPageList(allPages, currentHomePage);

        // If we're in another tab of the current page, ensure nav to its main tab when closing the page switcher:
        if (currentPage != currentHomePage && !previousPages.isEmpty()) {
            previousPages.pop();
        }

        this.sideTabsComponent.enabled(false);

        this.previousPages.push(new NavigationEntry(currentHomePage, currentHomePage, () -> {
            this.sideTabsComponent.enabled(true);
        }));

        this.currentPage = this.pageSwitcher;
        this.currentHomePage = this.pageSwitcher;

        setupFancyUI(this.pageSwitcher);
    }

    protected void switchPage(IFancyUIProvider nextHomePage) {
        // Ensure that the back button always leads back to the main page:
        this.currentHomePage = mainPage;
        this.currentPage = mainPage;
        this.previousPages.clear();

        sideTabsComponent.enabled(true);

        setupSideTabs(this.currentHomePage);
        navigate(nextHomePage, nextHomePage);
    }

    //////////////////////////////////////////////
    // *********** UI RENDERING ***********//

    /// ///////////////////////////////////////////

    protected void setupFancyUI(IFancyUIProvider fancyUI) {
        this.setupFancyUI(fancyUI, fancyUI.hasPlayerInventory());
    }

    protected void setupFancyUI(IFancyUIProvider fancyUI, boolean showInventory) {
        clearUI();

        UIAdapter<?> adapter = containerAccess().adapter();
        Size size = Size.zero();
        if (adapter != null) {
            size = Size.of(adapter.width(), adapter.height());
        }
        this.inflate(size);

        sideTabsComponent.selectTab(fancyUI);
        titleBar.updateState(
                currentHomePage,
                !this.previousPages.isEmpty(),
                this.allPages.size() > 1 && this.currentPage != this.pageSwitcher);

        size = this.calculateChildSpace(size);
        var page = fancyUI.createMainPage(this);
        page.inflate(size);

        // layout
        Sizing horizontal = page.horizontalSizing().get().copy().min(172);
        Sizing vertical = page.verticalSizing().get().copy().min(86);

        final var margins = this.margins.get();
        int width = horizontal.inflate(this.space.width() - margins.horizontal(), page::determineHorizontalContentSize);
        int height = vertical.inflate(this.space.height() - margins.vertical(),
                page::determineVerticalContentSize);

        int wholeGuiHeight = height + (!showInventory || playerInventory == null ? 0 : 76);
        this.sizing(Sizing.fixed(width + 20), Sizing.fixed(wholeGuiHeight + 16));
        this.background.sizing(Sizing.fixed(width), Sizing.fixed(wholeGuiHeight));

        AbstractContainerScreen<?> screen = containerAccess().screen();
        if (screen != null) {
            ((AbstractContainerScreenAccessor) screen).gtceu$setImageWidth(width);
            ((AbstractContainerScreenAccessor) screen).gtceu$setImageHeight(height);

            int leftPos = (screen.width - width) / 2;
            int topPos = (screen.height - height) / 2;
            ((AbstractContainerScreenAccessor) screen).gtceu$setLeftPos(leftPos);
            ((AbstractContainerScreenAccessor) screen).gtceu$setTopPos(topPos);
            containerAccess().adapter().leftPos(leftPos);
            containerAccess().adapter().topPos(topPos);

            containerAccess().adapter().moveAndResize(0, 0, screen.width, screen.height);
        }

        this.pageContainer.sizing(horizontal, vertical);
        this.tooltipsPanel.positioning(Positioning.absolute(width + 20 + 2, 2));

        setupInventoryPosition(showInventory);

        // setup
        this.pageContainer.child(page);
        fancyUI.attachConfigurators(configuratorPanel);
        configuratorPanel
                .positioning(Positioning.absolute(-(4 + 2), screen.height - configuratorPanel.height() - 4));
        fancyUI.attachTooltips(tooltipsPanel);

        sideTabsComponent.verticalSizing(Sizing.fixed(height));
        titleBar.horizontalSizing(Sizing.fixed(width));

        this.inflate(this.space);
    }

    private void setupInventoryPosition(boolean showInventory) {
        if (this.playerInventory == null)
            return;

        this.playerInventory.enabled(showInventory);
    }

    protected void clearUI() {
        this.pageContainer.clearChildren();
        this.configuratorPanel.clear();
        this.tooltipsPanel.clear();
    }

    @Override
    public void dispose() {
        super.dispose();
        clearUI();
    }

    protected void setupSideTabs(IFancyUIProvider currentHomePage) {
        this.sideTabsComponent.clearSubTabs();
        currentHomePage.attachSideTabs(sideTabsComponent);
    }
}
