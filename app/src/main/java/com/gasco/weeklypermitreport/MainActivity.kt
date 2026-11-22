package com.gasco.weeklypermitreport

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gasco.weeklypermitreport.data.local.entity.PermitCategory
import com.gasco.weeklypermitreport.domain.GascoExportEngine
import com.gasco.weeklypermitreport.ui.PermitViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val GascoColors = lightColorScheme(
    primary = Color(0xFF1E436E),
    onPrimary = Color.White,
    secondary = Color(0xFF2B76B9),
    background = Color(0xFFF6F8FB),
    surface = Color.White,
    onBackground = Color(0xFF1B365D),
    onSurface = Color(0xFF1B365D)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = GascoColors) {
                GascoPermitApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GascoPermitApp(vm: PermitViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var period by rememberSaveable { mutableStateOf(vm.defaultCurrentPeriod()) }
    var notes by rememberSaveable { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(12)
    ) { uris ->
        if (uris.isNotEmpty()) {
            vm.importPermitPhotosOffline(period, notes, uris)
            tab = 1
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                    label = { Text("Permit Intake") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text("Summary") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Code, contentDescription = null) },
                    label = { Text("Export") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Instrumentation & Control",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "GASCO · Amerya LPG Plant · Local Room DB",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
            )
            Spacer(Modifier.height(12.dp))

            when (tab) {
                0 -> {
                    OutlinedTextField(
                        value = period,
                        onValueChange = { period = it },
                        label = { Text("Report Period (DD/MM/YYYY-DD/MM/YYYY)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { period = vm.defaultCurrentPeriod() }) {
                        Text("Use current week")
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Additional Notes (Optional)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E436E)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (state.isScanningOcr) "Scanning Permit Photos..." else "Select Permit Photos (Offline OCR)")
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showManualDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("  Add Activity Manually")
                    }
                    state.statusMessage?.let { msg ->
                        Spacer(Modifier.height(12.dp))
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = Color(0xFF2B76B9))
                    }
                }

                1 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Summary of Site Activities (${state.activities.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { showManualDialog = true }) {
                            Text("+ Add")
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        val sections = GascoExportEngine.buildSections(state.activities)
                        sections.forEach { sec ->
                            item {
                                Surface(
                                    color = Color(0xFFDDE8F3),
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                                ) {
                                    Text(
                                        text = sec.heading,
                                        color = Color(0xFF1B365D),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            items(sec.items, key = { it.second.id }) { (no, act) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = no,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2B76B9),
                                        modifier = Modifier.padding(end = 10.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(act.equipment, fontWeight = FontWeight.Bold)
                                        Text(act.activity, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            act.displayDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    IconButton(onClick = { vm.deleteActivity(act) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }

                2 -> {
                    Button(
                        onClick = {
                            vm.buildShareIntent("pdf")?.let {
                                context.startActivity(Intent.createChooser(it, "Save or Share 2-Page PDF"))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E436E)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Download PDF") }
                    Spacer(Modifier.height(12.dp))
                    val report = state.activeReport
                    val latex = if (report != null) {
                        GascoExportEngine.generateLatex(report, state.activities)
                    } else {
                        "% No weekly report generated yet. Add activities from Permit Intake."
                    }
                    Surface(
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        LazyColumn(modifier = Modifier.padding(12.dp)) {
                            item {
                                Text(
                                    text = latex,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        var category by remember { mutableStateOf(PermitCategory.PM) }
        var equipment by remember { mutableStateOf("") }
        var activity by remember { mutableStateOf("") }
        val today = LocalDate.now()
        var displayDate by remember { mutableStateOf(today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }

        ModalBottomSheet(onDismissRequest = { showManualDialog = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Add Site Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PermitCategory.entries) { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.name) }
                        )
                    }
                }
                OutlinedTextField(value = equipment, onValueChange = { equipment = it }, label = { Text("Equipment (e.g., 100TT471A/B)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = displayDate, onValueChange = { displayDate = it }, label = { Text("Date (DD/MM/YYYY)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = activity, onValueChange = { activity = it }, label = { Text("Activity Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showManualDialog = false }) { Text("Cancel") }
                    Button(
                        enabled = equipment.isNotBlank() && activity.isNotBlank(),
                        onClick = {
                            val parts = displayDate.split("/")
                            val iso = if (parts.size == 3) "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}" else today.toString()
                            vm.saveManualActivity(period, category, displayDate, iso, equipment, activity)
                            showManualDialog = false
                        }
                    ) { Text("Save to Room") }
                }
            }
        }
    }
}
