package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

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

import java.util.*;

public class TerminalBehavior implements IInteractionItem, IItemUIFactory {

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.CONSUME;
        if (player.isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos controllerPos = context.getClickedPos();
            if (MetaMachine.getMachine(level, controllerPos) instanceof IMultiController controller) {
                var pattern = controller.getPattern();
                var blockPlacedMap = pattern.autoBuildPlaceBlockMap;
                var blockFailedMap = pattern.autoBuildFailedBlockMap;
                var predicateInfoMap = pattern.infoPredicate;
                Map<PartAbility, Integer> predicatePartAbilityCount = new HashMap<>();
                Map<Block, Integer> predicateBlockCount = new HashMap<>();
                for (var entry : predicateInfoMap.entrySet()) {
                    for (var ability : entry.getValue().abilities) {
                        predicatePartAbilityCount.merge(ability, 1, Integer::sum);
                    }
                }
                for (var block : blockFailedMap.entrySet()) {
                    if (!predicateInfoMap.containsKey(block.getKey().getSecond())) {
                        predicateBlockCount.merge(block.getKey().getFirst(), 1, Integer::sum);
                    }
                }

                if (!controller.isFormed()) {
                    if (level.isClientSide) return InteractionResult.sidedSuccess(true);
                    controller.getPattern().autoBuild(player, controller.getMultiblockState());
                    if (blockPlacedMap.isEmpty()) {
                        if (!blockFailedMap.isEmpty()) {
                            player.displayClientMessage(
                                    Component.translatable("gtceu.tools.printer.error_no_blocks_placed")
                                            .withStyle(ChatFormatting.RED),
                                    false);
                            for (var ability : predicatePartAbilityCount.entrySet()) {
                                if (ability.getKey().getLangKey() != null && !ability.getKey().getLangKey().isEmpty()) {
                                    boolean plural = ability.getValue() > 1;
                                    var abilityComponent = Component
                                            .translatable(ability.getKey().getLangKey() + (plural ? "_plural" : ""));
                                    var numberComponent = plural ? Component.literal(ability.getValue().toString())
                                            .withStyle(ChatFormatting.AQUA) : ability.getValue();
                                    player.displayClientMessage(
                                            Component.translatable("gtceu.tools.printer_part_notice", abilityComponent,
                                                    numberComponent),
                                            false);

                                    /*
                                     * if (ability.getValue() > 1) {
                                     * player.displayClientMessage(Component.translatable(
                                     * "gtceu.tools.printer_part_notice",
                                     * Component.translatable(ability.getKey().getLangKey()+"_plural"), ", " +
                                     * Component.literal(ability.getValue().toString()).withStyle(ChatFormatting.AQUA)),
                                     * false);
                                     * } else {
                                     * player.displayClientMessage(Component.translatable(
                                     * "gtceu.tools.printer_part_notice",
                                     * Component.translatable(ability.getKey().getLangKey()), ", " +
                                     * ability.getValue()), false);
                                     * }
                                     */
                                }
                            }
                            for (var value : predicateBlockCount.entrySet()) {
                                var message = Component
                                        .literal(ChatFormatting.WHITE + value.getKey().getName().getString() +
                                                ChatFormatting.WHITE + ", " + ChatFormatting.AQUA + value.getValue());
                                player.displayClientMessage(message, false);
                            }
                        } else {
                            player.displayClientMessage(Component.translatable("gtceu.tools.printer.no_blocks_placed")
                                    .withStyle(ChatFormatting.RED), false);
                        }
                    } else {
                        player.displayClientMessage(Component.translatable("gtceu.tools.printer.building_structure")
                                .withStyle(ChatFormatting.GOLD), false);
                        for (var block : blockPlacedMap.entrySet()) {
                            var message = Component
                                    .literal(ChatFormatting.WHITE + block.getKey().getName().getString() +
                                            ChatFormatting.WHITE + ", " + ChatFormatting.AQUA + block.getValue());
                            player.displayClientMessage(message, false);
                        }
                    }
                    return InteractionResult.sidedSuccess(false);
                }
            }
        } else {
            Level level = context.getLevel();
            BlockPos blockPos = context.getClickedPos();
            if (MetaMachine.getMachine(level, blockPos) instanceof IMultiController controller) {
                if (!level.isClientSide) {
                    if (controller.isFormed()) {
                        player.displayClientMessage(Component.literal("Structure is formed with no issues.")
                                .withStyle(ChatFormatting.GREEN), false);
                    } else {
                        player.displayClientMessage(
                                Component.literal("Structure is incomplete!").withStyle(ChatFormatting.RED), false);
                    }
                }
            }
            return InteractionResult.CONSUME;
        }
        // Consume and Pass result in the UI opening regardless
        return InteractionResult.CONSUME;
    }

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player entityPlayer) {
        var container = new WidgetGroup(8, 8, 160, 54);
        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return new ModularUI(176, 120, holder, entityPlayer)
                .background(GuiTextures.BACKGROUND)
                .widget(new ButtonWidget(15, 24, 20, 20, new TextTexture("+"), (data) -> {
                    holder.markAsDirty();
                }));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack heldItem = player.getItemInHand(usedHand);
        if (player.isShiftKeyDown()) {
            return IItemUIFactory.super.use(item, level, player, usedHand);
        }
        return InteractionResultHolder.pass(heldItem);
    }

    public String formatBuildResult(Map<Block, Integer> builtBlockMap) {
        if (!builtBlockMap.isEmpty()) {
            String result = ChatFormatting.WHITE.toString() +
                    Component.translatable("gtceu.tools.printer.blocks_placed").toString() + '\n';
            for (var block : builtBlockMap.entrySet()) {
                result += "  " + ChatFormatting.WHITE.toString() + block.getKey().getName().getString() +
                        ChatFormatting.WHITE.toString() + ", " + ChatFormatting.AQUA.toString() + block.getValue() +
                        '\n';
            }
            return result;
        }
        String result = ChatFormatting.WHITE.toString() +
                Component.translatable("gtceu.tools.printer.no_blocks_placed");
        return result;
    }
}
