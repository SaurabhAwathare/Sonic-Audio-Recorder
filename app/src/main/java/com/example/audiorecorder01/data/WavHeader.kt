package com.saurabh.sonicaudiorecorder.data

import java.io.RandomAccessFile

object WavHeader {
    fun writeWavHeader(
        file: RandomAccessFile,
        channels: Int,
        sampleRate: Int,
        bitDepth: Int,
        dataSize: Long
    ) {
        val totalFileSize = dataSize + 36
        val byteRate = sampleRate * channels * bitDepth / 8

        file.seek(0)
        file.writeBytes("RIFF") // ChunkID
        file.write(intToByteArray(totalFileSize.toInt()), 0, 4) // ChunkSize
        file.writeBytes("WAVE") // Format
        file.writeBytes("fmt ") // Subchunk1ID
        file.write(intToByteArray(16), 0, 4) // Subchunk1Size
        file.write(shortToByteArray(1), 0, 2) // AudioFormat (PCM = 1)
        file.write(shortToByteArray(channels.toShort()), 0, 2) // NumChannels
        file.write(intToByteArray(sampleRate), 0, 4) // SampleRate
        file.write(intToByteArray(byteRate), 0, 4) // ByteRate
        file.write(shortToByteArray((channels * bitDepth / 8).toShort()), 0, 2) // BlockAlign
        file.write(shortToByteArray(bitDepth.toShort()), 0, 2) // BitsPerSample
        file.writeBytes("data") // Subchunk2ID
        file.write(intToByteArray(dataSize.toInt()), 0, 4) // Subchunk2Size
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            (value shr 8 and 0xff).toByte(),
            (value shr 16 and 0xff).toByte(),
            (value shr 24 and 0xff).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xff).toByte(),
            (value.toInt() shr 8 and 0xff).toByte()
        )
    }
}
