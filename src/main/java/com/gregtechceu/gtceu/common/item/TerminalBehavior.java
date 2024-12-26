package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.IEnergyInfoProvider;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.predicates.PredicateAbilities;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TerminalBehavior implements IInteractionItem {

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos controllerPos = context.getClickedPos();
            if (context.getPlayer() != null && MetaMachine.getMachine(level, controllerPos) instanceof IMultiController controller) {
                Map<Block, Integer> blockPlacedMap = controller.getPattern().autoBuildPlaceBlockMap;
                Map<Pair<Block, BlockPos>, Integer> blockFailedMap = controller.getPattern().autoBuildFailedBlockMap;
                Map<BlockPos, PredicateAbilities> predicateInfoMap = controller.getPattern().infoPredicate;
                if (!controller.isFormed()) {
                    if (!level.isClientSide) {
                        controller.getPattern().autoBuild(context.getPlayer(), controller.getMultiblockState());
                        if (blockPlacedMap.isEmpty()){
                            if (!blockFailedMap.isEmpty()){
                                context.getPlayer().displayClientMessage(Component.translatable("gtceu.tools.printer.error_no_blocks_placed").withStyle(ChatFormatting.RED), false);
                                for (var block : blockFailedMap.entrySet()) {
                                    var blockPos = block.getKey().getSecond();
                                    if(predicateInfoMap.containsKey(blockPos)){
                                        for( var ability : predicateInfoMap.get(blockPos).abilities){
                                            if (!ability.getLangKey().isEmpty()) {
                                                if (block.getValue() > 1) {
                                                    context.getPlayer().displayClientMessage(Component.translatable("gtceu.tools.printer_part_notice", "s, " + ChatFormatting.AQUA.toString() + block.getValue()),false);
                                                } else {
                                                    context.getPlayer().displayClientMessage(Component.translatable("gtceu.tools.printer_part_notice", Component.translatable(ability.getLangKey()),", "+ block.getValue()),false);
                                                }
                                            }
                                        }
                                    } else {
                                        context.getPlayer().displayClientMessage(Component.literal(ChatFormatting.WHITE.toString() + block.getKey().getFirst().getName().getString() + ChatFormatting.WHITE.toString() + ", " + ChatFormatting.AQUA.toString() + block.getValue()), false);
                                    }
                                }
                            } else {
                                context.getPlayer().displayClientMessage(Component.translatable("gtceu.tools.printer.no_blocks_placed").withStyle(ChatFormatting.RED), false);
                            }
                        } else {
                            context.getPlayer().displayClientMessage(Component.translatable("gtceu.tools.printer.building_structure").withStyle(ChatFormatting.GOLD), false);
                            for (var block : blockPlacedMap.entrySet()) {
                                context.getPlayer().displayClientMessage(Component.literal(ChatFormatting.WHITE.toString() + block.getKey().getName().getString() + ChatFormatting.WHITE.toString() + ", " + ChatFormatting.AQUA.toString() + block.getValue()), false);
                            }
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        if (context.getPlayer() != null && !context.getPlayer().isShiftKeyDown()){
            Level level = context.getLevel();
            BlockPos blockPos = context.getClickedPos();
            if (context.getPlayer() != null && MetaMachine.getMachine(level, blockPos) instanceof IMultiController controller) {
                if (!level.isClientSide) {
                    if (controller.isFormed()){
                        context.getPlayer().displayClientMessage(Component.literal("Structure is formed with no issues.").withStyle(ChatFormatting.GREEN), false);
                    } else {
                        context.getPlayer().displayClientMessage(Component.literal("Structure is incomplete!").withStyle(ChatFormatting.RED), false);
                    }
                }
            }
            return InteractionResult.CONSUME;
        }
        //Consume and Pass result in the UI opening regardless
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack heldItem = player.getItemInHand(usedHand);
        return InteractionResultHolder.pass(heldItem);
    }

    public String formatBuildResult(Map<Block, Integer> builtBlockMap) {
        if (!builtBlockMap.isEmpty()) {
            String result = ChatFormatting.WHITE.toString() + Component.translatable("gtceu.tools.printer.blocks_placed").toString() + '\n';
            for (var block : builtBlockMap.entrySet()) {
                result += "  " + ChatFormatting.WHITE.toString() + block.getKey().getName().getString() + ChatFormatting.WHITE.toString() + ", " + ChatFormatting.AQUA.toString() + block.getValue() + '\n';
            }
            return result;
        }
        String result = ChatFormatting.WHITE.toString() + Component.translatable("gtceu.tools.printer.no_blocks_placed");
        return result;
    }
}
