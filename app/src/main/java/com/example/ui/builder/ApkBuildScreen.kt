package com.example.ui.builder

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.builder.ApkBuildProgress
import com.example.builder.ApkBuilder
import com.example.builder.GeneratedApkInfo
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TechBorder
import com.example.ui.theme.TechNavyBg
import com.example.ui.theme.TechSurface
import com.example.ui.theme.TechSurfaceVariant
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.IconHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApkBuildScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val apkBuilder = remember { ApkBuilder(context) }

    val sourceCode by viewModel.sourceCode.collectAsState()
    var appName by remember { mutableStateOf(viewModel.editingAppName.value.ifBlank { "My C++ App" }) }
    var packageName by remember {
        val clean = appName.lowercase().replace(Regex("[^a-z0-9]"), "")
        mutableStateOf("com.cppapp.${if (clean.isBlank()) "app" else clean}")
    }
    var versionName by remember { mutableStateOf("1.0.0") }
    var selectedIcon by remember { mutableStateOf(viewModel.editingIconKey.value) }
    var selectedColor by remember { mutableStateOf(viewModel.editingColorHex.value) }
    var displayMode by remember { mutableStateOf(viewModel.editingDisplayMode.value) }

    var isBuilding by remember { mutableStateOf(false) }
    var buildProgress by remember { mutableStateOf<ApkBuildProgress?>(null) }
    var generatedApkInfo by remember { mutableStateOf<GeneratedApkInfo?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("apk_build_screen"),
        containerColor = TechNavyBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TechSurface)
                    .border(1.dp, TechBorder)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(text = "C++ to APK Compiler", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Text(text = "Package C++ directly into standalone Android APK", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Packaging Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TechSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, TechBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonGreen.copy(alpha = 0.2f))
                                .border(1.dp, NeonGreen, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(text = "Turn C++ into Standalone APK", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                            Text(text = "Compiles code and bundles the interactive cin/cout runtime into a ready APK package", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // App Name
                    OutlinedTextField(
                        value = appName,
                        onValueChange = {
                            appName = it
                            val clean = it.lowercase().replace(Regex("[^a-z0-9]"), "")
                            packageName = "com.cppapp.${if (clean.isBlank()) "app" else clean}"
                        },
                        label = { Text("App Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = TechBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("apk_app_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Package Name & Version
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = packageName,
                            onValueChange = { packageName = it },
                            label = { Text("Package ID") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = TechBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(2f)
                        )

                        OutlinedTextField(
                            value = versionName,
                            onValueChange = { versionName = it },
                            label = { Text("Version") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = TechBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // App Launcher Icon
                    Text("Launcher Icon", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconHelper.availableIcons.forEach { (key, vector) ->
                            val isSelected = selectedIcon == key
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CyanAccent.copy(alpha = 0.25f) else TechSurfaceVariant)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) CyanAccent else TechBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedIcon = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = key,
                                    tint = if (isSelected) CyanAccent else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Theme Color
                    Text("Theme Accent", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconHelper.availableColors.forEach { hex ->
                            val col = IconHelper.parseColor(hex)
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) TextPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TechNavyBg, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Display Interface Mode
                    Text("Runtime Interface Mode", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = displayMode == "TERMINAL",
                            onClick = { displayMode = "TERMINAL" },
                            label = { Text("💻 Terminal Console") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                selectedLabelColor = CyanAccent,
                                containerColor = TechSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                        FilterChip(
                            selected = displayMode == "GUI_FORM",
                            onClick = { displayMode = "GUI_FORM" },
                            label = { Text("📋 Visual Form") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                                selectedLabelColor = CyanAccent,
                                containerColor = TechSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Build APK Button
                    Button(
                        onClick = {
                            isBuilding = true
                            generatedApkInfo = null
                            scope.launch {
                                try {
                                    val info = apkBuilder.buildApk(
                                        appName = appName,
                                        packageName = packageName,
                                        versionName = versionName,
                                        sourceCode = sourceCode,
                                        iconKey = selectedIcon,
                                        colorHex = selectedColor,
                                        displayMode = displayMode,
                                        onProgress = { buildProgress = it }
                                    )
                                    generatedApkInfo = info
                                    // Also save to library
                                    viewModel.buildCurrentCodeIntoApp(
                                        name = appName,
                                        description = "Standalone C++ APK",
                                        category = "Tool",
                                        iconKey = selectedIcon,
                                        colorHex = selectedColor,
                                        displayMode = displayMode,
                                        launchImmediately = false
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Build failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isBuilding = false
                                }
                            }
                        },
                        enabled = !isBuilding,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = TechNavyBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_apk_build_button")
                    ) {
                        if (isBuilding) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TechNavyBg, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Compiling & Building APK...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        } else {
                            Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("🔨 Build Standalone APK", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Real-time Progress Card
            AnimatedVisibility(visible = isBuilding && buildProgress != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TechSurfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Step ${buildProgress?.step ?: 1} of ${buildProgress?.totalSteps ?: 5}",
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${((buildProgress?.step ?: 1) * 20)}%",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { ((buildProgress?.step ?: 1) / 5f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = CyanAccent,
                            trackColor = TechSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = buildProgress?.message ?: "Packaging...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Success Output Card
            generatedApkInfo?.let { info ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, NeonGreen, RoundedCornerShape(16.dp))
                        .testTag("apk_success_card"),
                    colors = CardDefaults.cardColors(containerColor = TechSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(30.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "APK Package Built Successfully!", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                                Text(text = "Standalone Android APK ready to install or export", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // APK File Metadata
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(TerminalBg)
                                .border(1.dp, TechBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "FILE: ${info.file.name}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NeonGreen)
                                Text(text = "PACKAGE: ${info.packageName}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)
                                Text(text = "SIZE: ${info.fileSizeFormatted}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)
                                Text(text = "LOCATION: ROOT/build/${info.file.name}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyanAccent)
                                Text(text = "PATH: ${info.file.absolutePath}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { apkBuilder.launchInstaller(info) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = TechNavyBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(46.dp).testTag("install_apk_button")
                            ) {
                                Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Install APK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { apkBuilder.shareApk(info) },
                                colors = ButtonDefaults.buttonColors(containerColor = TechSurfaceVariant, contentColor = TextPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(46.dp).testTag("share_apk_button")
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share APK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("/build/${info.file.name}"))
                                    Toast.makeText(context, "Copied /build/${info.file.name} to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Path", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.testRunFromEditor()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = TechNavyBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test In-App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
