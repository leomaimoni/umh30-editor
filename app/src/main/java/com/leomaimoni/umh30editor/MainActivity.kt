package com.leomaimoni.umh30editor

import android.media.midi.MidiDeviceInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private lateinit var midi: MidiUsbHelper

    private var devices by mutableStateOf<List<MidiDeviceInfo>>(emptyList())
    private var selectedId by mutableStateOf<Int?>(null)
    private var connected by mutableStateOf(false)
    private var status by mutableStateOf("Conecte o UMH-30.")
    private var logs by mutableStateOf(listOf<String>())
    private val inputState = mutableStateMapOf<Int, String>()
    private val outputState = mutableStateMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        midi = MidiUsbHelper(this)
        refresh()
        setContent { MaterialTheme { Screen() } }
    }

    override fun onResume() {
        super.onResume()
        if (::midi.isInitialized) refresh()
    }

    override fun onDestroy() {
        if (::midi.isInitialized) midi.close()
        super.onDestroy()
    }

    private fun refresh() {
        devices = midi.listUsbMidiDevices()
        if (devices.isEmpty()) status = "Nenhum USB-MIDI encontrado."
    }

    private fun name(info: MidiDeviceInfo): String =
        info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            ?: "MIDI ${info.id}"

    private fun connect(info: MidiDeviceInfo) {
        midi.connectDevice(info,
            onConnected = {
                selectedId = info.id
                connected = true
                status = "Dispositivo aberto. Agora teste as portas individualmente."
                addLog("DEVICE OPEN: ${name(info)}  ID=${info.id}")
                addLog("INPUT ports: ${info.inputPortCount}")
                addLog("OUTPUT ports: ${info.outputPortCount}")
                info.ports.forEach {
                    val type = if (it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT) "INPUT" else "OUTPUT"
                    addLog("PORT ${it.portNumber}: $type")
                }
            },
            onError = {
                connected = false
                status = it
                addLog("ERROR: $it")
            }
        )
    }

    private fun openInput(port: Int) {
        status = "Abrindo INPUT $port..."
        midi.openInputPort(port,
            onSuccess = {
                inputState[port] = "OPEN"
                status = "INPUT $port aberta."
                addLog("OPEN INPUT $port: SUCCESS")
            },
            onError = {
                inputState[port] = "ERROR"
                status = it
                addLog("OPEN INPUT $port: $it")
            }
        )
    }

    private fun listenOutput(port: Int) {
        status = "Abrindo OUTPUT $port..."
        midi.openOutputPort(port,
            onReceive = { bytes ->
                runOnUiThread {
                    addLog("RX OUTPUT $port: ${Umh30Protocol.hex(bytes)}")
                }
            },
            onSuccess = {
                outputState[port] = "OPEN"
                status = "OUTPUT $port aberta para escuta."
                addLog("OPEN OUTPUT $port: SUCCESS")
            },
            onError = {
                outputState[port] = "ERROR"
                status = it
                addLog("OPEN OUTPUT $port: $it")
            }
        )
    }

    private fun sendSave() {
        val result = midi.send(Umh30Protocol.saveCommand)
        result.onSuccess {
            status = "SAVE enviado."
            addLog("TX: ${Umh30Protocol.hex(Umh30Protocol.saveCommand)}")
        }.onFailure {
            status = "Erro TX: ${it.message}"
            addLog("TX ERROR: ${it.message}")
        }
    }

    private fun addLog(s: String) {
        logs = (logs + s).takeLast(100)
    }

    @Composable
    private fun Screen() {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text("UMH-30 Editor", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Text("v0.2-alpha • Port Diagnostic")

            Spacer(Modifier.height(12.dp))
            Text(status)

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("USB MIDI devices", fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { refresh() }) { Text("Refresh") }
            }

            devices.forEach { info ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(name(info), fontWeight = FontWeight.Bold)
                        Text("ID ${info.id} • IN ${info.inputPortCount} • OUT ${info.outputPortCount}")
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { connect(info) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("OPEN DEVICE") }
                    }
                }
            }

            if (connected) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Text("INPUT PORTS • app → UMH-30",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)

                for (p in 0..2) {
                    PortRow(
                        label = "INPUT $p",
                        state = inputState[p],
                        action = { openInput(p) }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("OUTPUT PORTS • UMH-30 → app",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)

                for (p in 0..2) {
                    PortRow(
                        label = "OUTPUT $p",
                        state = outputState[p],
                        action = { listenOutput(p) }
                    )
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { sendSave() },
                    enabled = inputState.values.any { it == "OPEN" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SEND CONFIRMED SAVE / SET")
                }

                Spacer(Modifier.height(16.dp))
                Text("DIAGNOSTIC LOG", fontWeight = FontWeight.Bold)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        logs.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }

    @Composable
    private fun PortRow(
        label: String,
        state: String?,
        action: () -> Unit
    ) {
        Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, fontWeight = FontWeight.Bold)
                    Text(state ?: "not tested")
                }
                Button(onClick = action) { Text("TEST") }
            }
        }
    }
}
