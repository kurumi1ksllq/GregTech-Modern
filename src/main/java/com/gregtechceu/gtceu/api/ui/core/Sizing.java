package com.gregtechceu.gtceu.api.ui.core;

import com.gregtechceu.gtceu.api.ui.parsing.UIModelParsingException;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.util.Mth;

import org.w3c.dom.Element;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

@Accessors(fluent = true, chain = true)
public class Sizing implements Animatable<Sizing> {

    private static final Sizing CONTENT_SIZING = new Sizing(0, Method.CONTENT);

    public final Method method;
    public final int value;

    @Setter
    public int min = -1;
    @Setter
    public int max = -1;

    private Sizing(int value, Method method) {
        this.method = method;
        this.value = value;
    }

    public Sizing copy() {
        return new Sizing(value, method).min(min).max(max);
    }

    /**
     * Inflate into the given space
     *
     * @param space               The available space
     * @param contentSizeFunction A function for making the component set the
     *                            size based on its content
     */
    public int inflate(int space, Function<Sizing, Integer> contentSizeFunction) {
        return switch (this.method) {
            case FIXED -> this.value;
            case FILL -> {
                int value = Math.round((this.value / 100f) * space);
                if (max != -1) {
                    value = Math.min(max, value);
                }
                if (min != -1) {
                    value = Math.max(min, value);
                }
                yield value;
            }
            case CONTENT -> {
                int value = contentSizeFunction.apply(this) + this.value * 2;
                if (max != -1) {
                    value = Math.min(max, value);
                }
                if (min != -1) {
                    value = Math.max(min, value);
                }
                yield value;
            }
        };
    }

    public static Sizing fixed(int value) {
        return new Sizing(value, Method.FIXED);
    }

    /**
     * Dynamically size the component based on its content,
     * without any padding
     */
    public static Sizing content() {
        return CONTENT_SIZING;
    }

    /**
     * Dynamically size the component based on its content
     *
     * @param padding Padding to add onto the size of the content
     */
    public static Sizing content(int padding) {
        return new Sizing(padding, Method.CONTENT);
    }

    /**
     * Dynamically size the component to fill the available space
     */
    public static Sizing fill() {
        return fill(100);
    }

    /**
     * Dynamically size the component based on the available space
     *
     * @param percent How many percent of the available space to take up
     */
    public static Sizing fill(int percent) {
        return new Sizing(percent, Method.FILL);
    }

    /**
     * @return {@code true} if this sizing instance
     *         uses the {@linkplain Method#CONTENT CONTENT} method
     */
    public boolean isContent() {
        return this.method == Method.CONTENT;
    }

    /**
     * The content factor of a sizing instance describes where
     * on the spectrum from content to fixed sizing it sits. Specifically, this is
     * used to lerp the reference frame used for calculating {@code fill(...)} sizing
     * on children between the available space in this component (content factor 0)
     * and this component's own available space (content factor 1), both of which can be
     * independently determined prior to layout calculations
     */
    public float contentFactor() {
        return this.isContent() ? 1f : 0f;
    }

    @Override
    public Sizing interpolate(Sizing next, float delta) {
        if (next.method != this.method) {
            return new MergedSizing(this, next, delta);
        } else {
            return new Sizing(Mth.lerpInt(delta, this.value, next.value), this.method);
        }
    }

    public Sizing andThen(Sizing next) {
        return new GroupedSizing(this, next);
    }

    public enum Method {
        FIXED,
        CONTENT,
        FILL
    }

