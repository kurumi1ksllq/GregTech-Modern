package com.cleanroommc.modularui.overlay;

import com.cleanroommc.modularui.ConfigHolder;
import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.base.IMuiScreen;
import com.cleanroommc.modularui.base.drawable.IIcon;
import com.cleanroommc.modularui.base.drawable.IKey;
import com.cleanroommc.modularui.base.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.NamedDrawableRow;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.TreeUtil;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.menu.ContextMenuButton;
import com.cleanroommc.modularui.widgets.menu.Menu;

import org.jetbrains.annotations.NotNull;

public class DebugOverlay extends CustomModularScreen {

    private static final IIcon CHECKMARK = GuiTextures.CHECK_BOX.asIcon().size(8);

    private final IMuiScreen parent;

    public DebugOverlay(IMuiScreen screen) {
        super(ModularUI.MOD_ID);
        this.parent = screen;
    }

    @Override
    public @NotNull ModularPanel buildUI(ModularGuiContext context) {
        return new ModularPanel("debug")
                .fullScreenInvisible()
                .child(new ContextMenuButton<>("menu_debug_options")
                        .horizontalCenter()
                        .bottom(0)
                        .height(12)
                        .width(160)
                        .background(
                                new Rectangle()
                                        .color(Color.withAlpha(
                                                Long.decode(ConfigHolder.INSTANCE.dev.mui.outlineColor).intValue(),
                                                0.4f)))
                        .disableHoverBackground()
                        .overlay(IKey.str("Debug Options"))
                        .openUp()
                        .menuList(l1 -> l1
                                .name("menu_list")
                                .maxSize(100)
                                .widthRel(1f)
                                .child(new ButtonWidget<>().name("print_widget_tree_button")
                                        .height(12)
                                        .widthRel(1f)
                                        .invisible()
                                        .overlay(IKey.str("Print widget trees"))
                                        .onMousePressed((x, y, b) -> this.logWidgetTrees(b)))
                                .child(new ButtonWidget<>().name("print_resizer_tree_button")
                                        .height(12)
                                        .widthRel(1f)
                                        .invisible()
                                        .overlay(IKey.str("Print resizer tree"))
                                        .onMousePressed((x, y, b) -> {
                                            TreeUtil.print(parent.screen().getResizeNode());
                                            return true;
                                        }))
                                .child(new ContextMenuButton<>("menu_hover_info")
                                        .height(10)
                                        .widthRel(1f)
                                        .overlay(IKey.str("Widget hover info"))
                                        .openRightUp()
                                        .menu(new Menu<>()
                                                .width(100)
                                                .coverChildrenHeight()
                                                .padding(2)
                                                .child(new ListWidget<>()
                                                        .maxSize(100)
                                                        .widthRel(1f)
                                                        .child(toggleOption(0, "Any",
                                                                ConfigHolder.INSTANCE.dev.mui.showHovered))
                                                        .child(toggleOption(1, "Pos",
                                                                ConfigHolder.INSTANCE.dev.mui.showPos))
                                                        .child(toggleOption(2, "Size",
                                                                ConfigHolder.INSTANCE.dev.mui.showSize))
                                                        .child(toggleOption(3, "Widget Theme",
                                                                ConfigHolder.INSTANCE.dev.mui.showWidgetTheme))
                                                        .child(toggleOption(4, "Extra info",
                                                                ConfigHolder.INSTANCE.dev.mui.showExtra))
                                                        .child(toggleOption(5, "Outline",
                                                                ConfigHolder.INSTANCE.dev.mui.showOutline)))))
                                .child(new ContextMenuButton<>("menu_parent_hover_info")
                                        .name("menu_button_parent_hover_info")
                                        .height(10)
                                        .widthRel(1f)
                                        .overlay(IKey.str("Parent widget hover info"))
                                        .openRightUp()
                                        .menu(new Menu<>()
                                                .width(100)
                                                .coverChildrenHeight()
                                                .padding(2)
                                                .child(new ListWidget<>()
                                                        .maxSize(100)
                                                        .widthRel(1f)
                                                        .child(toggleOption(10, "Any",
                                                                ConfigHolder.INSTANCE.dev.mui.showParent))
                                                        .child(toggleOption(11, "Pos",
                                                                ConfigHolder.INSTANCE.dev.mui.showParentPos))
                                                        .child(toggleOption(12, "Size",
                                                                ConfigHolder.INSTANCE.dev.mui.showParentSize))
                                                        .child(toggleOption(13, "Widget Theme",
                                                                ConfigHolder.INSTANCE.dev.mui.showParentWidgetTheme))
                                                        .child(toggleOption(14, "Outline",
                                                                ConfigHolder.INSTANCE.dev.mui.showParentOutline)))))));
    }

    public static IWidget toggleOption(int i, String name, boolean boolValue) {
        return new ToggleButton()
                .name("hover_info_toggle" + i)
                .invisible()
                .widthRel(1f)
                .height(12)
                .value(new BoolValue(boolValue))
                .overlay(true, new NamedDrawableRow()
                        .name(IKey.str(name))
                        .drawable(CHECKMARK))
                .overlay(false, new NamedDrawableRow()
                        .name(IKey.str(name)));
    }

    private boolean logWidgetTrees(int b) {
        for (ModularPanel panel : parent.screen().getPanelManager().getOpenPanels()) {
            WidgetTree.print(panel);
        }
        return true;
    }
}
