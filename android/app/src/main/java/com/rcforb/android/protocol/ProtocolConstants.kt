package com.rcforb.android.protocol

object ControlByte {
    const val HEARTBEAT: Byte = 0x00
    const val PTT: Byte = 0x03
    const val PINGPONG: Byte = 0x05
    const val PTT_OFF: Byte = 0x06
    const val KEY_OFF: Byte = 0xFB.toByte()
    const val KEY_ON: Byte = 0xFC.toByte()
    const val USERIN: Byte = 0xFD.toByte()
    const val USEROUT: Byte = 0xFE.toByte()
    const val UTF8STRING: Byte = 0xFF.toByte()
}

const val HEARTBEAT_TIMEOUT_MS = 15_000L
const val HEARTBEAT_INTERVAL_MS = 4_000L
const val PING_INTERVAL_MS = 1_000L
const val DEFAULT_CMD_PORT = 4525
const val DEFAULT_AUDIO_PORT = 4524

const val OPUS_SAMPLE_RATE = 48000
const val OPUS_CHANNELS = 1
const val OPUS_BITRATE = 24000
const val OPUS_FRAME_MS = 20
const val OPUS_FRAME_SIZE = (OPUS_SAMPLE_RATE * OPUS_FRAME_MS) / 1000 // 960

enum class PacketType {
    AUDIO, COMMAND, HEARTBEAT, PINGPONG,
    PTT_ON, PTT_OFF, KEY_ON, KEY_OFF,
    USER_IN, USER_OUT
}

fun classifyPacket(data: ByteArray): PacketType {
    if (data.isEmpty()) return PacketType.HEARTBEAT
    val firstByte = data[0]

    if (firstByte == ControlByte.UTF8STRING && data.size > 1) return PacketType.COMMAND
    if (data.size == 1) {
        return when (firstByte) {
            ControlByte.HEARTBEAT -> PacketType.HEARTBEAT
            ControlByte.PINGPONG -> PacketType.PINGPONG
            ControlByte.PTT -> PacketType.PTT_ON
            ControlByte.PTT_OFF -> PacketType.PTT_OFF
            ControlByte.KEY_ON -> PacketType.KEY_ON
            ControlByte.KEY_OFF -> PacketType.KEY_OFF
            ControlByte.USERIN -> PacketType.USER_IN
            ControlByte.USEROUT -> PacketType.USER_OUT
            else -> PacketType.AUDIO
        }
    }
    return PacketType.AUDIO
}
