package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.data.recipe.GTRecipeTypes;

import com.lowdragmc.lowdraglib.syncdata.payload.ObjectTypedPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

/**
 * @author KilaBash
 * @date 2023/2/18
 * @implNote GTRecipePayload
 */
public class GTRecipePayload extends ObjectTypedPayload<GTRecipe> {

    private static RecipeManager getRecipeManager() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && Thread.currentThread() == server.getRunningThread()) {
            return server.getRecipeManager();
        } else {
            return Client.getRecipeManager();
        }
    }

    @Nullable
    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", payload.id.toString());
        tag.put("recipe",
                GTRecipeSerializer.CODEC.encoder().encode(payload, NbtOps.INSTANCE, NbtOps.INSTANCE.empty())
                        .result().orElse(new CompoundTag()));
        return tag;
    }

    @Override
    public void deserializeNBT(Tag tag, HolderLookup.Provider provider) {
        RecipeManager recipeManager = getRecipeManager();
        if (tag instanceof CompoundTag compoundTag) {
            NbtOps.INSTANCE.getMap(compoundTag.get("recipe")).ifSuccess(
                    map -> payload = GTRecipeSerializer.CODEC.decode(NbtOps.INSTANCE, map).result().orElse(null))
                    .ifError(t -> payload = null);
            if (payload != null) {
                payload.id = ResourceLocation.parse(compoundTag.getString("id"));
            }
        } else if (tag instanceof StringTag stringTag) { // Backwards Compatibility
            var holder = recipeManager.byKey(ResourceLocation.parse(stringTag.getAsString())).orElse(null);
            var recipe = holder == null ? null : holder.value();
            if (recipe instanceof GTRecipe gtRecipe) {
                payload = gtRecipe;
            } else if (recipe instanceof SmeltingRecipe) {
                payload = GTRecipeTypes.FURNACE_RECIPES.toGTRecipe(holder).value();
            } else {
                payload = null;
            }
        } else if (tag instanceof ByteArrayTag byteArray) { // Backwards Compatibility
            ByteBuf copiedDataBuffer = Unpooled.copiedBuffer(byteArray.getAsByteArray());
            FriendlyByteBuf buf = new FriendlyByteBuf(copiedDataBuffer);
            RecipeHolder<?> holder = recipeManager.byKey(buf.readResourceLocation()).orElse(null);
            this.payload = holder == null ? null : (GTRecipe) holder.value();
            buf.release();
        }
    }

    @Override
    public void writePayload(RegistryFriendlyByteBuf buf) {
        GTRecipeSerializer.STREAM_CODEC.encode(buf, this.payload);
    }

    @Override
    public void readPayload(RegistryFriendlyByteBuf buf) {
        var id = buf.readResourceLocation();
        if (buf.isReadable()) {
            buf.resetReaderIndex();
            this.payload = GTRecipeSerializer.STREAM_CODEC.decode(buf);
        } else { // Backwards Compatibility
            RecipeManager recipeManager = getRecipeManager();
            this.payload = (GTRecipe) recipeManager.byKey(id).map(RecipeHolder::value).orElse(null);
        }
    }

    static class Client {

        static RecipeManager getRecipeManager() {
            return Minecraft.getInstance().getConnection().getRecipeManager();
        }
    }
}
