package com.leomaimoni.umh30editor

/**
 * UMH-30 routing protocol.
 *
 * Source/destination encoding recovered from the previous UMH-30
 * investigation:
 *
 * type 00 = physical MIDI
 * type 01 = USB Host
 * index 1-based
 * state 01 = connect, 00 = disconnect
 *
 * Routing command:
 * F0 00 21 5E 02 01 SRC_TYPE SRC_INDEX DST_TYPE DST_INDEX STATE F7
 *
 * SET/SAVE:
 * F0 00 21 5E 02 02 00 F7
 */
object Umh30Protocol {
    private const val PHYSICAL = 0x00
    private const val USB_HOST = 0x01
    private const val CONNECT = 0x01

    val saveCommand = byteArrayOf(
        0xF0.toByte(), 0x00, 0x21, 0x5E,
        0x02, 0x02, 0x00, 0xF7.toByte()
    )

    fun routingCommand(
        sourceType: Int,
        sourceIndex: Int,
        destinationType: Int,
        destinationIndex: Int,
        connected: Boolean = true
    ): ByteArray {
        return byteArrayOf(
            0xF0.toByte(), 0x00, 0x21, 0x5E,
            0x02, 0x01,
            sourceType.toByte(),
            sourceIndex.toByte(),
            destinationType.toByte(),
            destinationIndex.toByte(),
            if (connected) CONNECT.toByte() else 0x00,
            0xF7.toByte()
        )
    }

    fun physical(index: Int): Int = PHYSICAL
    fun usbHost(): Int = USB_HOST

    fun hex(bytes: ByteArray): String =
        bytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}
