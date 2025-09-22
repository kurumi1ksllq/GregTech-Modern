package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraftforge.server.ServerLifecycleHooks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class GTRecipeTransformer implements IValueTransformer<GTRecipe> {

    private static RecipeManager getRecipeManager() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && Thread.currentThread() == server.getRunningThread()) {
            return server.getRecipeManager();
        } else {
            return Objects.requireNonNull(Minecraft.getInstance().getConnection()).getRecipeManager();
        }
    }

    @Override
    public void writeToBuffer(GTRecipe value, FriendlyByteBuf buf) {
        buf.writeBoolean(value == null);
        if (value == null) return;
        buf.writeResourceLocation(value.id);
        GTRecipeSerializer.SERIALIZER.toNetwork(buf, value);
        buf.writeInt(value.parallels);
        buf.writeInt(value.ocLevel);
    }

    @Override
    public GTRecipe readFromBuffer(FriendlyByteBuf buf, GTRecipe currentValue) {
        if (buf.readBoolean()) return null;
        GTRecipe recipe;
        var id = buf.readResourceLocation();
        if (buf.isReadable()) {
            recipe = GTRecipeSerializer.SERIALIZER.fromNetwork(id, buf);
            if (buf.isReadable()) {
                recipe.parallels = buf.readInt();
                recipe.ocLevel = buf.readInt();
            }
        } else { // Backwards Compatibility
            RecipeManager recipeManager = getRecipeManager();
            recipe = (GTRecipe) recipeManager.byKey(id).orElse(null);
        }

        return recipe;
    }

    @Override
    public Tag serializeNBT(GTRecipe value) {
        CompoundTag tag = new CompoundTag();
        if (value == null) return tag;
        tag.putString("id", value.id.toString());
        tag.put("recipe",
                GTRecipeSerializer.CODEC.encodeStart(NbtOps.INSTANCE, value).result().orElse(new CompoundTag()));
        tag.putInt("parallels", value.parallels);
        tag.putInt("ocLevel", value.ocLevel);
        return tag;
    }

    @Override
    public GTRecipe deserializeNBT(Tag tag, @Nullable GTRecipe currentVal) {
        if (tag instanceof CompoundTag comp && comp.isEmpty()) return null;
        RecipeManager recipeManager = getRecipeManager();
        GTRecipe result = null;
        if (tag instanceof CompoundTag compoundTag) {
            result = GTRecipeSerializer.CODEC.parse(NbtOps.INSTANCE, compoundTag.get("recipe")).result().orElse(null);
            if (result != null) {
                result.id = new ResourceLocation(compoundTag.getString("id"));
                result.parallels = compoundTag.contains("parallels") ? compoundTag.getInt("parallels") : 1;
                result.ocLevel = compoundTag.getInt("ocLevel");
            }
        } else if (tag instanceof StringTag stringTag) { // Backwards Compatibility
            var recipe = recipeManager.byKey(new ResourceLocation(stringTag.getAsString())).orElse(null);
            if (recipe instanceof GTRecipe gtRecipe) {
                result = gtRecipe;
            } else if (recipe instanceof SmeltingRecipe smeltingRecipe) {
                result = GTRecipeTypes.FURNACE_RECIPES.toGTrecipe(new ResourceLocation(stringTag.getAsString()),
                        smeltingRecipe);
            }
        } else if (tag instanceof ByteArrayTag byteArray) { // Backwards Compatibility
            ByteBuf copiedDataBuffer = Unpooled.copiedBuffer(byteArray.getAsByteArray());
            FriendlyByteBuf buf = new FriendlyByteBuf(copiedDataBuffer);
            result = (GTRecipe) recipeManager.byKey(buf.readResourceLocation()).orElse(null);
            buf.release();
        }
        return result;
    }
}
