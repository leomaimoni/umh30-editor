package com.leomaimoni.umh30editor

import android.media.midi.MidiDeviceInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class RouteNode(
    val id: String,
    val label: String,
    val type: Int,
    val index: Int
)

private const val MAX_USB_HOSTS = 8

class MainActivity : ComponentActivity() {
    private lateinit var midi: MidiUsbHelper

    private var devices by mutableStateOf<List<MidiDeviceInfo>>(emptyList())
    private var connected by mutableStateOf(false)
    private var status by mutableStateOf("Conecte o UMH-30.")
    private var logs by mutableStateOf(listOf<String>())
    private var routes by mutableStateOf(setOf<Pair<String, String>>())
    private var pendingChanges by mutableStateOf(false)
    private var usbNames by mutableStateOf<Map<Int, String>>(emptyMap())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        midi = MidiUsbHelper(this)
        refresh()
        setContent { MaterialTheme { App() } }
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
        status = if (devices.isEmpty()) "Nenhum USB-MIDI encontrado."
        else "${devices.size} USB-MIDI encontrado(s)."
    }

    private fun deviceName(info: MidiDeviceInfo): String =
        info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            ?: "MIDI ${info.id}"

    private fun connect(info: MidiDeviceInfo) {
        midi.connectDevice(info, {
            connected = true
            status = "UMH-30 conectado. Lendo identificação dos USB Host..."
            addLog("DEVICE OPEN: ${deviceName(info)} ID=${info.id}")
            addLog("ANDROID MIDI: IN=${info.inputPortCount} OUT=${info.outputPortCount}")

            // The UMH-30 appears to Android as one MIDI device with 3 output ports.
            // Listen on all three because the identification SysEx can be exposed on
            // the MIDI output stream selected by the firmware.
            for (port in 0 until info.outputPortCount.coerceAtMost(3)) {
                midi.openOutputPort(
                    port,
                    onReceive = { bytes ->
                        val parsed = Umh30Protocol.parseUsbHostName(bytes)
                        runOnUiThread {
                            addLog("RX OUT ${port + 1}: ${Umh30Protocol.hex(bytes)}")
                            if (parsed != null) {
                                usbNames = usbNames + (parsed.slot to parsed.name)
                                status = "USB ${parsed.slot}: ${parsed.name}"
                                addLog("USB HOST ${parsed.slot} = ${parsed.name}")
                            }
                        }
                    },
                    onSuccess = {
                        addLog("LISTEN OUTPUT ${port + 1}: SUCCESS")
                    },
                    onError = {
                        addLog("LISTEN OUTPUT ${port + 1}: $it")
                    }
                )
            }
        }, {
            connected = false
            status = it
            addLog("ERROR: $it")
        })
    }

    private fun sources(): List<RouteNode> = buildList {
        add(RouteNode("in1", "MIDI IN 1", 0, 1))
        add(RouteNode("in2", "MIDI IN 2", 0, 2))
        for (i in 1..MAX_USB_HOSTS) {
            val label = usbNames[i]?.let { "USB $i • $it" } ?: "MIDI USB $i"
            add(RouteNode("usb$i", label, 1, i))
        }
    }

    private fun destinations(): List<RouteNode> = buildList {
        add(RouteNode("out1", "MIDI OUT 1", 0, 1))
        add(RouteNode("out2", "MIDI OUT 2", 0, 2))
        for (i in 1..MAX_USB_HOSTS) {
            val label = usbNames[i]?.let { "USB $i • $it" } ?: "MIDI USB $i"
            add(RouteNode("usbout$i", label, 1, i))
        }
    }

    private fun forbidden(s: RouteNode, d: RouteNode): Boolean =
        s.type == d.type && s.index == d.index

    private fun toggle(s: RouteNode, d: RouteNode) {
        if (forbidden(s, d)) {
            status = "${s.label} não pode ser ligado a ele mesmo."
            return
        }
        val key = s.id to d.id
        routes = if (key in routes) routes - key else routes + key
        pendingChanges = true
        status = if (key in routes) "${s.label} → ${d.label}" else "Ligação removida."
    }

    private fun clearAll() {
        routes = emptySet()
        pendingChanges = true
        status = "Todas as ligações foram limpas."
    }

    private fun sendToDevice() {
        if (!connected) {
            status = "Conecte o UMH-30 primeiro."
            return
        }
        if (routes.isEmpty()) {
            status = "Nenhuma ligação selecionada."
            return
        }

        val src = sources()
        val dst = destinations()

        routes.forEach { (sid, did) ->
            val s = src.firstOrNull { it.id == sid } ?: return@forEach
            val d = dst.firstOrNull { it.id == did } ?: return@forEach
            if (forbidden(s, d)) return@forEach

            val msg = Umh30Protocol.routingCommand(
                s.type, s.index, d.type, d.index, true
            )
            midi.send(msg).onSuccess {
                addLog("TX ${s.label} -> ${d.label}")
                addLog(Umh30Protocol.hex(msg))
            }.onFailure {
                addLog("TX ERROR: ${it.message}")
            }
        }

        midi.send(Umh30Protocol.saveCommand).onSuccess {
            addLog("TX SET: ${Umh30Protocol.hex(Umh30Protocol.saveCommand)}")
            status = "Ligações enviadas e SET executado."
            pendingChanges = false
        }.onFailure {
            status = "Falha ao enviar SET: ${it.message}"
        }
    }

    private fun addLog(s: String) {
        logs = (logs + s).takeLast(160)
    }

    @Composable
    private fun App() {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("UMH-30 Editor", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    Text("v0.5-alpha • USB Host names + Routing", fontSize = 10.sp)
                }
                Text(
                    if (connected) "● CONNECTED" else "○ DISCONNECTED",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(5.dp))
            Text(status, fontSize = 10.sp)

            if (!connected) {
                devices.forEach { info ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(
                            Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(deviceName(info), fontWeight = FontWeight.Bold)
                                Text(
                                    "ID ${info.id} • IN ${info.inputPortCount} • OUT ${info.outputPortCount}",
                                    fontSize = 10.sp
                                )
                            }
                            Button(onClick = { connect(info) }) { Text("CONNECT") }
                        }
                    }
                }
            }

            if (connected) {
                Spacer(Modifier.height(6.dp))
                UsbHostSummary()
                Spacer(Modifier.height(6.dp))
                RoutingPanel()

                Spacer(Modifier.height(7.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { clearAll() },
                        modifier = Modifier.weight(1f)
                    ) { Text("LIMPAR TUDO") }

                    Button(
                        onClick = { sendToDevice() },
                        enabled = pendingChanges && routes.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("ENVIAR AO UMH-30") }
                }

                Spacer(Modifier.height(8.dp))
                Text("LOG", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(6.dp)) {
                        logs.forEach { Text(it, fontSize = 8.sp) }
                    }
                }
            }
        }
    }

    @Composable
    private fun UsbHostSummary() {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp)) {
                Text("USB HOST", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                if (usbNames.isEmpty()) {
                    Text(
                        "Aguardando mensagens de identificação do UMH-30...",
                        fontSize = 9.sp
                    )
                } else {
                    usbNames.toSortedMap().forEach { (slot, name) ->
                        Text("USB $slot  •  $name", fontSize = 9.sp)
                    }
                }
            }
        }
    }

    @Composable
    private fun RoutingPanel() {
        val sourceList = sources()
        val destList = destinations()
        var selectedSource by remember { mutableStateOf<RouteNode?>(null) }

        BoxWithConstraints(
            Modifier.fillMaxWidth().height(500.dp)
        ) {
            val leftWidth = 108.dp
            val rightWidth = 108.dp
            val centerStart = leftWidth
            val centerEnd = maxWidth - rightWidth

            Canvas(Modifier.fillMaxSize()) {
                val rowHeight = 34.dp.toPx()
                val top = 27.dp.toPx() + rowHeight / 2
                val x1 = centerStart.toPx() + 2.dp.toPx()
                val x2 = centerEnd.toPx() - 2.dp.toPx()

                routes.forEach { (sid, did) ->
                    val si = sourceList.indexOfFirst { it.id == sid }
                    val di = destList.indexOfFirst { it.id == did }
                    if (si >= 0 && di >= 0) {
                        drawLine(
                            color = androidx.compose.ui.graphics.Color(0xFF1565C0),
                            start = Offset(x1, top + si * rowHeight),
                            end = Offset(x2, top + di * rowHeight),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Column(
                Modifier.width(leftWidth).align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("MIDI IN / USB", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                sourceList.forEach { source ->
                    Node(
                        source.label,
                        selectedSource?.id == source.id,
                        routes.any { it.first == source.id }
                    ) {
                        selectedSource = source
                        status = "${source.label} selecionado. Agora toque em um OUT."
                    }
                }
            }

            Column(
                Modifier.width(rightWidth).align(Alignment.TopEnd),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("MIDI OUT / USB", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                destList.forEach { dest ->
                    Node(
                        dest.label,
                        false,
                        routes.any { it.second == dest.id }
                    ) {
                        val source = selectedSource
                        if (source == null) {
                            status = "Primeiro selecione uma entrada."
                        } else {
                            toggle(source, dest)
                            selectedSource = null
                        }
                    }
                }
            }
        }

        Text(
            "Toque em uma entrada e depois na saída. A linha mostra a ligação.",
            fontSize = 9.sp
        )
    }

    @Composable
    private fun Node(
        label: String,
        selected: Boolean,
        connected: Boolean,
        onClick: () -> Unit
    ) {
        Surface(
            Modifier
                .fillMaxWidth()
                .height(31.dp)
                .clickable(onClick = onClick)
                .border(
                    1.dp,
                    if (selected || connected)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(5.dp)
                ),
            shape = RoundedCornerShape(5.dp),
            tonalElevation = if (selected) 4.dp else 0.dp
        ) {
            Box(
                Modifier.fillMaxSize().padding(horizontal = 5.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(label, fontSize = 7.sp, maxLines = 1)
            }
        }
    }
}
