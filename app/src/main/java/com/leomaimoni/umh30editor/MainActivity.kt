package com.leomaimoni.umh30editor

import android.media.midi.MidiDeviceInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private lateinit var midi: MidiUsbHelper

    private var devices by mutableStateOf<List<MidiDeviceInfo>>(emptyList())
    private var connectedName by mutableStateOf<String?>(null)
    private var status by mutableStateOf("Conecte o UMH-30 ao celular.")
    private var log by mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        midi = MidiUsbHelper(this)
        refresh()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        midi.close()
        super.onDestroy()
    }

    private fun refresh() {
        devices = midi.listUsbMidiDevices()
        if (devices.isEmpty()) {
            status = "Nenhum dispositivo USB-MIDI encontrado."
        } else {
            status = "${devices.size} dispositivo(s) USB-MIDI encontrado(s)."
        }
    }

    private fun deviceName(info: MidiDeviceInfo): String {
        return info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            ?: "MIDI device ${info.id}"
    }

    private fun connect(info: MidiDeviceInfo) {
        status = "Conectando..."
        midi.connect(
            info = info,
            onConnected = {
                connectedName = deviceName(info)
                status = "Conectado a ${deviceName(info)}."
                addLog("CONNECTED")
                addLog("Inputs: ${info.inputPortCount}, Outputs: ${info.outputPortCount}")
                info.ports.forEach { port ->
                    addLog(
                        "Port ${port.portNumber}: " +
                            if (port.type == MidiDeviceInfo.PortInfo.TYPE_INPUT) "INPUT" else "OUTPUT"
                    )
                }
            },
            onReceive = { bytes ->
                runOnUiThread {
                    addLog("RX  ${Umh30Protocol.toHex(bytes)}")
                }
            },
            onError = { error ->
                runOnUiThread {
                    connectedName = null
                    status = error
                    addLog("ERROR  $error")
                }
            }
        )
    }

    private fun sendSave() {
        if (!midi.isConnected) {
            status = "Conecte o UMH-30 primeiro."
            return
        }

        val result = midi.send(Umh30Protocol.saveCommand)

        result.onSuccess {
            status = "Comando SAVE enviado."
            addLog("TX  ${Umh30Protocol.toHex(Umh30Protocol.saveCommand)}")
        }.onFailure {
            status = "Erro ao enviar SAVE: ${it.message}"
            addLog("TX ERROR  ${it.message}")
        }
    }

    private fun addLog(line: String) {
        log = (log + line).takeLast(80)
    }

    @Composable
    private fun AppScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "UMH-30 Editor",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "v0.1-alpha • transport + protocol capture",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("STATUS", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(status)

                    connectedName?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Connected: $it")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "USB MIDI devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(onClick = { refresh() }) {
                    Text("Refresh")
                }
            }

            Spacer(Modifier.height(8.dp))

            if (devices.isEmpty()) {
                Text("Nenhum dispositivo encontrado.")
            } else {
                devices.forEach { info ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                deviceName(info),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "ID ${info.id} • IN ${info.inputPortCount} • OUT ${info.outputPortCount}"
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = { connect(info) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (connectedName == deviceName(info))
                                        "Reconnect"
                                    else
                                        "Connect"
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Text(
                "Confirmed UMH-30 command",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text("SAVE / SET")
            Text(
                Umh30Protocol.toHex(Umh30Protocol.saveCommand),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { sendSave() },
                enabled = midi.isConnected,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SEND SAVE / SET")
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "MIDI / SysEx log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    if (log.isEmpty()) {
                        Text("Nenhum evento ainda.")
                    } else {
                        log.forEach { line ->
                            Text(
                                line,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
