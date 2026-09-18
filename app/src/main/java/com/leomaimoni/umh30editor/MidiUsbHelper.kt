package com.leomaimoni.umh30editor

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper

/**
 * Low-level Android MIDI transport.
 *
 * This is intentionally separated from the UMH-30 protocol.
 * That way we can change/reverse-engineer the SysEx commands without
 * touching the USB/MIDI connection code.
 */
class MidiUsbHelper(context: Context) {

    private val midiManager =
        context.getSystemService(Context.MIDI_SERVICE) as MidiManager

    private val mainHandler = Handler(Looper.getMainLooper())

    private var device: MidiDevice? = null
    private var inputPort: MidiInputPort? = null
    private var outputPort: MidiOutputPort? = null

    fun listUsbMidiDevices(): List<MidiDeviceInfo> {
        return midiManager.devices.filter {
            it.type == MidiDeviceInfo.TYPE_USB
        }
    }

    fun connect(
        info: MidiDeviceInfo,
        onConnected: (MidiDeviceInfo) -> Unit,
        onReceive: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        close()

        midiManager.openDevice(info, { opened ->
            if (opened == null) {
                onError("Não foi possível abrir o dispositivo MIDI.")
                return@openDevice
            }

            try {
                val inputInfo = info.ports.firstOrNull {
                    it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT
                }
                val outputInfo = info.ports.firstOrNull {
                    it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT
                }

                if (inputInfo == null) {
                    opened.close()
                    onError("O UMH-30 não apresentou uma porta MIDI de entrada para envio.")
                    return@openDevice
                }

                inputPort = opened.openInputPort(inputInfo.portNumber)

                if (outputInfo != null) {
                    outputPort = opened.openOutputPort(outputInfo.portNumber)

                    outputPort?.connect(object : MidiReceiver() {
                        override fun onSend(
                            data: ByteArray,
                            offset: Int,
                            count: Int,
                            timestamp: Long
                        ) {
                            onReceive(data.copyOfRange(offset, offset + count))
                        }
                    })
                }

                if (inputPort == null) {
                    opened.close()
                    onError("Não foi possível abrir a porta MIDI de envio.")
                    return@openDevice
                }

                device = opened
                onConnected(info)

            } catch (e: Exception) {
                try { opened.close() } catch (_: Exception) {}
                onError("Erro ao abrir MIDI: ${e.message}")
            }
        }, mainHandler)
    }

    fun send(bytes: ByteArray): Result<Unit> {
        val port = inputPort
            ?: return Result.failure(IllegalStateException("MIDI não conectado."))

        return try {
            port.send(bytes, 0, bytes.size)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    val isConnected: Boolean
        get() = inputPort != null

    fun close() {
        try { outputPort?.close() } catch (_: Exception) {}
        try { inputPort?.close() } catch (_: Exception) {}
        try { device?.close() } catch (_: Exception) {}

        outputPort = null
        inputPort = null
        device = null
    }
}
