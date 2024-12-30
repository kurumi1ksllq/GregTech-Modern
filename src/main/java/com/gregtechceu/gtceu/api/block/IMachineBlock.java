package com.gregtechceu.gtceu.api.block;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.machine.owner.ArgonautsOwner;
import com.gregtechceu.gtceu.common.machine.owner.FTBOwner;
import com.gregtechceu.gtceu.common.machine.owner.IMachineOwner;
import com.gregtechceu.gtceu.common.machine.owner.PlayerOwner;

import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import dev.ftb.mods.ftbteams.FTBTeamsAPIImpl;
import dev.ftb.mods.ftbteams.api.Team;
import earth.terrarium.argonauts.api.guild.Guild;
import earth.terrarium.argonauts.common.handlers.guild.GuildHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * IMachineBlock is an interface that provides methods to get the properties of Machines.
 * This is useful for Machines that have different properties based on the type of machine.
 * For example, a Machine that provides different textures based on the level of the machine.
 */
public interface IMachineBlock extends IBlockRendererProvider, EntityBlock {
    DirectionProperty UPWARDS_FACING_PROPERTY = DirectionProperty.create("upwards_facing", Direction.Plane.HORIZONTAL);

    /** TODO: Rename to asBlock() or something similar
     * The self method is used to cast the block to a Block.
     */
    default Block self() {
        return (Block) this;
    }

    /**
     * Get the definition of the machine.
     * @return the {@link MachineDefinition} defining the properties of the machine
     */
    MachineDefinition getDefinition();

    /**
     * Get the rotation state of the machine.
     * @return the {@link RotationState} defining the rotation state of the machine
     */
    RotationState getRotationState();

    /** TODO: blockState is not used, remove it
     * Get the tinted color of the machine.
     * @param blockState the {@link BlockState} of the machine
     * @param level the {@link BlockAndTintGetter} of the machine
     * @param pos the {@link BlockPos} of the machine
     * @param index the index of the tinted color
     * @return the tinted color of the machine
     */
    static int colorTinted(BlockState blockState, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos,
                           int index) {
        // TODO: Flip conditions and return early
        if (level != null && pos != null) {
            var machine = MetaMachine.getMachine(level, pos);
            if (machine != null) {
                return machine.tintColor(index);
            }
        }
        return -1;
    }

    /** TODO: Rename to getBlockEntity() or something similar
     * TODO: Rename parameter pos to blockPos
     * Get the block entity of the machine.
     * @param pos the {@link BlockPos} of the machine
     * @param state the {@link BlockState} of the machine
     * @return the {@link BlockEntity} of the machine
     */
    @Nullable
    @Override
    default BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return getDefinition().getBlockEntityType().create(pos, state);
    }

    /**
     * Get the block entity ticker of the machine.
     * @param level the {@link Level} of the machine
     * @param state the {@link BlockState} of the machine
     * @param blockEntityType the {@link BlockEntityType} of the machine
     * @return the {@link BlockEntityTicker} of the machine
     */
    @Nullable
    @Override
    default <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> blockEntityType) {
        // TODO: Flip conditions and return early
        if (blockEntityType == getDefinition().getBlockEntityType()) {
            if (state.getValue(BlockProperties.SERVER_TICK) && !level.isClientSide) {
                return (pLevel, pPos, pState, pTile) -> {
                    if (pTile instanceof IMachineBlockEntity metaMachine) {
                        metaMachine.getMetaMachine().serverTick();
                    }
                };
            }
            if (level.isClientSide) {
                return (pLevel, pPos, pState, pTile) -> {
                    if (pTile instanceof IMachineBlockEntity metaMachine) {
                        metaMachine.getMetaMachine().clientTick();
                    }
                };
            }
        }
        return null;
    }

    /**
     * Set the machine owner of the machine.
     * @param machine the {@link MetaMachine} of the machine
     * @param player the {@link ServerPlayer} of the machine owner
     */
    default void setMachineOwner(MetaMachine machine, ServerPlayer player) {
        // FTB Teams API
        if (IMachineOwner.MachineOwnerType.FTB.isAvailable()) {
            Optional<Team> team = FTBTeamsAPIImpl.INSTANCE.getManager().getTeamForPlayerID(player.getUUID());
            if (team.isPresent()) {
                machine.holder.setOwner(new FTBOwner(team.get(), player.getUUID()));
                return;
            }
        }
        // Argonauts API
        else if (IMachineOwner.MachineOwnerType.ARGONAUTS.isAvailable()) {
            Guild guild = GuildHandler.read(player.server).get(player);
            if (guild != null) {
                machine.holder.setOwner(new ArgonautsOwner(guild, player.getUUID()));
                return;
            }
        }
        // Alternatively, set the owner to the player
        machine.holder.setOwner(new PlayerOwner(player.getUUID()));
    }
}