    public static Sizing parse(Element sizingElement) {
        var methodString = sizingElement.getAttribute("method");
        if (methodString.isBlank()) {
            throw new UIModelParsingException(
                    "Missing 'method' attribute on sizing declaration. Must be one of: fixed, content, fill");
        }

        var method = Method.valueOf(methodString.toUpperCase(Locale.ROOT));
        var value = sizingElement.getTextContent().strip();

        var min = sizingElement.getAttribute("min").strip();
        if (!min.matches("^(\\d+)?$")) {
            throw new UIModelParsingException("Invalid min limit in sizing declaration");
        }
        var max = sizingElement.getAttribute("max").strip();
        if (!max.matches("^(\\d+)?$")) {
            throw new UIModelParsingException("Invalid max limit in sizing declaration");
        }

        if (method == Method.CONTENT) {
            if (!value.matches("^(-?\\d+)?$")) {
                throw new UIModelParsingException("Invalid value in sizing declaration");
            }

            return new Sizing(value.isEmpty() ? 0 : Integer.parseInt(value), method)
                    .min(min.isEmpty() ? -1 : Integer.parseInt(min))
                    .max(max.isEmpty() ? -1 : Integer.parseInt(max));
        } else {
            if (!value.matches("-?\\d+") && method != Method.FILL) {
                throw new UIModelParsingException("Invalid value in sizing declaration: " + value);
            } else if (method == Method.FILL) {
                return new Sizing(100, method);
            }

            return new Sizing(Integer.parseInt(value), method)
                    .min(min.isEmpty() ? -1 : Integer.parseInt(min))
                    .max(max.isEmpty() ? -1 : Integer.parseInt(max));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sizing sizing = (Sizing) o;
        return value == sizing.value && method == sizing.method &&
                min == sizing.min && max == sizing.max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, value, min, max);
    }

    private static class MergedSizing extends Sizing {

        private final Sizing first, second;
        private final float delta;

        private MergedSizing(Sizing first, Sizing second, float delta) {
            super(first.value, first.method);
            this.first = first;
            this.second = second;
            this.delta = delta;
        }

        @Override
        public Sizing copy() {
            return new MergedSizing(first.copy(), second.copy(), delta);
        }

        @Override
        public int inflate(int space, Function<Sizing, Integer> contentSizeFunction) {
            return Mth.lerpInt(
                    this.delta,
                    this.first.inflate(space, contentSizeFunction),
                    this.second.inflate(space, contentSizeFunction));
        }

        @Override
        public Sizing interpolate(Sizing next, float delta) {
            return this.first.interpolate(next, delta);
        }

        @Override
        public boolean isContent() {
            return this.first.isContent() || this.second.isContent();
        }

        @Override
        public float contentFactor() {
            if (this.first.isContent() && this.second.isContent()) return super.contentFactor();

            if (this.first.isContent()) {
                return 1f - delta;
            } else if (this.second.isContent()) {
                return delta;
            } else {
                return 0f;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            MergedSizing that = (MergedSizing) o;
            return Float.compare(delta, that.delta) == 0 && Objects.equals(first, that.first) &&
                    Objects.equals(second, that.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), first, second, delta);
        }
    }

    private static class GroupedSizing extends Sizing {

        private final Sizing first, second;

        private GroupedSizing(Sizing first, Sizing second) {
            super(first.value, first.method);
            this.first = first;
            this.second = second;
        }

        @Override
        public Sizing copy() {
            return new GroupedSizing(first.copy(), second.copy());
        }

        @Override
        public int inflate(int space, Function<Sizing, Integer> contentSizeFunction) {
            var value = this.first.inflate(space, contentSizeFunction) +
                    this.second.inflate(space, contentSizeFunction);
            if (max != -1) {
                value = Math.min(max, value);
            }
            if (min != -1) {
                value = Math.max(min, value);
            }
            return value;
        }

        @Override
        public Sizing interpolate(Sizing next, float delta) {
            return this.first.interpolate(next, delta);
        }

        @Override
        public boolean isContent() {
            return this.first.isContent() || this.second.isContent();
        }

        @Override
        public float contentFactor() {
            return (this.first.contentFactor() + this.second.contentFactor()) / 2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            MergedSizing that = (MergedSizing) o;
            return Objects.equals(first, that.first) &&
                    Objects.equals(second, that.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), first, second);
        }
    }
}
