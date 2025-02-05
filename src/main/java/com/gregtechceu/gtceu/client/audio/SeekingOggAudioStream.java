package com.gregtechceu.gtceu.client.audio;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.audio.OggAudioStream;

import java.io.IOException;
import java.io.InputStream;

@OnlyIn(Dist.CLIENT)
public class SeekingOggAudioStream extends OggAudioStream {

    public SeekingOggAudioStream(InputStream input, long msPos) throws IOException {
        super(input);
        seekBy(msPos);
    }

    public void seekBy(long milli) throws IOException {
        final long samplesPerS = (long) this.getFormat().getSampleRate();
        final int bytesPerSample = (this.getFormat().getChannels() * this.getFormat().getSampleSizeInBits() / 8);

        long remainingBytes = milli * samplesPerS * bytesPerSample / 1000L;

        read((int) remainingBytes);
    }

    /**
     * Encodes the given millisecond seek amount into a path/resource name suffix that can be appended to the sound path
     * to start playing from that point onwards.
     */
    public static String getEncodedSeekSuffxix(long milliseconds) {
        return String.format("?%d", milliseconds);
    }

    /**
     * @return The given path and the seeking metadata
     */

    public static String[] getMetadataAndPath(String path) {
        return path.split("\\?", 1);
    }
}
