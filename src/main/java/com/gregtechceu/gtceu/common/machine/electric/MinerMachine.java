package com.gregtechceu.gtceu.common.machine.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.IMiner;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.WorkableTieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataInfoProvider;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.component.ComponentPanelComponent;
import com.gregtechceu.gtceu.api.ui.component.SlotComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.*;
import com.gregtechceu.gtceu.api.ui.core.Insets;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.Surface;
import com.gregtechceu.gtceu.api.ui.editable.EditableMachineUI;
import com.gregtechceu.gtceu.api.ui.editable.EditableUI;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;
import com.gregtechceu.gtceu.api.ui.util.UIComponentUtils;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.common.item.PortableScannerBehavior;
import com.gregtechceu.gtceu.common.machine.trait.miner.MinerLogic;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MinerMachine extends WorkableTieredMachine
                          implements IMiner, IControllable, IFancyUIMachine, IDataInfoProvider {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MinerMachine.class,
            WorkableTieredMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Persisted
    protected final CustomItemStackHandler chargerInventory;
    private final long energyPerTick;
    @Nullable
    protected TickableSubscription autoOutputSubs, batterySubs;
    @Nullable
    protected ISubscription exportItemSubs, energySubs;

    public MinerMachine(IMachineBlockEntity holder, int tier, int speed, int maximumRadius, int fortune,
                        Object... args) {
        super(holder, tier, GTMachineUtils.defaultTankSizeFunction, args, (tier + 1) * (tier + 1), fortune, speed,
                maximumRadius);
        this.energyPerTick = GTValues.V[tier - 1];
        this.chargerInventory = createChargerItemHandler();
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    protected CustomItemStackHandler createChargerItemHandler(Object... args) {
        var handler = new CustomItemStackHandler();
        handler.setFilter(item -> GTCapabilityHelper.getElectricItem(item) != null ||
                (ConfigHolder.INSTANCE.compat.energy.nativeEUToFE &&
                        GTCapabilityHelper.getForgeEnergyItem(item) != null));
        return handler;
    }

    @Override
    protected NotifiableItemStackHandler createImportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, 0, IO.NONE);
    }

    @Override
    protected NotifiableItemStackHandler createExportItemHandler(Object... args) {
        if (args.length > 3 && args[args.length - 4] instanceof Integer invSize) {
            return new NotifiableItemStackHandler(this, invSize, IO.OUT, IO.BOTH);
        }
        throw new IllegalArgumentException(
                "MinerMachine need args [inventorySize, fortune, speed, maximumRadius] for initialization");
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        if (args.length > 2 && args[args.length - 3] instanceof Integer fortune &&
                args[args.length - 2] instanceof Integer speed && args[args.length - 1] instanceof Integer maxRadius) {
            return new MinerLogic(this, fortune, speed, maxRadius);
        }
        throw new IllegalArgumentException(
                "MinerMachine need args [inventorySize, fortune, speed, maximumRadius] for initialization");
    }

    @Override
    public void onMachineRemoved() {
        // Remove the miner pipes below this miner
        getRecipeLogic().onRemove();
        clearInventory(exportItems.storage);
        clearInventory(chargerInventory);
    }

    @Override
    public MinerLogic getRecipeLogic() {
        return (MinerLogic) super.getRecipeLogic();
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateAutoOutputSubscription();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            if (getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.getServer().tell(new TickTask(0, this::updateAutoOutputSubscription));
            }
            updateBatterySubscription();
            exportItemSubs = exportItems.addChangedListener(this::updateAutoOutputSubscription);
            energySubs = energyContainer.addChangedListener(this::updateBatterySubscription);
            chargerInventory.setOnContentsChanged(this::updateBatterySubscription);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (exportItemSubs != null) {
            exportItemSubs.unsubscribe();
            exportItemSubs = null;
        }

        if (energySubs != null) {
            energySubs.unsubscribe();
            energySubs = null;
        }
    }

    //////////////////////////////////////
    // ********** LOGIC **********//
    //////////////////////////////////////
    protected void updateAutoOutputSubscription() {
        var outputFacingItems = getFrontFacing();
        if (!exportItems.isEmpty() && GTTransferUtils.hasAdjacentItemHandler(getLevel(), getPos(), outputFacingItems)) {
            autoOutputSubs = subscribeServerTick(autoOutputSubs, this::autoOutput);
        } else if (autoOutputSubs != null) {
            autoOutputSubs.unsubscribe();
            autoOutputSubs = null;
        }
    }

    protected void updateBatterySubscription() {
        if (energyContainer.dischargeOrRechargeEnergyContainers(chargerInventory, 0, true)) {
            batterySubs = subscribeServerTick(batterySubs, this::chargeBattery);
        } else if (batterySubs != null) {
            batterySubs.unsubscribe();
            batterySubs = null;
        }
    }

    protected void autoOutput() {
        if (getOffsetTimer() % 5 == 0) {
            exportItems.exportToNearby(getFrontFacing());
        }
        updateAutoOutputSubscription();
    }

    protected void chargeBattery() {
        if (!energyContainer.dischargeOrRechargeEnergyContainers(chargerInventory, 0, false)) {
            updateBatterySubscription();
        }
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public void loadServerUI(Player player, UIContainerMenu<MetaMachine> menu, MetaMachine holder) {}

    public static BiFunction<ResourceLocation, Integer, EditableMachineUI> EDITABLE_UI_CREATOR = Util
            .memoize((path, inventorySize) -> new EditableMachineUI(path, () -> {
                FlowLayout template = createTemplate(inventorySize).createDefault();
                SlotComponent batterySlot = createBatterySlot().createDefault();
                batterySlot.positioning(Positioning.absolute(100, 10));
                StackLayout group = UIContainers.stack(Sizing.fill(), Sizing.fill());
                group.padding(Insets.both(6, 4));

                template.positioning(Positioning.relative(50, 50));
                group.child(template);
                group.child(batterySlot);
                return group;
            }, (template, adapter, machine) -> {
                if (machine instanceof MinerMachine minerMachine) {
                    createTemplate(inventorySize).setupUI(template, adapter, minerMachine);
                    createEnergyBar().setupUI(template, adapter, minerMachine);
                    createBatterySlot().setupUI(template, adapter, minerMachine);
                }
            }));

    protected static EditableUI<FlowLayout, MinerMachine> createTemplate(int inventorySize) {
        return new EditableUI<>("miner", FlowLayout.class, () -> {
            int rowSize = (int) Math.sqrt(inventorySize);
            int width = rowSize * 18 + 120;
            int height = Math.max(rowSize * 18, 80);
            FlowLayout group = UIContainers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(height));

            GridLayout slots = UIContainers.grid(Sizing.content(), Sizing.content(), rowSize, rowSize);
            slots.positioning(Positioning.absolute(120, (height - rowSize * 18) / 2));
            for (int y = 0; y < rowSize; y++) {
                for (int x = 0; x < rowSize; x++) {
                    int index = y * rowSize + x;
                    var slot = UIComponents.slot(index);
                    slot.id("slot." + index);
                    slots.child(slot, y, x);
                }
            }

            var componentPanel = UIComponents.componentPanel(list -> {});
            componentPanel.positioning(Positioning.absolute(4, 5));
            componentPanel.maxWidthLimit(110);
            componentPanel.id("component_panel");

            var container = UIContainers.verticalFlow(Sizing.fixed(117), Sizing.fill());
            container.child(UIContainers.verticalScroll(Sizing.fill(), Sizing.fill(), componentPanel)
                    .surface(Surface.UI_DISPLAY)
                    .margins(Insets.of(4)));
            container.surface(Surface.UI_BACKGROUND_INVERSE);
            group.child(container);
            group.child(slots);
            return group;
        }, (group, adapter, machine) -> {
            UIComponentUtils.componentByIdForEach(group, "^slot_[0-9]+$", SlotComponent.class, slot -> {
                var index = UIComponentUtils.componentIdIndex(slot);
                if (index >= 0 && index < machine.exportItems.getSlots()) {
                    slot.setSlot(machine.exportItems, index);
                    slot.canInsert(false);
                    slot.canExtract(true);
                }
            });
            UIComponentUtils.componentByIdForEach(group, "^component_panel$", ComponentPanelComponent.class, panel -> {
                panel.textSupplier(machine::addDisplayText);
            });
        });
    }

    /**
     * Create an energy bar widget.
     */
    protected static EditableUI<SlotComponent, MinerMachine> createBatterySlot() {
        return new EditableUI<>("battery_slot", SlotComponent.class, () -> {
            var slotWidget = UIComponents.slot(0);
            slotWidget.backgroundTexture(UITextures.group(GuiTextures.SLOT, GuiTextures.CHARGER_OVERLAY));
            return slotWidget;
        }, (slotWidget, adapter, machine) -> {
            slotWidget.setSlot(machine.chargerInventory, 0);
            slotWidget.canInsert(true);
            slotWidget.canExtract(true);
            slotWidget.tooltip(LangHandler.getMultiLang("gtceu.gui.charger_slot.tooltip",
                    GTValues.VNF[machine.getTier()], GTValues.VNF[machine.getTier()]));
        });
    }

    private void addDisplayText(@NotNull List<Component> textList) {
        int workingArea = IMiner.getWorkingArea(getRecipeLogic().getCurrentRadius());
        textList.add(Component.translatable("gtceu.machine.miner.startx", getRecipeLogic().getX()).append(" ")
                .append(Component.translatable("gtceu.machine.miner.minex", getRecipeLogic().getMineX())));
        textList.add(Component.translatable("gtceu.machine.miner.starty", getRecipeLogic().getY()).append(" ")
                .append(Component.translatable("gtceu.machine.miner.miney", getRecipeLogic().getMineY())));
        textList.add(Component.translatable("gtceu.machine.miner.startz", getRecipeLogic().getZ()).append(" ")
                .append(Component.translatable("gtceu.machine.miner.minez", getRecipeLogic().getMineZ())));
        textList.add(Component.translatable("gtceu.universal.tooltip.working_area", workingArea, workingArea));
        if (getRecipeLogic().isDone())
            textList.add(Component.translatable("gtceu.multiblock.large_miner.done")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)));
        else if (getRecipeLogic().isWorking())
            textList.add(Component.translatable("gtceu.multiblock.large_miner.working")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
        else if (!this.isWorkingEnabled())
            textList.add(Component.translatable("gtceu.multiblock.work_paused"));
        if (getRecipeLogic().isInventoryFull())
            textList.add(Component.translatable("gtceu.multiblock.large_miner.invfull")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        if (!drainInput(true))
            textList.add(Component.translatable("gtceu.multiblock.large_miner.needspower")
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
    }

    @Override
    public boolean drainInput(boolean simulate) {
        long resultEnergy = energyContainer.getEnergyStored() - energyPerTick;
        if (resultEnergy >= 0L && resultEnergy <= energyContainer.getEnergyCapacity()) {
            if (!simulate)
                energyContainer.removeEnergy(energyPerTick);
            return true;
        }
        return false;
    }

    //////////////////////////////////////
    // ******* Interaction *******//
    //////////////////////////////////////

    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        if (isRemote()) return InteractionResult.SUCCESS;

        if (!this.isActive()) {
            int currentRadius = getRecipeLogic().getCurrentRadius();
            if (currentRadius == 1)
                getRecipeLogic().setCurrentRadius(getRecipeLogic().getMaximumRadius());
            else if (playerIn.isShiftKeyDown())
                getRecipeLogic().setCurrentRadius(Math.max(1, Math.round(currentRadius / 2.0f)));
            else
                getRecipeLogic().setCurrentRadius(Math.max(1, currentRadius - 1));

            getRecipeLogic().resetArea(true);

            int workingArea = IMiner.getWorkingArea(getRecipeLogic().getCurrentRadius());
            playerIn.sendSystemMessage(
                    Component.translatable("gtceu.universal.tooltip.working_area", workingArea, workingArea));
        } else {
            playerIn.sendSystemMessage(Component.translatable("gtceu.multiblock.large_miner.errorradius"));
        }
        return InteractionResult.SUCCESS;
    }

    @NotNull
    @Override
    public List<Component> getDataInfo(PortableScannerBehavior.DisplayMode mode) {
        if (mode == PortableScannerBehavior.DisplayMode.SHOW_ALL ||
                mode == PortableScannerBehavior.DisplayMode.SHOW_MACHINE_INFO) {
            int workingArea = IMiner.getWorkingArea(getRecipeLogic().getCurrentRadius());
            return Collections.singletonList(
                    Component.translatable("gtceu.universal.tooltip.working_area", workingArea, workingArea));
        }
        return new ArrayList<>();
    }
}
