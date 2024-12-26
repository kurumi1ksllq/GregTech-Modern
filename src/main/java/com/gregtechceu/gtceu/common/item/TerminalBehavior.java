package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

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

import java.util.HashMap;
import java.util.Map;

public class TerminalBehavior implements IInteractionItem {

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos blockPos = context.getClickedPos();

            if (context.getPlayer() != null &&
                    MetaMachine.getMachine(level, blockPos) instanceof IMultiController controller) {
                Map<Block, Integer> blockMap = controller.getPattern().autoBuildBlockMap;

                if (!controller.isFormed()) {
                    if (!level.isClientSide) {
                        controller.getPattern().autoBuild(context.getPlayer(), controller.getMultiblockState());
                        if (blockMap.isEmpty()){
                            context.getPlayer().displayClientMessage(Component.translatable("gtceu.tools.printer.no_blocks_placed").withStyle(ChatFormatting.RED), false);
                        } else {
                            context.getPlayer().displayClientMessage(Component.translatable("gtceu.tools.printer.building_structure").withStyle(ChatFormatting.GOLD), false);
                            for (var block : blockMap.entrySet()) {
                                context.getPlayer().displayClientMessage(Component.literal(ChatFormatting.WHITE.toString() + block.getKey().getName().getString() + ChatFormatting.WHITE.toString() + ", " + ChatFormatting.AQUA.toString() + block.getValue()).withStyle(ChatFormatting.DARK_RED), false);
                            }
                        }
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
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
