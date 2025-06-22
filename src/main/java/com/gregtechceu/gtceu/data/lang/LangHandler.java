package com.gregtechceu.gtceu.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

import static com.gregtechceu.gtceu.data.lang.LangUtil.*;

public class LangHandler {

    /**
     *
     * Unformatted text must be in light gray - §7
     * Items must be in gold - §6
     * Fluids must be in blue - §9
     * Directions must be in yellow - §e
     * Disabled/Inactive/Errors must be in red - §c
     * Enabled/Active must be in green - §a
     * Potion effects must be in yellow - §e
     * Time must be in red - §c
     * Percentages must be in green - §a
     * Keys must be in all caps
     * Key combos must follow the format KEY1 + KEY2 (for example, SHIFT + R-CLICK)
     *
     */

    public static void init(RegistrateLangProvider provider) {
        AdvancementLang.init(provider);
        ArmorLang.init(provider);
        BlockLang.init(provider);
        CommandLang.init(provider);
        ConfigurationLang.init(provider);
        CoverLang.init(provider);
        GUILang.init(provider);
        HazardLang.init(provider);
        IntegrationLang.init(provider);
        ItemLang.init(provider);
        MachineLang.init(provider);
        MaterialLang.init(provider);
        RecipeLang.init(provider);
        SubtitleLang.init(provider);
        ToolLang.init(provider);
    }
}
