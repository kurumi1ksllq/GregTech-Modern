package com.gregtechceu.gtceu.api.recipe.ingredient;

import lombok.Getter;
import net.minecraft.util.StringRepresentable;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;

public enum LinkedIngredientLinkMode implements StringRepresentable {

    /**
     * The ingredient's roll is calculated directly from the linked ingredient(s)' roll.
     */
    LINK_DIRECT("direct"),
    /**
     * The ingredient's roll is calculated as the inverse of the linked ingredient(s)' roll.
     */
    LINK_INVERSE("inverse"),
    /**
     * The ingredient's roll is calculated directly from one of the linked ingredient's rolls. Only use if the linked
     * ingredients use {@link ChanceLogic#XOR}.
     */
    LINK_XOR("xor"),
    /**
     * The ingredient's roll is calculated as the inverse of one of the linked ingredient's rolls. Only use if the linked
     * ingredients use {@link ChanceLogic#XOR}.
     */
    LINK_XOR_INVERSE("xor-inverse"),
    /**
     * This ingredient is rolled independently. This is a fallback case and should not generally be used.
     */
    LINK_NONE("none");

    @Getter
    private final String serializedName;

    LinkedIngredientLinkMode(String name) {
        this.serializedName = name;
    }

    public static LinkedIngredientLinkMode getModeFromName(String name) {
        return switch (name.strip().toLowerCase()) {
            case "direct" -> LINK_DIRECT;
            case "inverse" -> LINK_INVERSE;
            case "xor" -> LINK_XOR;
            case "xor-inverse" -> LINK_XOR_INVERSE;
            default -> LINK_NONE;
        };
    }
}
