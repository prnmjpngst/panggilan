package com.prnmjpngst.pemanggilan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.prnmjpngst.pemanggilan.service.AnnouncerService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startService(Intent(this, AnnouncerService::class.java))
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    val cfg by ctx.configFlow().collectAsState(initial = AppConfig())
    var screen by remember { mutableStateOf("home") }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var statusText by remember { mutableStateOf("Memuat...") }
    val scope = rememberCoroutineScope()

    val refresh: () -> Unit = {
        scope.launch {
            try {
                val api = createApi(cfg.baseUrl, cfg.apiKey)
                val r = api.vehicles()
                if (r.isSuccessful) {
                    vehicles = (r.body()?.items ?: emptyList()).map { it.toVehicle() }
                    statusText = "Sync: ${r.body()?.lastSync ?: "-"} (${vehicles.size} data)"
                } else statusText = "Error ${r.code()}"
            } catch (e: Exception) {
                statusText = "Gagal: ${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (screen == "home") "Pemanggilan" else "Konfigurasi") })
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { screen = "home" }) { Text("Home") }
                Button(onClick = { screen = "config" }) { Text("Config") }
                Button(onClick = { refresh() }) { Text("Refresh") }
            }
            Text(statusText, Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(8.dp))
            if (screen == "home") HomeScreen(cfg, vehicles, refresh)
            else ConfigScreen(cfg) { newCfg ->
                scope.launch { ctx.saveConfig(newCfg) }
            }
        }
    }
}

@Composable
fun HomeScreen(cfg: AppConfig, vehicles: List<Vehicle>, refresh: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(vehicles, key = { it.id }) { v ->
            val plate = v.field(cfg, "plate")
            val result = v.field(cfg, "result")
            val gagal = isGagal(v, cfg)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("$plate  —  $result", style = MaterialTheme.typography.titleMedium)
                    Text("Status: ${v.status}  (called ${v.calledCount}x)", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                val clips = buildClips(plate, gagal)
                                AudioPlayer.playSequence(ctx, clips)
                                try { createApi(cfg.baseUrl, cfg.apiKey).call(v.id) } catch (_: Exception) {}
                                refresh()
                            }
                        }) { Text("Panggil") }
                        if (v.status != "done") {
                            Button(onClick = {
                                scope.launch {
                                    try { createApi(cfg.baseUrl, cfg.apiKey).done(v.id) } catch (_: Exception) {}
                                    refresh()
                                }
                            }) { Text("Selesai") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigScreen(cfg: AppConfig, onSave: (AppConfig) -> Unit) {
    var baseUrl by remember { mutableStateOf(cfg.baseUrl) }
    var apiKey by remember { mutableStateOf(cfg.apiKey) }
    var poll by remember { mutableStateOf(cfg.pollIntervalSec.toString()) }
    var repeat by remember { mutableStateOf(cfg.repeatCount.toString()) }
    var delay by remember { mutableStateOf(cfg.repeatDelaySec.toString()) }
    var autoGagal by remember { mutableStateOf(cfg.autoPlayGagal) }
    var autoLulus by remember { mutableStateOf(cfg.autoPlayLulus) }
    var plateCol by remember { mutableStateOf(cfg.plateColumn) }
    var idCol by remember { mutableStateOf(cfg.idColumn) }
    var resultCol by remember { mutableStateOf(cfg.resultColumn) }
    var ownerCol by remember { mutableStateOf(cfg.ownerColumn) }
    var gagalVal by remember { mutableStateOf(cfg.gagalValue) }
    var lulusVal by remember { mutableStateOf(cfg.lulusValue) }

    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        @Composable
        fun field(label: String, value: String, onChange: (String) -> Unit) {
            OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
        }
        field("Base URL", baseUrl) { baseUrl = it }
        field("API Key", apiKey) { apiKey = it }
        field("Poll interval (detik)", poll) { poll = it }
        field("Repeat count", repeat) { repeat = it }
        field("Repeat delay (detik)", delay) { delay = it }
        field("Plate column", plateCol) { plateCol = it }
        field("ID column", idCol) { idCol = it }
        field("Result column", resultCol) { resultCol = it }
        field("Owner column", ownerCol) { ownerCol = it }
        field("Gagal value", gagalVal) { gagalVal = it }
        field("Lulus value", lulusVal) { lulusVal = it }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            androidx.compose.material3.Checkbox(checked = autoGagal, onCheckedChange = { autoGagal = it })
            Text("Auto play gagal")
            androidx.compose.material3.Checkbox(checked = autoLulus, onCheckedChange = { autoLulus = it })
            Text("Auto play lulus")
        }
        Button(onClick = {
            onSave(
                AppConfig(
                    baseUrl = baseUrl.trim(),
                    apiKey = apiKey.trim(),
                    pollIntervalSec = poll.toIntOrNull() ?: 30,
                    repeatCount = repeat.toIntOrNull() ?: 2,
                    repeatDelaySec = delay.toIntOrNull() ?: 5,
                    autoPlayGagal = autoGagal,
                    autoPlayLulus = autoLulus,
                    plateColumn = plateCol.trim(),
                    idColumn = idCol.trim(),
                    resultColumn = resultCol.trim(),
                    ownerColumn = ownerCol.trim(),
                    gagalValue = gagalVal.trim(),
                    lulusValue = lulusVal.trim(),
                )
            )
        }) { Text("Simpan") }
    }
}