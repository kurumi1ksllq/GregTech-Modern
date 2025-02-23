package com.gregtechceu.gtceu.common.item.debug;

import com.google.common.base.Joiner;
import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import net.minecraft.world.level.Level;

public class StructureWriterBehaviour implements IItemUIFactory {

    public static final StructureWriterBehaviour INSTANCE = new StructureWriterBehaviour();

    public static final BlockPos MAX = new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    public static final BlockPos MIN = new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

    @Getter
    private static BlockPos min = MAX;
    @Getter
    private static BlockPos max = MIN;

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player entityPlayer) {

        var container = new WidgetGroup(8, 8, 154, 160);
        container.addWidget(new ImageWidget(4, 4, 152, 46, GuiTextures.DISPLAY));
        container.addWidget(new LabelWidget(7, 7, () -> {
            var p = getMinMax(holder.getHeld());
            BlockPos min = MAX;
            BlockPos max = MIN;
            if (!p.getFirst().equals(MIN) && !p.getSecond().equals(MAX)) {
                min = p.getFirst();
                max = p.getSecond();
                return LocalizationUtils.format("behaviour.structure_writer.gui_pos",
                        min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
            }
            return LocalizationUtils.format("behaviour.structure_writer.gui_pos_not_set");
        }).setTextColor(0xFAF9F6));
        container.addWidget(new LabelWidget(7, 20, () -> {
            var dir = getDirection(holder.getHeld());
            var dirs = BlockPatternPrinter.getDirection(dir);
            return LocalizationUtils.format("behaviour.structure_writer.export_order",
                    dirs[0].name(), dirs[1].name(), dirs[2].name());
        }).setTextColor(0xFAF9F6));

        return new ModularUI(176, 142, holder, entityPlayer)
                .background(GuiTextures.BACKGROUND)
                .widget(container)
                .widget(new ButtonWidget(9, 68, 158, 20,
                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("behaviour.structure_writer.export_to_chat")),
                        data -> export(holder)))
                .widget(new ButtonWidget(9, 91, 77, 20,
                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("behaviour.structure_writer.rotate_x")),
                        data -> {
                            Direction dir = getDirection(holder.getHeld());
                            setDirection(holder.getHeld(), dir.getClockWise(Direction.Axis.X));
                }))
                .widget(new ButtonWidget(90, 91, 77, 20,
                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("behaviour.structure_writer.rotate_y")),
                        data -> {
                            Direction dir = getDirection(holder.getHeld());
                            setDirection(holder.getHeld(), dir.getClockWise(Direction.Axis.Y));
                }))
                .widget(new ButtonWidget(9, 114, 158, 20,
                        new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("behaviour.structure_writer.clear_selection")),
                        data -> {
                    clearPos(holder.getHeld(), holder.getPlayer());

                }));
    }

    private void export(HeldItemUIFactory.HeldItemHolder holder) {
        var p = getMinMax(holder.getHeld());
        BlockPos min = MAX;
        BlockPos max = MIN;
        if(!p.getFirst().equals(MIN) && !p.getSecond().equals(MAX)) {
            min = p.getFirst();
            max = p.getSecond();
            Direction dir = getDirection(holder.getHeld());
            StringBuilder builder = new StringBuilder();

            BlockPatternPrinter print = new BlockPatternPrinter(holder.getPlayer().level(), min, max);
            var dirs = BlockPatternPrinter.getDirection(dir);
            print.changeDir(dirs[0], dirs[1], dirs[2]);

            builder.append("FactoryBlockPattern.start(%s, %s, %s)\n".formatted(dirs[0], dirs[1], dirs[2]));
            for(int i = 0; i < print.pattern.length; i++) {
                String[] s = print.pattern[i];
                builder.append(".aisle(\"%s\")\n".formatted(Joiner.on("\", \"").join(s)));
            }

            for(var c : print.symbolMap.entrySet()) {
                builder.append(".where('%s', %s)\n".formatted(c.getKey(), c.getValue()));
            }

            GTCEu.LOGGER.info('\n' + builder.toString());
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        Player p = context.getPlayer();
        if (p == null) return InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();
        if (p.isShiftKeyDown()) {
            var look = p.getLookAngle().multiply(3, 3, 3).add(p.getEyePosition());

            BlockPos lookAt = new BlockPos((int) look.x, (int) look.y, (int) look.z);
            addPos(stack, lookAt, p);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide) return IItemUIFactory.super.use(item, level, player, usedHand);
        ItemStack stack = player.getMainHandItem();
        if (!player.isShiftKeyDown()) {
            var look = player.getLookAngle().multiply(3, 3, 3).add(player.getEyePosition());

            BlockPos lookAt = new BlockPos((int) look.x, (int) look.y, (int) look.z);
            addPos(stack, lookAt, player);
        }
        return IItemUIFactory.super.use(item, level, player, usedHand);
    }

    public void addPos(ItemStack stack, BlockPos pos, Player player) {
        int newMinX = min.getX(), newMinY = min.getY(), newMinZ = min.getZ(),
                newMaxX = max.getX(), newMaxY = max.getY(), newMaxZ = max.getZ();

        boolean changed = false;
        CompoundTag indexTag = stack.getOrCreateTag();
        if (!indexTag.contains("index")) indexTag.putInt("index", 1);
        int index = indexTag.getInt("index");
        if (index == 1) {
            player.displayClientMessage(Component.translatable("behaviour.structure_writer.place_first_block", pos.getX(), pos.getY(), pos.getZ()), true);
            if (pos.getX() < newMinX) newMinX = pos.getX();
            if (pos.getY() < newMinY) newMinY = pos.getY();
            if (pos.getZ() < newMinZ) newMinZ = pos.getZ();
            BlockPos newMin = new BlockPos(newMinX, newMinY, newMinZ);
            if (!newMin.equals(min)) {
                min = newMin;
                changed = true;
            }
        }

        if (index == 2) {
            player.displayClientMessage(Component.translatable("behaviour.structure_writer.place_second_block", pos.getX(), pos.getY(), pos.getZ()), true);
            if (pos.getX() > newMaxX) newMaxX = pos.getX();
            if (pos.getY() > newMaxY) newMaxY = pos.getY();
            if (pos.getZ() > newMaxZ) newMaxZ = pos.getZ();
            BlockPos newMax = new BlockPos(newMaxX, newMaxY, newMaxZ);
            if (!newMax.equals(max)) {
                max = newMax;
                changed = true;
            }
        }

        if(index < 3) {
            //clearPos(stack, player);
            indexTag.putInt("index", ++index);
        }


        CompoundTag tag = stack.getOrCreateTagElement("pos");
        if (index > 1) {
            tag.putInt("minX", Math.min(min.getX(), max.getX()));
            tag.putInt("minY", Math.min(min.getY(), max.getY()));
            tag.putInt("minZ", Math.min(min.getZ(), max.getZ()));

            tag.putInt("maxX", Math.max(min.getX(), max.getX()));
            tag.putInt("maxY", Math.max(min.getY(), max.getY()));
            tag.putInt("maxZ", Math.max(min.getZ(), max.getZ()));
        }
    }

    public void clearPos(ItemStack stack, Player player) {
        min = MAX;
        max = MIN;

        CompoundTag tag = stack.getOrCreateTagElement("pos");
        tag.remove("minX");
        tag.remove("minY");
        tag.remove("minZ");
        tag.remove("maxX");
        tag.remove("maxY");
        tag.remove("maxZ");

        CompoundTag tag2 = stack.getOrCreateTag();
        tag2.putInt("index", 1);

        player.displayClientMessage(Component.translatable("behaviour.structure_writer.cleared"), true);
    }

    public static Pair<BlockPos, BlockPos> getMinMax(ItemStack stack) {
        BlockPos min1 = MAX;
        BlockPos max1 = MIN;
        CompoundTag t = stack.getOrCreateTagElement("pos");
        if (t.contains("maxX")) {
            min1 = new BlockPos(t.getInt("minX"), t.getInt("minY"), t.getInt("minZ"));
            max1 = new BlockPos(t.getInt("maxX"), t.getInt("maxY"), t.getInt("maxZ"));
        }
        return Pair.of(min1, max1);
    }

    public static Direction getDirection(ItemStack stack) {
        var dir = stack.getOrCreateTag().getString("dir");
        return dir.isEmpty() ? Direction.WEST : Direction.byName(dir);
    }

    public static void setDirection(ItemStack stack, Direction dir) {
        stack.getOrCreateTag().putString("dir", dir.getName());
    }
}
