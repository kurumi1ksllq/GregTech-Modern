package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.common.item.datacomponents.BindingData;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.core.mixins.EntityAccessor;
import com.gregtechceu.gtceu.data.item.GTDataComponents;
import com.gregtechceu.gtceu.utils.ResearchManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class DataItemBehavior implements IInteractionItem, IAddInformation {

    public static final DataItemBehavior INSTANCE = new DataItemBehavior();

    protected DataItemBehavior() {}

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level level,
                                                  Player player, InteractionHand usedHand) {
        if (player.isSecondaryUseActive()) {
            int permissionLevel = ((EntityAccessor) player).gtceu$getPermissionLevel();
            stack.set(GTDataComponents.BINDING_DATA, new BindingData(permissionLevel, player.getUUID()));

            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return IInteractionItem.super.use(stack, level, player, usedHand);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        ResearchManager.ResearchItem researchData = stack.get(GTDataComponents.RESEARCH_ITEM);
        if (researchData == null) {
            BlockPos pos = stack.get(GTDataComponents.DATA_COPY_POS);
            if (pos != null) {
                tooltipComponents.add(Component.translatable("gtceu.tooltip.proxy_bind",
                        makePosPart(pos.getX()), makePosPart(pos.getY()), makePosPart(pos.getZ())));
            }
        }
    }

    private static Component makePosPart(int coordinate) {
        return Component.literal(Integer.toString(coordinate)).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        Player player = context.getPlayer();

        ICoverable coverable = GTCapabilityHelper.getCoverable(level, pos, face);
        if (coverable != null &&
                coverable.getCoverAtSide(face) instanceof IDataStickInteractable interactable) {
            if (context.isSecondaryUseActive()) {
                if (itemStack.get(GTDataComponents.RESEARCH_ITEM) == null) {
                    return interactable.onDataStickShiftUse(player, itemStack);
                }
            } else {
                return interactable.onDataStickUse(player, itemStack);
            }
        }

        MetaMachine machine = MetaMachine.getMachine(level, pos);
        if (machine != null) {
            if (!MachineOwner.canOpenOwnerMachine(player, machine)) {
                return InteractionResult.FAIL;
            }
            if (machine instanceof IDataStickInteractable interactable) {
                if (context.isSecondaryUseActive()) {
                    if (!itemStack.has(GTDataComponents.RESEARCH_ITEM)) {
                        return interactable.onDataStickShiftUse(player, itemStack);
                    }
                } else {
                    return interactable.onDataStickUse(player, itemStack);
                }
            }
        }
        return InteractionResult.PASS;
    }
}
