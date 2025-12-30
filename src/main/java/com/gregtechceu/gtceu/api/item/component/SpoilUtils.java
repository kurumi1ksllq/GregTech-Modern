package com.gregtechceu.gtceu.api.item.component;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public class SpoilUtils {

    /**
     * Consider frozen and non-frozen spoilables equal. This is done to allow filtering by ticks remaining until
     * spoiled.<br>
     * If you want the player to have frozen stacks in their inventory, set this to {@code false} to prevent players
     * from
     * entirely bypassing the spoilage system.
     */
    public static boolean FROZEN_EQUALITY = true;

    /**
     * Initializes this ItemStack's spoilage timer if it wasn't initialized before.
     * Should be called when it finishes crafting, for example.
     */
    public static void update(ItemStack stack, SpoilContext spoilContext) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(stack);
        if (spoilable != null) spoilable.updateFreshness(spoilContext, true);
    }

    public static void updateBlock(Level level, BlockPos pos) {
        updateBlock(level.getBlockEntity(pos));
    }

    public static void updateBlock(BlockEntity blockEntity) {
        for (Direction side : Direction.values()) updateBlock(blockEntity, side);
        updateBlock(blockEntity, null);
    }

    public static void updateBlock(BlockEntity blockEntity, Direction side) {
        IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).resolve().orElse(null);
        if (handler != null) {
            SpoilContext ctx = new SpoilContext(blockEntity.getLevel(), blockEntity.getBlockPos(), null, handler, -1);
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                update(handler.getStackInSlot(slot), ctx.withSlot(slot));
            }
        }
    }

    public static void spawnEntity(SpoilContext spoilContext, EntityType<? extends Mob> type, int count) {
        if (spoilContext.level() instanceof ServerLevel level) {
            BlockPos pos = null;
            if (spoilContext.entity() != null) pos = spoilContext.entity().blockPosition();
            else if (spoilContext.pos() != null) pos = spoilContext.pos();
            if (pos != null && type != null) {
                if (level.getBlockState(pos).isSuffocating(level, pos)) {
                    for (Direction direction : Direction.values()) {
                        BlockPos relative = pos.relative(direction);
                        if (!level.getBlockState(relative).isSuffocating(level, relative)) {
                            pos = relative;
                            break;
                        }
                    }
                }
                for (int i = 0; i < count; i++) type.spawn(level, pos, MobSpawnType.SPAWN_EGG);
            }
        }
    }
}
