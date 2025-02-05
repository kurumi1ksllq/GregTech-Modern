package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.client.audio.SeekingOggAudioStream;
import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Mixin(SoundBufferLibrary.class)
@Debug(export = true)
public abstract class SoundBufferLibraryMixin {

    @Final
    @Shadow
    private ResourceProvider resourceManager;

    @Inject(method = "lambda$getStream$2", at = @At(value = "HEAD"), cancellable = true)
    private void getStreamLambdaMixin(ResourceLocation resourceLocation, boolean isWrapper, CallbackInfoReturnable<AudioStream> cir) {
        String[] metadata = SeekingOggAudioStream.getMetadataAndPath(resourceLocation.getPath());
        if (metadata.length > 1 && !metadata[1].isEmpty()) {
            long seekMs = Long.parseLong(metadata[1]);
            ResourceLocation strippedResourceLocation = new ResourceLocation(resourceLocation.getNamespace(), metadata[0]);
            try (InputStream inputstream = this.resourceManager.open(strippedResourceLocation)) {
                cir.setReturnValue(new SeekingOggAudioStream(inputstream, seekMs));
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
    }

    @Inject(method = "lambda$getCompleteBuffer$0", at = @At(value = "HEAD"), cancellable = true)
    private void getCompleteBufferMixin(ResourceLocation resourceLocation, CallbackInfoReturnable<SoundBuffer> cir) {
        String[] metadata = SeekingOggAudioStream.getMetadataAndPath(resourceLocation.getPath());
        if (metadata.length > 1 && !metadata[1].isEmpty()) {
            long seekMs = Long.parseLong(metadata[1]);
            ResourceLocation strippedResourceLocation = new ResourceLocation(resourceLocation.getNamespace(), metadata[0]);
            try (InputStream inputstream = this.resourceManager.open(strippedResourceLocation)) {
                SeekingOggAudioStream stream = new SeekingOggAudioStream(inputstream, seekMs);
                ByteBuffer data = stream.readAll();
                cir.setReturnValue(new SoundBuffer(data, stream.getFormat()));
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }
    }
}
