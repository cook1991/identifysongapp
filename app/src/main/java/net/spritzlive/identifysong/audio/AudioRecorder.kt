package net.spritzlive.identifysong.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private const val SAMPLE_RATE = 44100
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

/**
 * Records mono 16-bit PCM audio from the mic for [durationMs] and writes it out
 * as a playable WAV file (AudD's API needs a real audio container, not raw PCM).
 */
object AudioRecorder {

    suspend fun recordWav(context: Context, durationMs: Long = 9000): File = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("RECORD_AUDIO permission not granted")
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        require(minBufferSize > 0) { "Unable to configure AudioRecord for this device" }

        val bufferSize = minBufferSize * 2
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        val pcmBuffer = ByteArrayOutputStream()
        val readBuffer = ByteArray(bufferSize)

        try {
            audioRecord.startRecording()
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < durationMs) {
                val read = audioRecord.read(readBuffer, 0, readBuffer.size)
                if (read > 0) {
                    pcmBuffer.write(readBuffer, 0, read)
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }

        val pcmData = pcmBuffer.toByteArray()
        val outFile = File.createTempFile("identifysong_", ".wav", context.cacheDir)
        FileOutputStream(outFile).use { out ->
            out.write(buildWavHeader(pcmData.size, SAMPLE_RATE, channels = 1, bitsPerSample = 16))
            out.write(pcmData)
        }
        outFile
    }

    private fun buildWavHeader(pcmDataSize: Int, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val totalDataLen = pcmDataSize + 36

        fun ByteArrayOutputStream.writeIntLE(value: Int) {
            write(value and 0xff)
            write((value shr 8) and 0xff)
            write((value shr 16) and 0xff)
            write((value shr 24) and 0xff)
        }

        fun ByteArrayOutputStream.writeShortLE(value: Int) {
            write(value and 0xff)
            write((value shr 8) and 0xff)
        }

        val header = ByteArrayOutputStream(44)
        header.write("RIFF".toByteArray(Charsets.US_ASCII))
        header.writeIntLE(totalDataLen)
        header.write("WAVE".toByteArray(Charsets.US_ASCII))
        header.write("fmt ".toByteArray(Charsets.US_ASCII))
        header.writeIntLE(16) // fmt chunk size
        header.writeShortLE(1) // PCM format
        header.writeShortLE(channels)
        header.writeIntLE(sampleRate)
        header.writeIntLE(byteRate)
        header.writeShortLE(blockAlign)
        header.writeShortLE(bitsPerSample)
        header.write("data".toByteArray(Charsets.US_ASCII))
        header.writeIntLE(pcmDataSize)
        return header.toByteArray()
    }
}
