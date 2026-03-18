package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.mui.factory.PosGuiData;
import com.gregtechceu.gtceu.api.mui.value.sync.PanelSyncManager;
import com.gregtechceu.gtceu.api.mui.widget.ParentWidget;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.client.mui.screen.UISettings;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.ItemHandlerHelper;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeTankMachine extends QuantumTankMachine {

    @Getter
    @SaveField
    private int mBPerCycle = 1000;
    @Getter
    @SaveField
    private int ticksPerCycle = 1;

    public CreativeTankMachine(BlockEntityCreationInfo info) {
        super(info, GTValues.MAX, 1);
    }

    protected FluidCache createCacheFluidHandler() {
        return new InfiniteCache(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) autoOutput.setTicksPerCycle(ticksPerCycle);
    }

    @Override
    public long getStoredAmount() {
        return (long) Math.ceil(1d * mBPerCycle / ticksPerCycle);
    }

    private InteractionResult updateStored(FluidStack fluid) {
        stored = new FluidStack(fluid, 1000);
        onFluidChanged();
        return InteractionResult.SUCCESS;
    }

    private void setTicksPerCycle(String value) {
        if (value.isEmpty()) return;
        ticksPerCycle = Integer.parseInt(value);
        onFluidChanged();
    }

    private void setmBPerCycle(String value) {
        if (value.isEmpty()) return;
        mBPerCycle = Integer.parseInt(value);
        onFluidChanged();
    }

    @Override
    public void saveToItem(@NotNull CompoundTag tag) {
        tag.putInt("mBPerCycle", mBPerCycle);
        tag.putInt("ticksPerCycle", ticksPerCycle);
    }

    @Override
    public void loadFromItem(@NotNull CompoundTag tag) {
        mBPerCycle = tag.getInt("mBPerCycle");
        ticksPerCycle = tag.getInt("ticksPerCycle");
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        var heldItem = player.getItemInHand(hand);
        if (hit.getDirection() == getFrontFacing() && !isRemote()) {
            // Clear fluid if empty + shift-rclick
            if (heldItem.isEmpty()) {
                if (player.isCrouching() && !stored.isEmpty()) {
                    return updateStored(FluidStack.EMPTY);
                }
                return InteractionResult.PASS;
            }

            // If no fluid set and held-item has fluid, set fluid
            if (stored.isEmpty()) {
                return FluidUtil.getFluidContained(heldItem)
                        .map(this::updateStored)
                        .orElse(InteractionResult.PASS);
            }

            // Need to make a fake source to fully fill held-item since our cache only allows mbPerTick extraction
            CustomFluidTank source = new CustomFluidTank(new FluidStack(stored, Integer.MAX_VALUE));
            ItemStack result = FluidUtil.tryFillContainer(heldItem, source, Integer.MAX_VALUE, player, true)
                    .getResult();
            if (!result.isEmpty() && heldItem.getCount() > 1) {
                ItemHandlerHelper.giveItemToPlayer(player, result);
                result = heldItem.copy();
                result.shrink(1);
            }

            if (!result.isEmpty()) {
                player.setItemInHand(hand, result);
                return InteractionResult.SUCCESS;
            } else {
                return FluidUtil.getFluidContained(heldItem)
                        .map(this::updateStored)
                        .orElse(InteractionResult.PASS);
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    // TODO
    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        super.buildMainUI(mainWidget, guiData, syncManager, settings);
    }

    private class InfiniteCache extends FluidCache {

        public InfiniteCache(MetaMachine holder) {
            super(holder);
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return stored;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!stored.isEmpty() && stored.isFluidEqual(resource)) return resource.getAmount();
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (!stored.isEmpty()) return new FluidStack(stored, mBPerCycle);
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (!stored.isEmpty() && stored.isFluidEqual(resource)) return new FluidStack(resource, mBPerCycle);
            return FluidStack.EMPTY;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return true;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 1000;
        }
    }
}
