package com.cleanroommc.modularui;

import com.cleanroommc.modularui.screen.RichTooltip;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

@Config(id = ModularUI.MOD_ID)
public class ConfigHolder {

    public static ConfigHolder INSTANCE;
    private static final Object LOCK = new Object();

    public static void init() {
        synchronized (LOCK) {
            if (INSTANCE == null) {
                INSTANCE = Configuration.registerConfig(ConfigHolder.class, ConfigFormats.yaml()).getConfigInstance();
            }
        }
    }

    @Configurable
    public ClientConfigs client = new ClientConfigs();
    @Configurable
    public DeveloperConfigs dev = new DeveloperConfigs();


    public static class ClientConfigs {


        @Configurable
        public UIConfigs ui = new UIConfigs();

        public static class UIConfigs {

            @Configurable
            @Configurable.Comment({
                    "If progress bar should step in texture pixels or screen pixels. (Screen pixels are way smaller and therefore smoother)",
                    "Default: true"})
            public boolean smoothProgressBar = true;
            @Configurable
            @Configurable.Comment({"Duration of UI animations in ms.",
                    "Default: 100"})
            @Configurable.Range(min = 1, max = 500)
            public int animationTime = 100;
            @Configurable
            @Configurable.Comment({"Default tooltip position around the widget or its panel.",
                    "Default: VERTICAL"})
            public RichTooltip.Pos tooltipPos = RichTooltip.Pos.NEXT_TO_MOUSE;

            @Configurable
            @Configurable.Comment({"The default color to overlay onto Machine (and other) UIs.",
                    "#FFFFFF is no coloring (like GTCE) (default).",
                    "#D2DCFF is the classic blue from GT5."})
            @Configurable.StringPattern(value = "#[0-9a-fA-F]{1,6}")
            @Configurable.Gui.ColorValue
            public String defaultUIColor = "#FFFFFF";
            @Configurable
            @Configurable.Comment({
                    "If true, pressing the ESC key in a text field will restore the last text instead of confirming current one.",
                    "Default: fakse"})
            public boolean escRestoresLastText = false;
            @Configurable
            @Configurable.Comment({
                    "If true and not specified otherwise, screens will try to use the 'vanilla_dark' theme.",
                    "Default: false"})
            public boolean useDarkThemeByDefault = false;

            @Configurable
            @Configurable.Comment({"If true, vanilla tooltips will be replaced with MUI's RichTooltip",
                    "Default: false"})
            public boolean replaceVanillaTooltips = false;

            @Configurable
            public boolean enableTestOverlays = false;

            public int getDefaultUIColor() {
                return Long.decode(ConfigHolder.INSTANCE.client.ui.defaultUIColor).intValue() | 0xFF000000;
            }
        }
    }

    public static class DeveloperConfigs {

        @Configurable
        @Configurable.Comment({"Debug general events? (will print recipe conficts etc. to server's debug.log)",
                "Default: false"})
        public boolean debug = false;
        @Configurable
        @Configurable.Comment({"Debug UI? (Will draw widget outlines and widget information)", "Default: false"})
        public boolean debugUI = ModularUI.isDev();

        @Configurable
        public MuiConfigs mui = new MuiConfigs();

        public static class MuiConfigs {

            @Configurable
            @Configurable.Comment({"Color for outlining widgets in debug mode, in ARGB"})
            @Configurable.StringPattern(value = "#[0-9a-fA-F]{1,8}")
            @Configurable.Gui.ColorValue
            public String textColor = "#dcb42873";

            @Configurable
            @Configurable.Comment({"Color for outlining widgets in debug mode, in ARGB"})
            @Configurable.StringPattern(value = "#[0-9a-fA-F]{1,8}")
            @Configurable.Gui.ColorValue
            public String outlineColor = "#dcb42873";

            @Configurable
            @Configurable.Comment({"Color for cursor in debug mode, in ARGB"})
            @Configurable.StringPattern(value = "#[0-9a-fA-F]{1,8}")
            @Configurable.Gui.ColorValue
            public String cursorColor = "#ff4cAf50";

            @Configurable
            @Configurable.Comment({"Scale of debug text",
                    "Default: 0.8f"})
            @Configurable.DecimalRange(min = 0.1f, max = 10.0f)
            public float scale = 0.8f;

            @Configurable
            public boolean showHovered = true;
            @Configurable
            public boolean showPos = true;
            @Configurable
            public boolean showSize = true;
            @Configurable
            public boolean showWidgetTheme = true;
            @Configurable
            public boolean showExtra = true;
            @Configurable
            public boolean showOutline = true;

            @Configurable
            public boolean showParent = true;
            @Configurable
            public boolean showParentPos = true;
            @Configurable
            public boolean showParentSize = true;
            @Configurable
            public boolean showParentWidgetTheme = true;
            @Configurable
            public boolean showParentOutline = true;
        }
    }
}
