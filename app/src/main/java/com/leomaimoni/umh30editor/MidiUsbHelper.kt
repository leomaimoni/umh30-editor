package com.leomaimoni.umh30editor

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper

class MidiUsbHelper(context: Context) {
    private val manager =
        context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    private val handler = Handler(Looper.getMainLooper())

    private var device: MidiDevice? = null
    private var openedInput: MidiInputPort? = null
    private var openedOutput: MidiOutputPort? = null

    fun listUsbMidiDevices(): List<MidiDeviceInfo> =
        manager.devices.filter { it.type == MidiDeviceInfo.TYPE_USB }

    fun connectDevice(
        info: MidiDeviceInfo,
        onConnected: () -> Unit,
        onError: (String) -> Unit
    ) {
        close()
        manager.openDevice(info, { opened ->
            if (opened == null) {
                onError("Android não conseguiu abrir o dispositivo MIDI.")
                return@openDevice
            }
            device = opened
            onConnected()
        }, handler)
    }

    fun openInputPort(
        portNumber: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val d = device ?: run {
            onError("Conecte o UMH-30 primeiro.")
            return
        }
        try {
            openedInput?.close()
            openedInput = d.openInputPort(portNumber)
            if (openedInput == null) {
                onError("openInputPort($portNumber) retornou null.")
            } else {
                onSuccess()
            }
        } catch (e: Exception) {
            onError("openInputPort($portNumber): ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun openOutputPort(
        portNumber: Int,
        onReceive: (ByteArray) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val d = device ?: run {
            onError("Conecte o UMH-30 primeiro.")
            return
        }
        try {
            openedOutput?.close()
            val out = d.openOutputPort(portNumber)
            if (out == null) {
                onError("openOutputPort($portNumber) retornou null.")
                return
            }
            openedOutput = out
            out.connect(object : MidiReceiver() {
                override fun onSend(
                    data: ByteArray,
                    offset: Int,
                    count: Int,
                    timestamp: Long
                ) {
                    onReceive(data.copyOfRange(offset, offset + count))
                }
            })
            onSuccess()
        } catch (e: Exception) {
            onError("openOutputPort($portNumber): ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun send(bytes: ByteArray): Result<Unit> {
        val port = openedInput
            ?: return Result.failure(IllegalStateException("Nenhuma INPUT port está aberta."))
        return try {
            port.send(bytes, 0, bytes.size)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        try { openedOutput?.close() } catch (_: Exception) {}
        try { openedInput?.close() } catch (_: Exception) {}
        try { device?.close() } catch (_: Exception) {}
        openedOutput = null
        openedInput = null
        device = null
    }
}
