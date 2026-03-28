package com.saurabh.sonicaudiorecorder.data

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    
    private var noiseSuppressor: NoiseSuppressor? = null
    private var automaticGainControl: AutomaticGainControl? = null
    
    private var isRecording = false
    private var isPaused = false
    
    private var lastAmplitude = 0f

    // High Quality Configuration
    private val sampleRate = 48000 // Professional standard
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO // Mono for voice clarity
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT // High depth
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    fun start(outputFile: File, format: String = "WAV") {
        if (isRecording) return
        isRecording = true
        isPaused = false
        
        if (format == "WAV") {
            startWavRecording(outputFile)
        } else {
            startAacRecording(outputFile)
        }
    }

    private fun startAacRecording(outputFile: File) {
        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION) // Optimized for clarity
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(sampleRate)
            setAudioEncodingBitRate(192000) // High bit rate for AAC
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
    }

    @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
    @SuppressLint("MissingPermission")
    private fun startWavRecording(outputFile: File) {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, // Best for clean, processed voice
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        // Enable Hardware Effects if available
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(audioRecord!!.audioSessionId)
            noiseSuppressor?.enabled = true
        }
        if (AutomaticGainControl.isAvailable()) {
            automaticGainControl = AutomaticGainControl.create(audioRecord!!.audioSessionId)
            automaticGainControl?.enabled = true
        }

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val data = ByteArray(bufferSize)
            try {
                outputFile.createNewFile()
                val fos = FileOutputStream(outputFile)
                
                writeWavHeader(fos)
                
                audioRecord?.startRecording()
                
                while (isRecording) {
                    if (isPaused) {
                        lastAmplitude = 0f
                        Thread.sleep(100)
                        continue
                    }
                    
                    val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                    if (read > 0) {
                        fos.write(data, 0, read)
                        
                        // Amplitude calc for UI visualization
                        var sum = 0.0
                        for (i in 0 until read step 2) {
                            val sample = ByteBuffer.wrap(data, i, 2).order(ByteOrder.LITTLE_ENDIAN).short.toDouble()
                            sum += sample * sample
                        }
                        lastAmplitude = Math.sqrt(sum / (read / 2)).toFloat()
                    }
                }
                
                fos.close()
                updateWavHeader(outputFile)
            } catch (e: Exception) {
                Log.e("AudioRecorder", "WAV recording failed", e)
            } finally {
                cleanupEffects()
            }
        }
    }

    private fun cleanupEffects() {
        noiseSuppressor?.release()
        noiseSuppressor = null
        automaticGainControl?.release()
        automaticGainControl = null
    }

    fun stop() {
        isRecording = false
        isPaused = false
        recordingJob?.cancel()
        
        mediaRecorder?.apply {
            try { stop() } catch(_: Exception) {}
            release()
        }
        mediaRecorder = null

        audioRecord?.apply {
            try { stop() } catch(_: Exception) {}
            release()
        }
        audioRecord = null
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
        isPaused = true
        mediaRecorder?.pause()
    }

    fun resume() {
        if (!isRecording || !isPaused) return
        isPaused = false
        mediaRecorder?.resume()
    }

    private fun writeWavHeader(out: FileOutputStream) {
        val channels = if (channelConfig == AudioFormat.CHANNEL_IN_MONO) 1 else 2
        val bitRate = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 16 else 8
        val byteRate = (sampleRate * channels * bitRate / 8).toLong()
        val header = ByteArray(44)
        val dataLength = 0L // Header is initially written with 0 length
        val totalDataLen = dataLength + 36

        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte(); header[5] = (0).toByte()
        header[6] = (0).toByte(); header[7] = (0).toByte()
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
        header[40] = (0).toByte(); header[41] = (0 and 0xff).toByte()
        header[42] = (0 and 0xff).toByte(); header[43] = (0 and 0xff).toByte()
        out.write(header, 0, 44)
    }

    private fun updateWavHeader(file: File) {
        val raf = RandomAccessFile(file, "rw")
        val totalDataLen = file.length() - 8
        val audioDataLen = file.length() - 44
        
        raf.seek(4)
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(totalDataLen.toInt()).array())
        raf.seek(40)
        raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(audioDataLen.toInt()).array())
        raf.close()
    }
}
