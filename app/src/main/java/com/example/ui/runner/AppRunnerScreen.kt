package com.example.ui.runner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.EngineType
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TechBorder
import com.example.ui.theme.TechNavyBg
import com.example.ui.theme.TechSurface
import com.example.ui.theme.TechSurfaceVariant
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.IconHelper

@Composable
fun AppRunnerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val activeApp by viewModel.activeApp.collectAsState()
    val lines by viewModel.terminalLines.collectAsState()
    val runnerState by viewModel.runnerState.collectAsState()
    val activeDisplayMode by viewModel.activeDisplayMode.collectAsState()
    val activeEngine by viewModel.activeEngine.collectAsState()

    var showSourceDialog by remember { mutableStateOf(false) }

    val themeColor = IconHelper.parseColor(activeApp?.colorHex ?: "#00D4FF")

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("app_runner_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TechSurface)
                    .border(width = 1.dp, color = TechBorder)
            ) {
                // Top App Identity & Navigation Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("runner_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    // App Icon in color badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(themeColor.copy(alpha = 0.2f))
                            .border(1.dp, themeColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconHelper.getIcon(activeApp?.iconKey ?: "terminal"),
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeApp?.name ?: "C++ App",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeApp?.category ?: "App",
                                color = themeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = " • v1.0",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // View Source Code Button
                    IconButton(
                        onClick = { showSourceDialog = true },
                        modifier = Modifier.testTag("view_source_code_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Source Code",
                            tint = TextSecondary
                        )
                    }

                    // Clear Terminal
                    IconButton(
                        onClick = { viewModel.clearTerminal() },
                        modifier = Modifier.testTag("clear_terminal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear",
                            tint = TextSecondary
                        )
                    }

                    // Restart
                    IconButton(
                        onClick = { viewModel.restartExecution() },
                        modifier = Modifier.testTag("restart_execution_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart",
                            tint = CyanAccent
                        )
                    }
                }

                // Sub-Bar: Interface Mode Toggle (Terminal vs GUI Form) & Engine Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TechNavyBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mode Switcher Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TechSurfaceVariant)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeDisplayMode == "TERMINAL") CyanAccent else TechSurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            androidx.compose.foundation.text.BasicText(
                                text = "💻 Terminal",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeDisplayMode == "TERMINAL") TechNavyBg else TextSecondary
                                ),
                                modifier = Modifier.testTag("toggle_terminal_mode")
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeDisplayMode == "GUI_FORM") CyanAccent else TechSurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            androidx.compose.foundation.text.BasicText(
                                text = "📋 Form UI",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeDisplayMode == "GUI_FORM") TechNavyBg else TextSecondary
                                ),
                                modifier = Modifier.testTag("toggle_gui_mode")
                            )
                        }
                    }

                    // Engine Switcher
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TechSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeEngine == EngineType.ON_DEVICE) "⚡ Local VM" else "☁ GCC G++",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (activeEngine == EngineType.ON_DEVICE) NeonGreen else CyanAccent
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeDisplayMode == "TERMINAL") {
                TerminalView(
                    lines = lines,
                    runnerState = runnerState,
                    onSubmitInput = { viewModel.submitInput(it) },
                    onRestart = { viewModel.restartExecution() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                GuiFormView(
                    app = activeApp,
                    lines = lines,
                    runnerState = runnerState,
                    onSubmitInput = { viewModel.submitInput(it) },
                    onRestart = { viewModel.restartExecution() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Source Code Viewer Modal
    if (showSourceDialog) {
        val code = activeApp?.sourceCode ?: viewModel.sourceCode.value
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            containerColor = TechSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("C++ Source Code", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TerminalBg)
                        .border(1.dp, TechBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NeonGreen,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSourceDialog = false
                        activeApp?.let { viewModel.editExistingApp(it) } ?: run {
                            viewModel.navigateTo(Screen.Editor)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = TechNavyBg)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit in Studio")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSourceDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Close")
                }
            }
        )
    }
}
