package com.saurabh.sonicaudiorecorder.data

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.StatFs
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AudioRecorder"

@Singleton
class AudioRecorder @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null

    @Volatile
    private var isRecording = false
    @Volatile
    private var isPaused = false

    @Volatile
    private var recordingException: Throwable? = null

    private var lastAmplitude = 0f
    private var currentOutputFile: File? = null

    private val recordingMutex = Mutex()

    // High Quality Configuration
    private val sampleRate = 48000 // Professional standard
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO // Mono for voice clarity
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT // High depth
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    // Interface for error reporting
    interface RecordingErrorListener {
        fun onError(message: String)
    }
    private var errorListener: RecordingErrorListener? = null

    fun setErrorListener(listener: RecordingErrorListener?) {
        this.errorListener = listener
    }

    suspend fun start(outputFile: File, format: String = "WAV"): Boolean = recordingMutex.withLock {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress")
            return false
        }

        // Pre-flight check: Disk space
        if (!hasEnoughSpace(outputFile.parentFile ?: context.filesDir)) {
            Log.e(TAG, "Insufficient storage to start recording")
            errorListener?.onError("Insufficient storage space")
            return false
        }

        Log.i(TAG, "Starting recording: ${outputFile.absolutePath}, format: $format")
        isRecording = true
        isPaused = false
        recordingException = null
        currentOutputFile = outputFile

        return if (format == "WAV") {
            startWavRecording(outputFile)
        } else {
            startAacRecording(outputFile)
        }
    }

    private fun hasEnoughSpace(directory: File, requiredMb: Long = 10): Boolean {
        return try {
            val stat = StatFs(directory.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes > (requiredMb * 1024 * 1024)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage space", e)
            true
        }
    }

    private fun startAacRecording(outputFile: File): Boolean {
        return try {
            mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }).apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(sampleRate)
                setAudioEncodingBitRate(192000)
                setOutputFile(outputFile.absolutePath)

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaRecorder error: $what, extra: $extra")
                    errorListener?.onError("Recording error: $what")
                    recordingException = IOException("MediaRecorder error: $what, extra: $extra")
                    isRecording = false
                }

                prepare()
                start()
                Log.d(TAG, "AAC Recording started successfully")
            }
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to start AAC recording", e)
            errorListener?.onError("Failed to start recording: ${e.message}")
            isRecording = false
            false
        }
    }

    @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
    @SuppressLint("MissingPermission")
    private fun startWavRecording(outputFile: File): Boolean {
        return try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(audioRecord!!.audioSessionId)
                noiseSuppressor?.enabled = true
                Log.d(TAG, "NoiseSuppressor enabled")
            }
            if (AutomaticGainControl.isAvailable()) {
                automaticGainControl = AutomaticGainControl.create(audioRecord!!.audioSessionId)
                automaticGainControl?.enabled = true
                Log.d(TAG, "AutomaticGainControl enabled")
            }

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val data = ByteArray(bufferSize)
                var totalBytesWritten = 0L
                var raf: RandomAccessFile? = null

                try {
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                    outputFile.createNewFile()

                    raf = RandomAccessFile(outputFile, "rw")
                    writeWavHeader(raf)

                    audioRecord?.startRecording()
                    if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        throw IOException("AudioRecord failed to start")
                    }

                    Log.d(TAG, "WAV Recording started")
                    var lastHeaderUpdate = 0L

                    while (isRecording && isActive) {
                        val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                        if (read > 0) {
                            if (isPaused) {
                                lastAmplitude = 0f
                                continue
                            }

                            try {
                                raf.write(data, 0, read)
                                totalBytesWritten += read

                                if (totalBytesWritten - lastHeaderUpdate > 200000) {
                                    updateWavHeader(raf, totalBytesWritten)
                                    lastHeaderUpdate = totalBytesWritten
                                }
                            } catch (e: IOException) {
                                Log.e(TAG, "I/O error during recording", e)
                                errorListener?.onError("I/O error: ${e.message}")
                                recordingException = e
                                isRecording = false
                                break
                            }

                            var sum = 0.0
                            for (i in 0 until read step 2) {
                                val sample = ((data[i + 1].toInt() shl 8) or (data[i].toInt() and 0xFF)).toShort().toDouble()
                                sum += sample * sample
                            }
                            lastAmplitude = Math.sqrt(sum / (read / 2)).toFloat()
                        } else if (read < 0) {
                            if (isRecording && isActive) {
                                Log.e(TAG, "AudioRecord read error during active recording: $read")
                                errorListener?.onError("Hardware recording error")
                                recordingException = IOException("AudioRecord read error: $read")
                            } else {
                                Log.d(TAG, "AudioRecord read returned $read during intentional stop or cancellation")
                            }
                            break
                        }
                    }

                    if (raf != null && totalBytesWritten > 0) {
                        updateWavHeader(raf, totalBytesWritten)
                    }
                    Log.d(TAG, "WAV Recording finished. Total bytes: $totalBytesWritten")

                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "WAV recording loop failed", e)
                    recordingException = e
                    errorListener?.onError("Recording failed: ${e.message}")
                } finally {
                    try {
                        raf?.close()
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to close RandomAccessFile", e)
                        if (recordingException == null) {
                            recordingException = e
                        }
                    }
                    cleanupEffects()
                }
            }
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Failed to initialize WAV recording", e)
            errorListener?.onError("Failed to start recording: ${e.message}")
            isRecording = false
            false
        }
    }

    private fun cleanupEffects() {
        try {
            noiseSuppressor?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing NoiseSuppressor", e)
        }
        noiseSuppressor = null
        try {
            automaticGainControl?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AutomaticGainControl", e)
        }
        automaticGainControl = null
    }

    suspend fun stop(): Result<File?> = recordingMutex.withLock {
        if (!isRecording && recordingJob == null && mediaRecorder == null) {
            return Result.success(currentOutputFile)
        }
        Log.i(TAG, "Stopping recording")
        isRecording = false
        isPaused = false

        var stopException: Throwable? = null

        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "MediaRecorder stop failed", e)
                stopException = e
            }
            try {
                release()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "MediaRecorder release failed", e)
            }
        }
        mediaRecorder = null

        audioRecord?.apply {
            try {
                stop()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "AudioRecord stop failed", e)
                if (stopException == null) stopException = e
            }
        }

        // Deterministic join without timeout
        try {
            recordingJob?.join()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error joining recordingJob", e)
            if (stopException == null) stopException = e
        }
        recordingJob = null

        audioRecord?.apply {
            try {
                release()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "AudioRecord release failed", e)
            }
        }
        audioRecord = null

        val outputFile = currentOutputFile
        currentOutputFile = null

        val finalError = recordingException ?: stopException

        return if (finalError == null && outputFile != null && outputFile.exists() && outputFile.length() > 44) {
            Log.i(TAG, "File successfully finalized: ${outputFile.absolutePath}, size: ${outputFile.length()} bytes")
            Result.success(outputFile)
        } else {
            val errorMsg = finalError?.message ?: "Recording output file invalid or corrupt"
            Log.e(TAG, "Recording finalization failed: $errorMsg")
            Result.failure(finalError ?: IOException("File missing or invalid WAV size: ${outputFile?.length()}"))
        }
    }

    fun getAmplitude(): Float {
        if (isPaused) return 0f
        return if (mediaRecorder != null) {
            try {
                mediaRecorder?.maxAmplitude?.toFloat() ?: 0f
            } catch (_: Exception) {
                0f
            }
        } else {
            lastAmplitude
        }
    }

    fun pause() {
        if (!isRecording || isPaused) return
        Log.d(TAG, "Pausing recording")
        isPaused = true
        try {
            mediaRecorder?.pause()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "MediaRecorder pause failed", e)
        }
    }

    fun resume() {
        if (!isRecording || !isPaused) return
        Log.d(TAG, "Resuming recording")
        isPaused = false
        try {
            mediaRecorder?.resume()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "MediaRecorder resume failed", e)
        }
    }

    private fun writeWavHeader(raf: RandomAccessFile) {
        val channels = if (channelConfig == AudioFormat.CHANNEL_IN_MONO) 1 else 2
        val bitRate = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 16 else 8
        val byteRate = (sampleRate * channels * bitRate / 8).toLong()
        val header = ByteArray(44)
        val dataLength = 0L 
        val totalDataLen = dataLength + 36

        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 
        header[20] = 1; header[21] = 0 
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte(); header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte(); header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte(); header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte(); header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * bitRate / 8).toByte(); header[33] = 0 
        header[34] = bitRate.toByte(); header[35] = 0 
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = 0; header[41] = 0; header[42] = 0; header[43] = 0

        raf.seek(0)
        raf.write(header)
    }

    private fun updateWavHeader(raf: RandomAccessFile, audioDataLen: Long) {
        val totalDataLen = audioDataLen + 36
        val currentPos = raf.filePointer

        raf.seek(4)
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(totalDataLen.toInt()).array())

        raf.seek(40)
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(audioDataLen.toInt()).array())

        raf.seek(currentPos)
    }
}
