package com.gregtechceu.gtceu.api.item.armor;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.armor.modifier.ArmorModifier;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.core.mixins.ServerGamePacketListenerImplAccessor;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.ForgeEventFactory;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ArmorUtils {

    public static final int MIN_NIGHTVISION_CHARGE = 4;
    public static final int NIGHTVISION_DURATION = 20 * 20; // 20 seconds
    public static final int NIGHT_VISION_RESET = 11 * 20; // 11 seconds is before the flashing

    public static final String ARMOR_KEY = "GT.Armor";
    public static final String MODIFIERS_KEY = "Modifiers";
    public static final String MAX_MODIFIERS_KEY = "MaxModifiers";

    public static boolean isModifiable(ItemStack stack) {
        return stack.is(CustomTags.MODIFIABLE_EQUIPMENT);
    }

    public static boolean hasArmorTag(ItemStack stack) {
        return isModifiable(stack) && stack.getTagElement(ARMOR_KEY) != null;
    }

    @Nullable
    public static CompoundTag getArmorTag(ItemStack stack) {
        if (!isModifiable(stack)) return null;
        return stack.getOrCreateTagElement(ARMOR_KEY);
    }

    /**
     * @param stack the stack to get maximum modifier amount for
     * @return the maximum amount of modifiers for the given stack
     */
    public static int getMaxModifiers(ItemStack stack) {
        if (!(hasArmorTag(stack)
                && getArmorTag(stack).contains(MAX_MODIFIERS_KEY, Tag.TAG_INT))
                && stack.getItem() instanceof ModifiableArmorItem armorComponentItem) {
            setMaxModifiers(stack, armorComponentItem.getDefaultMaxModifiers());
            return armorComponentItem.getDefaultMaxModifiers();
        } else if (!hasArmorTag(stack)) {
            return 0;
        }
        return getArmorTag(stack).getInt(MAX_MODIFIERS_KEY);
    }

    public static void setMaxModifiers(ItemStack stack, int maxModifiers) {
        if (!isModifiable(stack)) return;
        getArmorTag(stack).putInt(MAX_MODIFIERS_KEY, maxModifiers);
    }

    /**
     * Clear all modifiers from the given piece of armor
     * @param stack the armor to remove all modifiers from
     */
    public static void clearModifiers(ItemStack stack) {
        if (!hasArmorTag(stack)) return;
        CompoundTag tag = getArmorTag(stack);
        tag.remove(MODIFIERS_KEY);
    }

    /**
     * Add an armor modifier to the given stack
     * @param stack the stack to add the modifier to, if both are valid
     * @param modifier the modifier to add to the stack
     */
    public static void addModifier(ItemStack stack, ArmorModifier modifier) {
        CompoundTag tag = getArmorTag(stack);
        ListTag modifierList = tag.getList(MODIFIERS_KEY, Tag.TAG_STRING);
        if (modifierList.size() >= getMaxModifiers(stack)) return;

        modifierList.add(StringTag.valueOf(modifier.id().toString()));
        modifier.onAddToItem().apply(stack);
        tag.put(MODIFIERS_KEY, modifierList);
    }

    /**
     * An unmodifiable list of all modifiers on the given stack
     * @param stack the stack to get the modifiers from
     * @return the modifiers on the stack
     */
    @Unmodifiable
    public static @NotNull List<ArmorModifier> getModifiers(ItemStack stack) {
        if (!hasArmorTag(stack)) return Collections.emptyList();
        CompoundTag tag = getArmorTag(stack);
        ListTag modifierList = tag.getList(MODIFIERS_KEY, Tag.TAG_STRING);

        List<ArmorModifier> modifiers = new ArrayList<>();
        for (int i = 0; i < modifierList.size(); i++) {
            String idString = modifierList.getString(i);
            ResourceLocation id = ResourceLocation.tryParse(idString);
            if (id == null) {
                GTCEu.LOGGER.error("invalid armor modifier with id {}", idString);
                continue;
            }
            modifiers.add(ArmorModifier.MODIFIERS.get(id));
        }
        return modifiers;
    }

    /**
     * Check is possible to charge item
     */
    public static boolean isPossibleToCharge(ItemStack chargeable) {
        IElectricItem container = GTCapabilityHelper.getElectricItem(chargeable);
        if (container != null) {
            return container.getCharge() < container.getMaxCharge() &&
                    (container.getCharge() + container.getTransferLimit()) <= container.getMaxCharge();
        }
        return false;
    }

    /**
     * Searches all three player inventories for items that can be charged
     *
     * @param tier of charger
     * @return Map of the inventory and a list of the index of a chargable item
     */
    public static List<Pair<NonNullList<ItemStack>, List<Integer>>> getChargeableItem(Player player, int tier) {
        List<Pair<NonNullList<ItemStack>, List<Integer>>> inventorySlotMap = new ArrayList<>();

        List<Integer> openMainSlots = new ArrayList<>();
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack current = player.getInventory().items.get(i);
            IElectricItem item = GTCapabilityHelper.getElectricItem(current);
            if (item == null) continue;

            if (isPossibleToCharge(current) && item.getTier() <= tier) {
                openMainSlots.add(i);
            }
        }

        if (!openMainSlots.isEmpty()) {
            inventorySlotMap.add(Pair.of(player.getInventory().items, openMainSlots));
        }

        List<Integer> openArmorSlots = new ArrayList<>();
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack current = player.getInventory().armor.get(i);
            IElectricItem item = GTCapabilityHelper.getElectricItem(current);
            if (item == null) {
                continue;
            }

            if (isPossibleToCharge(current) && item.getTier() <= tier) {
                openArmorSlots.add(i);
            }
        }

        if (!openArmorSlots.isEmpty()) {
            inventorySlotMap.add(Pair.of(player.getInventory().armor, openArmorSlots));
        }

        ItemStack offHand = player.getInventory().offhand.get(0);
        IElectricItem offHandItem = GTCapabilityHelper.getElectricItem(offHand);
        if (offHandItem == null) {
            return inventorySlotMap;
        }

        if (isPossibleToCharge(offHand) && offHandItem.getTier() <= tier) {
            inventorySlotMap.add(Pair.of(player.getInventory().offhand, Collections.singletonList(0)));
        }

        return inventorySlotMap;
    }

    /**
     * Spawn particle behind player with speedY speed
     */
    public static void spawnParticle(Level world, Player player, ParticleOptions type, double speedY) {
        if (type != null) {
            Vec3 forward = player.getForward();
            world.addParticle(type, player.getX() - forward.x, player.getY() + 0.5D, player.getZ() - forward.z, 0.0D,
                    speedY, 0.0D);
        }
    }

    public static void playJetpackSound(@Nonnull Player player) {
        if (player.level().isClientSide()) {
            float cons = (float) player.getDeltaMovement().y + player.moveDist;
            cons = Mth.clamp(cons, 0.6F, 1.0F);

            if (player.getDeltaMovement().y > 0.05F) {
                cons += 0.1F;
            }

            if (player.getDeltaMovement().y < -0.05F) {
                cons -= 0.4F;
            }

            player.playSound(GTSoundEntries.JET_ENGINE.getMainEvent(), 0.3F, cons);
        }
    }

    /**
     * Resets private field, amount of ticks player in the sky
     */
    public static void resetPlayerFloatingTime(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ((ServerGamePacketListenerImplAccessor) serverPlayer.connection).setAboveGroundTickCount(0);
        }
    }

    /**
     * This method feeds player with food, if food heal amount more than
     * empty food gaps, then reminder adds to saturation
     *
     * @return result of eating food
     */
    public static InteractionResultHolder<ItemStack> eat(Player player, ItemStack food) {
        if (!food.isEdible()) {
            return InteractionResultHolder.fail(food);
        }

        FoodProperties foodItem = food.getFoodProperties(player);
        if (foodItem != null && player.getFoodData().needsFood()) {
            ItemStack result = ForgeEventFactory.onItemUseFinish(player, food.copy(), player.getUseItemRemainingTicks(),
                    food.finishUsingItem(player.level(), player));
            return InteractionResultHolder.success(result);
        } else {
            return InteractionResultHolder.fail(food);
        }
    }

    /**
     * Format itemstacks list from [1xitem@1, 1xitem@1, 1xitem@2] to
     * [2xitem@1, 1xitem@2]
     *
     * @return Formated list
     */
    public static List<ItemStack> format(List<ItemStack> input) {
        Object2IntMap<ItemStack> items = new Object2IntOpenCustomHashMap<>(
                ItemStackHashStrategy.comparingAllButCount());
        List<ItemStack> output = new ArrayList<>();
        for (ItemStack itemStack : input) {
            if (items.containsKey(itemStack)) {
                int amount = items.get(itemStack);
                items.replace(itemStack, ++amount);
            } else {
                items.put(itemStack, 1);
            }
        }
        for (Object2IntMap.Entry<ItemStack> entry : items.object2IntEntrySet()) {
            ItemStack stack = entry.getKey().copy();
            stack.setCount(entry.getIntValue());
            output.add(stack);
        }
        return output;
    }

    @Nonnull
    public static String format(long value) {
        return new DecimalFormat("###,###.##").format(value);
    }

    public static String format(double value) {
        return new DecimalFormat("###,###.##").format(value);
    }

    /**
     * Modular HUD class for armor
     * now available only string rendering, if will be needed,
     * may be will add some additional functions
     */
    @OnlyIn(Dist.CLIENT)
    public static class ModularHUD {

        private byte stringAmount = 0;
        private final List<Component> stringList;
        private static final Minecraft mc = Minecraft.getInstance();

        public ModularHUD() {
            this.stringList = new ArrayList<>();
        }

        public void newString(Component string) {
            this.stringAmount++;
            this.stringList.add(string);
        }

        public void draw(GuiGraphics poseStack) {
            for (int i = 0; i < stringAmount; i++) {
                Pair<Integer, Integer> coords = this.getStringCoord(i);
                poseStack.drawString(mc.font, stringList.get(i), coords.getFirst(), coords.getSecond(), 0xFFFFFF,
                        false);
            }
        }

        @Nonnull
        private Pair<Integer, Integer> getStringCoord(int index) {
            int posX;
            int posY;
            int fontHeight = mc.font.lineHeight;
            int windowHeight = mc.getWindow().getGuiScaledHeight();
            int windowWidth = mc.getWindow().getGuiScaledWidth();
            int stringWidth = mc.font.width(stringList.get(index));
            switch (ConfigHolder.INSTANCE.client.armorHud.hudLocation) {
                case 1 -> {
                    posX = 1 + ConfigHolder.INSTANCE.client.armorHud.hudOffsetX;
                    posY = 1 + ConfigHolder.INSTANCE.client.armorHud.hudOffsetY + (fontHeight * index);
                }
                case 2 -> {
                    posX = windowWidth - (1 + ConfigHolder.INSTANCE.client.armorHud.hudOffsetX) - stringWidth;
                    posY = 1 + ConfigHolder.INSTANCE.client.armorHud.hudOffsetY + (fontHeight * index);
                }
                case 3 -> {
                    posX = 1 + ConfigHolder.INSTANCE.client.armorHud.hudOffsetX;
                    posY = windowHeight - fontHeight * (stringAmount - index) - 1 -
                            ConfigHolder.INSTANCE.client.armorHud.hudOffsetY;
                }
                case 4 -> {
                    posX = windowWidth - (1 + ConfigHolder.INSTANCE.client.armorHud.hudOffsetX) - stringWidth;
                    posY = windowHeight - fontHeight * (stringAmount - index) - 1 -
                            ConfigHolder.INSTANCE.client.armorHud.hudOffsetY;
                }
                default -> throw new IllegalArgumentException(
                        "Armor Hud config hudLocation is improperly configured. Allowed values: [1,2,3,4]");
            }
            return Pair.of(posX, posY);
        }

        public void reset() {
            this.stringAmount = 0;
            this.stringList.clear();
        }
    }
}
