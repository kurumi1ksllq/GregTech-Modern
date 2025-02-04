package com.gregtechceu.gtceu.api.cover.filter;

import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.component.TextBoxComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.TagExprFilter;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import lombok.Getter;

import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * @author KilaBash
 * @date 2023/3/14
 * @implNote TagFilter
 */
public abstract class TagFilter<T, S extends Filter<T, S>> implements Filter<T, S> {

    private static final Pattern DOUBLE_WILDCARD = Pattern.compile("\\*{2,}");
    private static final Pattern DOUBLE_AND = Pattern.compile("&{2,}");
    private static final Pattern DOUBLE_OR = Pattern.compile("\\|{2,}");
    private static final Pattern DOUBLE_NOT = Pattern.compile("!{2,}");
    private static final Pattern DOUBLE_XOR = Pattern.compile("\\^{2,}");
    private static final Pattern DOUBLE_SPACE = Pattern.compile(" {2,}");

    @Getter
    protected String oreDictFilterExpression = "";

    protected Consumer<S> itemWriter = filter -> {};
    protected Consumer<S> onUpdated = filter -> itemWriter.accept(filter);

    protected TagExprFilter.TagExprParser.MatchExpr matchExpr = null;

    @OnlyIn(Dist.CLIENT)
    private TextBoxComponent textBox;

    protected TagFilter() {}

    @Override
    public boolean isBlank() {
        return oreDictFilterExpression.isBlank();
    }

    public CompoundTag saveFilter() {
        if (isBlank()) {
            return null;
        }
        var tag = new CompoundTag();
        tag.putString("oreDict", oreDictFilterExpression);
        return tag;
    }

    public void setOreDict(String oreDict) {
        /*
         * Moved the validation filtering here as the vanilla textbox doesn't do modification on input.
         * also makes the user experience better.
         * hopefully.
         * -screret
         */
        // remove all operators that are double
        oreDict = DOUBLE_WILDCARD.matcher(oreDict).replaceAll("*");
        oreDict = DOUBLE_AND.matcher(oreDict).replaceAll("&");
        oreDict = DOUBLE_OR.matcher(oreDict).replaceAll("|");
        oreDict = DOUBLE_NOT.matcher(oreDict).replaceAll("!");
        oreDict = DOUBLE_XOR.matcher(oreDict).replaceAll("^");
        oreDict = DOUBLE_SPACE.matcher(oreDict).replaceAll(" ");
        // move ( and ) so it doesn't create invalid expressions f.e. xxx (& yyy) => xxx & (yyy)
        // append or prepend ( and ) if the amount is not equal
        StringBuilder builder = new StringBuilder();
        int unclosed = 0;
        char last = ' ';
        for (int i = 0; i < oreDict.length(); i++) {
            char c = oreDict.charAt(i);
            if (c == ' ') {
                if (last != '(')
                    builder.append(" ");
                continue;
            }
            if (c == '(')
                unclosed++;
            else if (c == ')') {
                unclosed--;
                if (last == '&' || last == '|' || last == '^') {
                    int l = builder.lastIndexOf(" " + last);
                    int l2 = builder.lastIndexOf(String.valueOf(last));
                    builder.insert(l == l2 - 1 ? l : l2, ")");
                    continue;
                }
                if (i > 0 && builder.charAt(builder.length() - 1) == ' ') {
                    builder.deleteCharAt(builder.length() - 1);
                }
            } else if ((c == '&' || c == '|' || c == '^') && last == '(') {
                builder.deleteCharAt(builder.lastIndexOf("("));
                builder.append(c).append(" (");
                continue;
            }

            builder.append(c);
            last = c;
        }
        if (unclosed > 0) {
            builder.append(")".repeat(unclosed));
        } else if (unclosed < 0) {
            unclosed = -unclosed;
            for (int i = 0; i < unclosed; i++) {
                builder.insert(0, "(");
            }
        }
        oreDict = builder.toString();
        oreDict = oreDict.replaceAll(" {2,}", " ");
        textBox.text(oreDict);

        this.oreDictFilterExpression = oreDict;
        matchExpr = TagExprFilter.parseExpression(oreDictFilterExpression);
        onUpdated.accept((S) this);
    }

    public UIComponent openConfigurator(int x, int y, UIAdapter<StackLayout> adapter) {
        StackLayout group = UIContainers.stack(Sizing.fixed(18 * 3 + 25), Sizing.fixed(18 * 3));
        group.positioning(Positioning.absolute(x, y));

        group.child(UIComponents.texture(GuiTextures.INFO_ICON)
                .sizing(Sizing.fixed(20))
                .positioning(Positioning.absolute(0, 0))
                .tooltip(LangHandler.getMultiLang("cover.tag_filter.info")));

        this.textBox = UIComponents.textBox(Sizing.fixed(18 * 3 + 25))
                .textSupplier(() -> oreDictFilterExpression);
        textBox.verticalSizing(Sizing.fixed(12));
        textBox.onChanged().subscribe(this::setOreDict);
        textBox.setMaxLength(64);
        textBox.positioning(Positioning.absolute(0, 29));
        group.child(textBox);

        return group;
    }

    @Override
    public void setOnUpdated(Consumer<S> onUpdated) {
        this.onUpdated = filter -> {
            this.itemWriter.accept(filter);
            onUpdated.accept(filter);
        };
    }
}
