package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.common.blockentity.EquipmentFoundryBlockEntity;
import com.gregtechceu.gtceu.common.data.GTBlockEntities;

import com.lowdragmc.lowdraglib.gui.factory.BlockEntityUIFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("deprecation")
@ParametersAreNonnullByDefault
public class EquipmentFoundryBlock extends BaseEntityBlock {

    public EquipmentFoundryBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return GTBlockEntities.EQUIPMENT_FOUNDRY.get().create(pos, state);
    }

    @Override
    public @NotNull InteractionResult use(BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer &&
                level.getBlockEntity(pos) instanceof EquipmentFoundryBlockEntity equipmentFoundry) {
            BlockEntityUIFactory.INSTANCE.openUI(equipmentFoundry, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
