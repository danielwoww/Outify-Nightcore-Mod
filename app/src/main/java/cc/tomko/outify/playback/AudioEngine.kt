package cc.tomko.outify.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.media3.common.util.UnstableApi
import cc.tomko.outify.playback.callbacks.PlayerEventCallback
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

private const val TAG = "AudioEngine"
private const val MIN_SPEED = 0.25f
private const val MAX_SPEED = 4f

enum class PcmFormat {
    S16,
}

/**
 * Plays the received PCM audio using modern AudioAttributes/AudioFormat API.
 */
@UnstableApi
class AudioEngine(
    val context: Context,
    eventCallback: PlayerEventCallback,
    private val stateHolder: PlaybackStateHolder,
) {
    @Volatile
    private var audioTrack: AudioTrack? = null
    private var currentSampleRate = -1
    private var currentChannels = -1
    private var currentFormat: PcmFormat? = null

    @Volatile
    private var outputPaused = false

    private val pcmBuffer = ByteBuffer.allocateDirect(4 * 8192)

    /*
     * Nightcore mode: speed changes tempo AND pitch together (plain resampling,
     * like speeding up a record), instead of Sonic's pitch-preserving time-stretch.
     *
     * Preferred path: let AudioTrack resample natively via setPlaybackRate()
     * (high quality, no CPU or allocations on our side). If the device rejects
     * the requested rate (typically above 2x its native output rate), fall back
     * to a software linear-interpolation resampler that keeps its phase across
     * PCM chunks so there are no clicks at chunk boundaries.
     */
    private var appliedTrackRate = -1
    private var rejectedTrackRate = -1
    private var resampleOut: ByteBuffer = ByteBuffer.allocateDirect(0)
    private var resamplePos = 0.0
    private var resampleHasPrev = false
    private val resamplePrev = ShortArray(8)

    private val writeLock = ReentrantLock()

    init {
        // Registers this class as the PCM callback.
        // Rust stores the GlobalRef and calls the onPcm method
        registerPcmCallback(this, pcmBuffer)

        // Registers callbacks to handle librespot events
        registerPlayerEventListener(eventCallback)
    }

    private fun ensureAudioTrack(sampleRate: Int, channels: Int, format: PcmFormat): Boolean {
        writeLock.withLock {
            val existing = audioTrack
            if (existing != null
                && sampleRate == currentSampleRate
                && channels == currentChannels
                && format == currentFormat
                && existing.state == AudioTrack.STATE_INITIALIZED
            ) {
                if (existing.playState != AudioTrack.PLAYSTATE_PLAYING) existing.play()
                return true
            }

            // Otherwise recreate
            releaseAudioTrack()

            val channelMask = when (channels) {
                1 -> AudioFormat.CHANNEL_OUT_MONO
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                else -> {
                    // fallback to stereo for unknown channel counts
                    Log.w(TAG, "Unsupported channel count $channels, falling back to stereo")
                    AudioFormat.CHANNEL_OUT_STEREO
                }
            }

            val encoding = when (format) {
                PcmFormat.S16 -> AudioFormat.ENCODING_PCM_16BIT
            }

            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding)
            if (minBufferSize <= 0) {
                Log.e(TAG, "Invalid min buffer size: $minBufferSize")
                return false
            }

            val bytesPerSample = when (encoding) {
                AudioFormat.ENCODING_PCM_16BIT -> 2
                AudioFormat.ENCODING_PCM_8BIT -> 1
                AudioFormat.ENCODING_PCM_FLOAT -> 4
                else -> 2
            }
            val frameSize = bytesPerSample * max(1, channels)
            // Sized for the fastest supported speed: at N x the track drains N times
            // faster, so a 1x-sized buffer (~20 ms) leaves only a few ms of headroom and
            // underruns (crackles/dropouts). Pause and seek flush the track, so the
            // extra size doesn't add control latency.
            val bufferSize = max(minBufferSize, frameSize * 1024) * MAX_SPEED.toInt()

            try {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                val formatBuilder = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(encoding)
                    .setChannelMask(channelMask)
                    .build()

                val newTrack = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(formatBuilder)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                if (newTrack.state != AudioTrack.STATE_INITIALIZED) {
                    Log.e(TAG, "Failed to initialize AudioTrack: state=${newTrack.state}")
                    newTrack.release()
                    return false
                }

                newTrack.play()

                audioTrack = newTrack
                currentSampleRate = sampleRate
                currentChannels = channels
                currentFormat = format

                Log.d(
                    TAG,
                    "AudioTrack created: sampleRate=$sampleRate, channels=$channels, encoding=$encoding, buffer=$bufferSize"
                )
                return true
            } catch (t: Throwable) {
                Log.e(TAG, "Exception while creating AudioTrack", t)
                return false
            }
        }
    }

    fun releaseAudioTrack() {
        writeLock.withLock {
            resetResampler()
            appliedTrackRate = -1
            rejectedTrackRate = -1
            val t = audioTrack ?: return
            try {
                if (t.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    try {
                        t.stop()
                    } catch (ignored: IllegalStateException) {
                        // ignore - may happen if already stopped
                    }
                } else if (t.playState == AudioTrack.PLAYSTATE_PAUSED) {
                    try {
                        t.stop()
                    } catch (ignored: IllegalStateException) {
                    }
                }
            } catch (ignored: Exception) {
            } finally {
                try {
                    t.release()
                } catch (ignored: Exception) {
                }
                audioTrack = null
                currentSampleRate = -1
                currentChannels = -1
                currentFormat = null
            }
        }
    }

    /**
     * Releases native resources — frees JNI GlobalRefs for PCM callback
     * and player event listener.
     */
    fun releaseNative() {
        unregisterPcmCallback()
        unregisterPlayerEventListener()
    }

    fun pause() {
        outputPaused = true
        // Do not wait for writeLock before pausing: a blocking write may hold it.
        // AudioTrack.pause() interrupts that write so teardown cannot deadlock.
        try {
            audioTrack?.pause()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "pause() failed", e)
        }
        writeLock.withLock {
            resetResampler()
            audioTrack?.let {
                try {
                    it.pause()
                    it.flush()
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "pause() failed", e)
                }
            }
        }
    }

    fun resume() {
        // Start the track on the next PCM frame, never from a stale status alone.
        if (stateHolder.playbackIntent.canPlay) outputPaused = false
    }

    private fun resetResampler() {
        resamplePos = 0.0
        resampleHasPrev = false
    }

    fun setVolume(volume: Float) {
        audioTrack?.setVolume(
            volume.coerceIn(0.0f, AudioTrack.getMaxVolume())
        )
    }

    fun flush() {
        writeLock.withLock {
            resetResampler()
            audioTrack?.let {
                try {
                    it.flush()
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "flush() failed", e)
                }
            }
        }
    }

    /**
     * Marks this class as the one to receive onPcm data
     */
    private external fun registerPcmCallback(callbackPtr: AudioEngine?, buffer: ByteBuffer)

    /**
     * Called from rust trampoline when the PCMBuffer is filled with PCM.
     */
    fun onPcmReady(size: Int, sampleRate: Int, channels: Int) {
        writeLock.withLock {
            if (outputPaused || !stateHolder.playbackIntent.canPlay) return
            pcmBuffer.order(ByteOrder.nativeOrder())

            if (!ensureAudioTrack(sampleRate, channels, PcmFormat.S16)) {
                Log.w(TAG, "ensureAudioTrack failed - dropping frame")
                return
            }

            val cap = pcmBuffer.capacity()
            if (size > cap) {
                Log.w(TAG, "pcm size $size > buffer capacity $cap; dropping frame")
                return
            }

            pcmBuffer.position(0)
            pcmBuffer.limit(size)

            try {
                val track = audioTrack ?: run {
                    Log.w(TAG, "audioTrack is null in onPcmReady")
                    return
                }

                val speed = stateHolder.state.value.playbackSpeed.coerceIn(MIN_SPEED, MAX_SPEED)
                val targetRate = if (speed == 1f) sampleRate else (sampleRate * speed).toInt()
                val nativeOk = applyTrackRate(track, targetRate)
                if (nativeOk) {
                    resetResampler()
                    writeToTrack(pcmBuffer, size, track)
                } else {
                    // Device can't resample this far natively: play at the source
                    // rate and resample in software instead.
                    applyTrackRate(track, sampleRate)
                    val out = resamplePcmS16(pcmBuffer, size, channels, speed)
                    if (out.limit() > 0) writeToTrack(out, out.limit(), track)
                }
            } catch (ise: IllegalStateException) {
                Log.e(TAG, "AudioTrack write failed", ise)
            } finally {
                pcmBuffer.position(0)
                pcmBuffer.limit(pcmBuffer.capacity())
            }
        }
    }

    /**
     * Sets the AudioTrack's playback (resampling) rate. Returns false if the
     * device rejects it. Cheap to call per chunk: only touches the track on change.
     */
    private fun applyTrackRate(track: AudioTrack, rate: Int): Boolean {
        if (rate == appliedTrackRate) return true
        if (rate == rejectedTrackRate) return false
        val result = track.setPlaybackRate(rate)
        if (result == AudioTrack.SUCCESS) {
            appliedTrackRate = rate
            return true
        }
        rejectedTrackRate = rate
        Log.w(TAG, "setPlaybackRate($rate) rejected ($result); using software resampler")
        return false
    }

    /**
     * Software fallback: linear-interpolation resampler for interleaved S16 PCM.
     * Reading the input [speed] frames per output frame raises tempo and pitch
     * together. The fractional read position and the last input frame are kept
     * between calls so consecutive chunks join seamlessly. The output buffer is
     * reused to avoid allocating on every PCM callback.
     */
    private fun resamplePcmS16(
        input: ByteBuffer,
        inputSizeBytes: Int,
        channels: Int,
        speed: Float,
    ): ByteBuffer {
        val ch = channels.coerceIn(1, resamplePrev.size)
        val bytesPerFrame = 2 * ch
        val inFrames = inputSizeBytes / bytesPerFrame

        val maxOut = (inFrames / speed).toInt() + 2
        if (resampleOut.capacity() < maxOut * bytesPerFrame) {
            resampleOut = ByteBuffer.allocateDirect(maxOut * bytesPerFrame * 2)
                .order(ByteOrder.nativeOrder())
        }
        val out = resampleOut
        out.clear()
        if (inFrames == 0) {
            out.limit(0)
            return out
        }

        // Frame index -1 refers to the last frame of the previous chunk.
        fun sample(frame: Int, c: Int): Int =
            if (frame < 0) resamplePrev[c].toInt()
            else input.getShort((frame * ch + c) * 2).toInt()

        var pos = if (resampleHasPrev) resamplePos else resamplePos.coerceAtLeast(0.0)
        val step = speed.toDouble()
        val last = inFrames - 1
        while (pos < last && out.remaining() >= bytesPerFrame) {
            val i = kotlin.math.floor(pos).toInt()
            val frac = pos - i
            for (c in 0 until ch) {
                val s0 = sample(i, c)
                val s1 = sample(i + 1, c)
                out.putShort((s0 + (s1 - s0) * frac).toInt().toShort())
            }
            pos += step
        }

        for (c in 0 until ch) resamplePrev[c] = input.getShort((last * ch + c) * 2)
        resampleHasPrev = true
        resamplePos = pos - inFrames // relative to the next chunk, >= -1

        out.flip()
        return out
    }

    private fun writeToTrack(buffer: ByteBuffer, size: Int, track: AudioTrack) {
        val written = track.write(buffer, size, AudioTrack.WRITE_BLOCKING)
        if (written < 0) {
            Log.e(TAG, "AudioTrack.write returned error: $written")
        } else if (written < size) {
            Log.w(TAG, "AudioTrack wrote $written / $size bytes (partial write)")
        }
    }

    /**
     * Registers PlayerEvent listener to FFI.
     * FFI stores the GlobalRef of the callback
     */
    external fun registerPlayerEventListener(callback: PlayerEventCallback);

    /**
     * Unregisters the PCM callback, freeing its JNI GlobalRef
     */
    private external fun unregisterPcmCallback()

    /**
     * Unregisters the player event listener, freeing its JNI GlobalRef
     */
    private external fun unregisterPlayerEventListener()
}
