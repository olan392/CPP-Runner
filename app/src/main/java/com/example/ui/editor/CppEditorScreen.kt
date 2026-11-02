package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CppTemplates
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.builder.BuildAppDialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TechBorder
import com.example.ui.theme.TechNavyBg
import com.example.ui.theme.TechSurface
import com.example.ui.theme.TechSurfaceVariant
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalText
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.IconHelper

@Composable
fun CppEditorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val sourceCode by viewModel.sourceCode.collectAsState()
    val appName by viewModel.editingAppName.collectAsState()
    val appDesc by viewModel.editingAppDescription.collectAsState()
    val appCategory by viewModel.editingCategory.collectAsState()
    val appIcon by viewModel.editingIconKey.collectAsState()
    val appColor by viewModel.editingColorHex.collectAsState()
    val appDisplayMode by viewModel.editingDisplayMode.collectAsState()

    var showBuildDialog by remember { mutableStateOf(false) }
    var showTemplatesDialog by remember { mutableStateOf(false) }

    val quickTokens = listOf(
        "cin >> ", "cout << ", "<< endl;", "{", "}", "(", ")", ";",
        "int ", "string ", "double ", "for (int i = 0; i < n; i++)",
        "if ()", "return 0;", "getline(cin, );", "rand() % 100"
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("cpp_editor_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TechSurface)
                    .border(1.dp, TechBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("editor_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "C++ Code Studio",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Write C++ & compile to mobile app",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Templates picker button
                        IconButton(
                            onClick = { showTemplatesDialog = true },
                            modifier = Modifier.testTag("templates_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Templates",
                                tint = CyanAccent
                            )
                        }

                        // Test Run
                        Button(
                            onClick = { viewModel.testRunFromEditor() },
                            colors = ButtonDefaults.buttonColors(containerColor = TechSurfaceVariant, contentColor = NeonGreen),
                            modifier = Modifier.height(36.dp).testTag("editor_run_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Build Into Standalone APK
                        Button(
                            onClick = { viewModel.navigateTo(Screen.ApkBuilder) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = TechNavyBg),
                            modifier = Modifier.height(36.dp).testTag("editor_build_app_button")
                        ) {
                            Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Build APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick Token Insertion Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TechNavyBg)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTokens.forEach { token ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TechSurfaceVariant)
                                .clickable {
                                    viewModel.sourceCode.value = viewModel.sourceCode.value + token
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = token,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = CyanAccent
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TerminalBg)
        ) {
            // Editor text view with line numbers
            val lines = sourceCode.split("\n")
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(8.dp)
            ) {
                // Line numbers gutter
                Column(
                    modifier = Modifier
                        .width(36.dp)
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lines.size) {
                        Text(
                            text = "$i",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Code Input Field
                BasicTextField(
                    value = sourceCode,
                    onValueChange = { viewModel.sourceCode.value = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TerminalText,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(CyanAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cpp_code_input")
                )
            }
        }
    }

    // Build App Customization Dialog
    if (showBuildDialog) {
        BuildAppDialog(
            initialName = appName,
            initialDescription = appDesc,
            initialCategory = appCategory,
            initialIconKey = appIcon,
            initialColorHex = appColor,
            initialDisplayMode = appDisplayMode,
            onDismiss = { showBuildDialog = false },
            onBuild = { name, desc, cat, iconKey, colorHex, mode, launch ->
                showBuildDialog = false
                viewModel.buildCurrentCodeIntoApp(name, desc, cat, iconKey, colorHex, mode, launch)
            }
        )
    }

    // Templates Selection Dialog
    if (showTemplatesDialog) {
        AlertDialog(
            onDismissRequest = { showTemplatesDialog = false },
            containerColor = TechSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("C++ Starter Templates", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CppTemplates.templates.forEach { tmpl ->
                        val themeCol = IconHelper.parseColor(tmpl.colorHex)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, TechBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.loadTemplate(tmpl)
                                    showTemplatesDialog = false
                                },
                            colors = CardDefaults.cardColors(containerColor = TechSurfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(themeCol.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = IconHelper.getIcon(tmpl.iconKey),
                                        contentDescription = null,
                                        tint = themeCol,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tmpl.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = tmpl.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { showTemplatesDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
