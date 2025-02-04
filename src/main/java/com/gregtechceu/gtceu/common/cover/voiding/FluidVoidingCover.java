package com.gregtechceu.gtceu.common.cover.voiding;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.texture.ResourceTexture;
import com.gregtechceu.gtceu.common.cover.PumpCover;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FluidVoidingCover extends PumpCover {

    public FluidVoidingCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide, 0);
        setWorkingEnabled(false);
    }

    @Override
    protected boolean isSubscriptionActive() {
        return isWorkingEnabled();
    }

    //////////////////////////////////////////////
    // *********** COVER LOGIC ***********//
    //////////////////////////////////////////////

    @Override
    protected void update() {
        if (coverHolder.getOffsetTimer() % 5 != 0)
            return;

        doVoidFluids();
        subscriptionHandler.updateSubscription();
    }

    protected void doVoidFluids() {
        IFluidHandlerModifiable fluidHandler = getOwnFluidHandler();
        if (fluidHandler == null) {
            return;
        }
        voidAny(fluidHandler);
    }

    void voidAny(IFluidHandlerModifiable fluidHandler) {
        final Map<FluidStack, Integer> fluidAmounts = enumerateDistinctFluids(fluidHandler, TransferDirection.EXTRACT);

        for (FluidStack fluidStack : fluidAmounts.keySet()) {
            if (!filterHandler.test(fluidStack))
                continue;

            var toDrain = fluidStack.copy();
            toDrain.setAmount(fluidAmounts.get(fluidStack));

            fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public void setWorkingEnabled(boolean workingEnabled) {
        isWorkingEnabled = workingEnabled;
        subscriptionHandler.updateSubscription();
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public ParentUIComponent createUIWidget(UIAdapter<StackLayout> adapter) {
        final var group = UIContainers.stack(Sizing.fixed(176), Sizing.fixed(120));
        group.padding(Insets.both(10, 5));

        group.child(UIComponents.label(Component.translatable(getUITitle()))
                .positioning(Positioning.relative(0, 0)));

        group.child(UIComponents.toggleButton(GuiTextures.BUTTON_POWER, this::isWorkingEnabled, this::setWorkingEnabled)
                .positioning(Positioning.absolute(0, 15))
                .sizing(Sizing.fixed(20)));

        group.child(filterHandler.createFilterSlotUI(138, 86));
        group.child(filterHandler.createFilterConfigUI(0, 45, 126, 60, adapter));

        buildAdditionalUI(group);

        return group;
    }

    @NotNull
    protected String getUITitle() {
        return "cover.fluid.voiding.title";
    }

    protected void buildAdditionalUI(StackLayout group) {
        // Do nothing in the base implementation. This is intended to be overridden by subclasses.
    }

    protected void configureFilter() {
        // Do nothing in the base implementation. This is intended to be overridden by subclasses.
    }

    @Override
    public InteractionResult onSoftMalletClick(Player playerIn, InteractionHand hand, BlockHitResult hitResult) {
        if (!isClientSide()) {
            setWorkingEnabled(!isWorkingEnabled);
            playerIn.sendSystemMessage(Component.translatable(isWorkingEnabled() ?
                    "cover.voiding.message.enabled" : "cover.voiding.message.disabled"));
        }
        playerIn.swing(hand);
        return InteractionResult.CONSUME;
    }

    // TODO: Decide grid behavior
    @Override
    public boolean shouldRenderGrid(Player player, BlockPos pos, BlockState state, ItemStack held,
                                    Set<GTToolType> toolTypes) {
        return super.shouldRenderGrid(player, pos, state, held, toolTypes);
    }

    @Override
    public ResourceTexture sideTips(Player player, BlockPos pos, BlockState state, Set<GTToolType> toolTypes,
                                    Direction side) {
        if (toolTypes.contains(GTToolType.SOFT_MALLET)) {
            return isWorkingEnabled() ? GuiTextures.TOOL_START : GuiTextures.TOOL_PAUSE;
        }
        return super.sideTips(player, pos, state, toolTypes, side);
    }

    //////////////////////////////////////
    // ***** LDLib SyncData ******//
    //////////////////////////////////////

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidVoidingCover.class,
            PumpCover.MANAGED_FIELD_HOLDER);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }
}
