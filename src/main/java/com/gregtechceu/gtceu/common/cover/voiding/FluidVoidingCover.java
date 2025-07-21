package com.gregtechceu.gtceu.common.cover.voiding;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget;
import com.gregtechceu.gtceu.client.renderer.pipe.cover.CoverRenderer;
import com.gregtechceu.gtceu.client.renderer.pipe.cover.CoverRendererBuilder;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.common.cover.PumpCover;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FluidVoidingCover extends PumpCover {

    protected final NullFluidTank nullFluidTank = new NullFluidTank();

    @Persisted
    @Getter
    protected boolean isEnabled = false;

    public FluidVoidingCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide, 0);
        setWorkingEnabled(false);
    }

    @Override
    protected CoverRenderer buildRenderer() {
        return new CoverRendererBuilder(GTCEu.id("block/cover/overlay_fluid_voiding"), null).build();
    }

    @Override
    protected boolean isSubscriptionActive() {
        return isWorkingEnabled();
    }

    //////////////////////////////////////////////
    // *********** COVER LOGIC ***********//
    //////////////////////////////////////////////

    @Override
    public void update() {
        if (isWorkingEnabled && coverHolder.getOffsetTimer() % 20 == 0) {
            doTransferFluids();
        }
    }

    protected void doTransferFluids() {
        IFluidHandler myFluidHandler = coverHolder.getCapability(ForgeCapabilities.FLUID_HANDLER,
                attachedSide).resolve().orElse(null);
        if (myFluidHandler == null) {
            return;
        }
        GTTransferUtils.transferFluidsFiltered(myFluidHandler, nullFluidTank,
                getFilterHandler()::test);
        subscriptionHandler.updateSubscription();
    }

    public void setWorkingEnabled(boolean workingEnabled) {
        isWorkingEnabled = workingEnabled;
        subscriptionHandler.updateSubscription();
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        final var group = new WidgetGroup(0, 0, 176, 120);
        group.addWidget(new LabelWidget(10, 5, getUITitle()));

        group.addWidget(new ToggleButtonWidget(10, 20, 20, 20,
                GuiTextures.BUTTON_POWER, this::isWorkingEnabled, this::setWorkingEnabled));

        // group.addWidget(filterHandler.createFilterSlotUI(36, 21));
        group.addWidget(filterHandler.createFilterSlotUI(148, 91));
        group.addWidget(filterHandler.createFilterConfigUI(10, 50, 126, 60));

        buildAdditionalUI(group);

        return group;
    }

    @NotNull
    protected String getUITitle() {
        return "cover.fluid.voiding.title";
    }

    @Override
    public InteractionResult onSoftMalletClick(Player playerIn, InteractionHand hand, BlockHitResult hitResult) {
        if (!isRemote()) {
            setWorkingEnabled(!isWorkingEnabled);
            playerIn.sendSystemMessage(Component.translatable(isWorkingEnabled() ?
                    "cover.voiding.message.enabled" : "cover.voiding.message.disabled"));
        }
        return InteractionResult.sidedSuccess(playerIn.level().isClientSide);
    }

    // TODO: Decide grid behavior
    @Override
    public boolean shouldRenderGrid(Player player, BlockPos pos, BlockState state, ItemStack held,
                                    Set<GTToolType> toolTypes) {
        return super.shouldRenderGrid(player, pos, state, held, toolTypes);
    }

    @Override
    public @Nullable ResourceTexture sideTips(Player player, BlockPos pos, BlockState state, Set<GTToolType> toolTypes,
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

    class NullFluidTank extends FluidTank {

        public NullFluidTank() {
            super(Integer.MAX_VALUE);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (FluidVoidingCover.this.filterHandler.test(resource)) {
                return resource.getAmount();
            }
            return 0;
        }
    }
}
