package com.gregtechceu.gtceu.common.cover.detector;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.IEnergyInfoProvider;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.component.LongInputComponent;
import com.gregtechceu.gtceu.api.ui.component.ToggleButtonComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.ParentUIComponent;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.RedstoneUtil;

import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AdvancedEnergyDetectorCover extends EnergyDetectorCover implements IUICover {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AdvancedEnergyDetectorCover.class, DetectorCover.MANAGED_FIELD_HOLDER);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Persisted
    @Getter
    @Setter
    public long minValue, maxValue;
    @Persisted
    @Getter
    @Setter
    private int outputAmount;
    @Persisted
    @Getter
    private boolean usePercent;

    private LongInputComponent minValueInput;
    private LongInputComponent maxValueInput;

    public AdvancedEnergyDetectorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);

        final int DEFAULT_MIN_PERCENT = 33, DEFAULT_MAX_PERCENT = 66;

        this.minValue = DEFAULT_MIN_PERCENT;
        this.maxValue = DEFAULT_MAX_PERCENT;
        this.usePercent = true;
    }

    @Override
    protected void update() {
        if (coverHolder.getOffsetTimer() % 20 != 0) return;

        IEnergyInfoProvider energyInfoProvider = getEnergyInfoProvider();

        if (energyInfoProvider == null) {
            setRedstoneSignalOutput(outputAmount);
            return;
        }

        // TODO properly support values > MAX_LONG
        IEnergyInfoProvider.EnergyInfo energyInfo = energyInfoProvider.getEnergyInfo();
        long capacity = energyInfo.capacity().longValue();
        long stored = energyInfo.stored().longValue();

        if (usePercent) {
            if (capacity > 0) {
                float ratio = (float) stored / capacity;
                this.outputAmount = RedstoneUtil.computeLatchedRedstoneBetweenValues(ratio * 100, this.maxValue,
                        this.minValue, isInverted(), this.outputAmount);
            } else {
                this.outputAmount = isInverted() ? 0 : 15;
            }
        } else {
            this.outputAmount = RedstoneUtil.computeLatchedRedstoneBetweenValues(stored, this.maxValue, this.minValue,
                    isInverted(), this.outputAmount);
        }
        setRedstoneSignalOutput(outputAmount);
    }

    public void setUsePercent(boolean usePercent) {
        var wasPercent = this.usePercent;
        this.usePercent = usePercent;

        initializeMinMaxInputs(wasPercent);
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public void loadServerUI(Player player, UIContainerMenu<CoverBehavior> menu, CoverBehavior holder) {
        menu.addServerboundMessage(UpdateMinValue.class, msg -> setMinValue(msg.value()));
        menu.addServerboundMessage(UpdateMaxValue.class, msg -> setMaxValue(msg.value()));
        menu.addServerboundMessage(UpdateUsePercent.class, msg -> setUsePercent(msg.value()));
        menu.addServerboundMessage(UpdateInverted.class, msg -> setInverted(msg.value()));
    }

    @Override
    public ParentUIComponent createUIWidget(UIAdapter<StackLayout> adapter) {
        var menu = adapter.menu();

        StackLayout group = UIContainers.stack(Sizing.fixed(176), Sizing.fixed(105));
        group.child(UIComponents.label(Component.translatable("cover.advanced_energy_detector.label"))
                .positioning(Positioning.absolute(10, 5)));

        group.child(UIComponents.textArea(Sizing.fixed(25), Sizing.content(),
                Component.translatable("cover.advanced_energy_detector.min").getString())
                .positioning(Positioning.absolute(10, 55)));

        group.child(UIComponents.textArea(Sizing.fixed(25), Sizing.content(),
                Component.translatable("cover.advanced_energy_detector.max").getString())
                .positioning(Positioning.absolute(10, 80)));

        minValueInput = new LongInputComponent(Sizing.fixed(176 - 40 - 10), Sizing.fixed(20),
                this::getMinValue, value -> {
                    this.setMinValue(value);
                    menu.sendMessage(new UpdateMinValue(value));
                });
        minValueInput.positioning(Positioning.absolute(40, 50));
        maxValueInput = new LongInputComponent(Sizing.fixed(176 - 40 - 10), Sizing.fixed(20),
                this::getMaxValue, value -> {
                    this.setMaxValue(value);
                    menu.sendMessage(new UpdateMaxValue(value));
                });
        maxValueInput.positioning(Positioning.absolute(40, 75));
        initializeMinMaxInputs(usePercent);
        group.child(minValueInput);
        group.child(maxValueInput);

        // Invert Redstone Output Toggle:
        group.child(new ToggleButtonComponent(GuiTextures.INVERT_REDSTONE_BUTTON, this::isInverted, value -> {
            this.setInverted(value);
            menu.sendMessage(new UpdateInverted(value));
        }) {

            @Override
            public void update(float delta, int mouseX, int mouseY) {
                super.update(delta, mouseX, mouseY);
                tooltip(LangHandler.getMultiLang(
                        "cover.advanced_energy_detector.invert." + (pressed ? "enabled" : "disabled")));
            }
        }.positioning(Positioning.absolute(9, 20)).sizing(Sizing.fixed(20)));

        // Mode (EU / Percent) Toggle:
        group.child(new ToggleButtonComponent(GuiTextures.ENERGY_DETECTOR_COVER_MODE_BUTTON, this::isUsePercent,
                value -> {
                    this.setUsePercent(value);
                    menu.sendMessage(new UpdateUsePercent(value));
                }) {

            @Override
            public void update(float delta, int mouseX, int mouseY) {
                super.update(delta, mouseX, mouseY);
                tooltip(LangHandler.getMultiLang(
                        "cover.advanced_energy_detector.use_percent." + (pressed ? "enabled" : "disabled")));
            }
        }.positioning(Positioning.absolute(167 - 29, 20))
                .sizing(Sizing.fixed(20)));

        return group;
    }

    public record UpdateMinValue(long value) {}

    public record UpdateMaxValue(long value) {}

    public record UpdateUsePercent(boolean value) {}

    public record UpdateInverted(boolean value) {}

    private void initializeMinMaxInputs(boolean wasPercent) {
        if (GTCEu.isClientThread() || minValueInput == null || maxValueInput == null)
            return;

        long energyCapacity = getEnergyInfoProvider().getEnergyInfo().capacity().longValue();

        minValueInput.setMin(0L);
        maxValueInput.setMin(0L);

        if (usePercent) {
            // This needs to be before setting the maximum, because otherwise the value would be limited to 100 EU
            // before converting to percent.
            if (!wasPercent) {
                minValueInput.setValue(Math.max((long) (((double) minValue / energyCapacity) * 100), 100));

                minValueInput.setValue(GTMath.clamp((long) (((double) minValue / energyCapacity) * 100), 0, 100));
                maxValueInput.setValue(GTMath.clamp((long) (((double) maxValue / energyCapacity) * 100), 0, 100));
            }

            minValueInput.setMax(100L);
            maxValueInput.setMax(100L);
        } else {
            minValueInput.setMax(energyCapacity);
            maxValueInput.setMax(energyCapacity);

            // This needs to be after setting the maximum, because otherwise the converted value would be
            // limited to 100.
            if (wasPercent) {
                minValueInput.setValue(GTMath.clamp((long) ((minValue / 100.0) * energyCapacity), 0, energyCapacity));
                maxValueInput.setValue(GTMath.clamp((long) ((maxValue / 100.0) * energyCapacity), 0, energyCapacity));
            }
        }
    }
}
