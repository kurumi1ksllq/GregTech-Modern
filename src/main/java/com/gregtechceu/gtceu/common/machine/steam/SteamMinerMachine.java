package com.gregtechceu.gtceu.common.machine.steam;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.IMiner;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.*;
import com.gregtechceu.gtceu.api.machine.steam.SteamWorkableMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.component.PredicatedTextureComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.GridLayout;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.common.item.PortableScannerBehavior;
import com.gregtechceu.gtceu.common.machine.trait.miner.SteamMinerLogic;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SteamMinerMachine extends SteamWorkableMachine implements IMiner, IControllable, IExhaustVentMachine,
                               IUIMachine, IMachineLife, IDataInfoProvider {

    @Getter
    @Setter
    @Persisted
    @DescSynced
    private boolean needsVenting;
    @Persisted
    public final NotifiableItemStackHandler importItems;
    @Persisted
    public final NotifiableItemStackHandler exportItems;
    private final int inventorySize;
    private final int energyPerTick;
    @Nullable
    protected TickableSubscription autoOutputSubs;
    @Nullable
    protected ISubscription exportItemSubs;

    public SteamMinerMachine(IMachineBlockEntity holder, boolean isHighPressure, int speed, int maximumRadius,
                             int fortune, int energyPerTick) {
        super(holder, isHighPressure, fortune, speed, maximumRadius);
        this.inventorySize = 4;
        this.energyPerTick = energyPerTick;
        this.importItems = createImportItemHandler();
        this.exportItems = createExportItemHandler();
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    @Override
    protected @NotNull RecipeLogic createRecipeLogic(Object... args) {
        if (args.length > 2 && args[args.length - 3] instanceof Integer fortune &&
                args[args.length - 2] instanceof Integer speed && args[args.length - 1] instanceof Integer maxRadius) {
            return new SteamMinerLogic(this, fortune, speed, maxRadius);
        }
        throw new IllegalArgumentException(
                "MinerMachine need args [fortune, speed, maximumRadius] for initialization");
    }

    @Override
    public SteamMinerLogic getRecipeLogic() {
        return (SteamMinerLogic) super.getRecipeLogic();
    }

    @Override
    protected NotifiableFluidTank createSteamTank(Object... args) {
        return new NotifiableFluidTank(this, 1, 16 * FluidType.BUCKET_VOLUME, IO.IN);
    }

    protected NotifiableItemStackHandler createImportItemHandler(@SuppressWarnings("unused") Object... args) {
        return new NotifiableItemStackHandler(this, 0, IO.IN);
    }

    protected NotifiableItemStackHandler createExportItemHandler(@SuppressWarnings("unused") Object... args) {
        return new NotifiableItemStackHandler(this, inventorySize, IO.OUT);
    }

    @Override
    public void onMachineRemoved() {
        getRecipeLogic().onRemove();
        clearInventory(exportItems.storage);
    }

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        updateAutoOutputSubscription();
        getRecipeLogic().updateTickSubscription();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            if (getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.getServer().tell(new TickTask(0, this::updateAutoOutputSubscription));
            }
            exportItemSubs = exportItems.addChangedListener(this::updateAutoOutputSubscription);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (exportItemSubs != null) {
            exportItemSubs.unsubscribe();
            exportItemSubs = null;
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

    protected void autoOutput() {
        if (getOffsetTimer() % 5 == 0) {
            exportItems.exportToNearby(getFrontFacing());
        }
        updateAutoOutputSubscription();
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////
    @Override
    public void loadServerUI(Player player, UIContainerMenu<MetaMachine> menu, MetaMachine holder) {
        // TODO implement
    }

    @Override
    public void loadClientUI(Player player, UIAdapter<StackLayout> adapter, MetaMachine holder) {
        int rowSize = (int) Math.sqrt(inventorySize);

        FlowLayout group = UIContainers.horizontalFlow(Sizing.fixed(175), Sizing.fixed(176));
        group.surface(Surface.UI_BACKGROUND_BRONZE);
        group.child(UIComponents.playerInventory(player.getInventory(), GuiTextures.SLOT_STEAM.get(isHighPressure()))
                .positioning(Positioning.absolute(7, 94)));

        GridLayout grid = UIContainers.grid(Sizing.content(), Sizing.content(), rowSize, rowSize);
        grid.positioning(Positioning.absolute(142 - rowSize * 9, 18));
        for (int y = 0; y < rowSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                int index = y * rowSize + x;
                grid.child(UIComponents.slot(exportItems, index)
                        .canInsert(false)
                        .canExtract(true)
                        .backgroundTexture(GuiTextures.SLOT_STEAM.get(isHighPressure())),
                        y, x);
            }
        }
        group.child(grid)
                .child(UIComponents.label(getBlockState().getBlock().getName())
                        .positioning(Positioning.absolute(5, 5)))
                .child(new PredicatedTextureComponent(GuiTextures.INDICATOR_NO_STEAM.get(isHighPressure()))
                        .predicate(recipeLogic::isWaiting)
                        .positioning(Positioning.absolute(79, 42))
                        .sizing(Sizing.fixed(18)))
                .child(UIContainers.verticalFlow(Sizing.fixed(105), Sizing.fixed(75))
                        .child(UIComponents.componentPanel(this::addDisplayText)
                                .maxWidthLimit(84))
                        .child(UIComponents.componentPanel(this::addDisplayText2)
                                .maxWidthLimit(84))
                        .padding(Insets.of(3))
                        .positioning(Positioning.absolute(7, 16)));

        adapter.rootComponent.child(group);
    }

    void addDisplayText(List<Component> textList) {
        int workingArea = IMiner.getWorkingArea(getRecipeLogic().getCurrentRadius());
        textList.add(Component.translatable("gtceu.machine.miner.startx", this.getRecipeLogic().getX()));
        textList.add(Component.translatable("gtceu.machine.miner.starty", this.getRecipeLogic().getY()));
        textList.add(Component.translatable("gtceu.machine.miner.startz", this.getRecipeLogic().getZ()));
        textList.add(Component.translatable("gtceu.universal.tooltip.working_area", workingArea, workingArea));
        if (this.getRecipeLogic().isDone())
            textList.add(Component.translatable("gtceu.multiblock.large_miner.done")
                    .withStyle(ChatFormatting.GREEN));
        else if (this.getRecipeLogic().isWorking())
            textList.add(Component.translatable("gtceu.multiblock.large_miner.working")
                    .withStyle(ChatFormatting.GOLD));
        else if (!this.isWorkingEnabled())
            textList.add(Component.translatable("gtceu.multiblock.work_paused"));
        if (getRecipeLogic().isInventoryFull())
            textList.add(Component.translatable("gtceu.multiblock.large_miner.invfull")
                    .withStyle(ChatFormatting.RED));
        if (isVentingBlocked())
            textList.add(Component.translatable("gtceu.multiblock.large_miner.vent")
                    .withStyle(ChatFormatting.RED));
        else if (!drainInput(true))
            textList.add(Component.translatable("gtceu.multiblock.large_miner.steam")
                    .withStyle(ChatFormatting.RED));
    }

    void addDisplayText2(List<Component> textList) {
        textList.add(Component.translatable("gtceu.machine.miner.minex", this.getRecipeLogic().getMineX()));
        textList.add(Component.translatable("gtceu.machine.miner.miney", this.getRecipeLogic().getMineY()));
        textList.add(Component.translatable("gtceu.machine.miner.minez", this.getRecipeLogic().getMineZ()));
    }

    public boolean drainInput(boolean simulate) {
        long resultSteam = steamTank.getFluidInTank(0).getAmount() - energyPerTick;
        if (!this.isVentingBlocked() && resultSteam >= 0L && resultSteam <= steamTank.getTankCapacity(0)) {
            if (!simulate)
                steamTank.drainInternal(energyPerTick, IFluidHandler.FluidAction.EXECUTE);
            return true;
        }
        return false;
    }

    @Override
    public @NotNull Direction getVentingDirection() {
        return Direction.UP;
    }

    @Override
    public void markVentingComplete() {
        this.needsVenting = false;
    }

    @Override
    public float getVentingDamage() {
        return isHighPressure() ? 12F : 6F;
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
