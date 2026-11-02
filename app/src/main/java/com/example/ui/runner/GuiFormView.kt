package com.example.ui.runner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.BuiltAppEntity
import com.example.data.model.ConsoleLine
import com.example.data.model.OutputType
import com.example.data.model.RunnerState
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TechBorder
import com.example.ui.theme.TechNavyBg
import com.example.ui.theme.TechSurface
import com.example.ui.theme.TechSurfaceVariant
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.util.IconHelper

@Composable
fun GuiFormView(
    app: BuiltAppEntity?,
    lines: List<ConsoleLine>,
    runnerState: RunnerState,
    onSubmitInput: (String) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var formInput by remember { mutableStateOf("") }
    val themeColor = IconHelper.parseColor(app?.colorHex ?: "#00D4FF")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TechNavyBg)
            .padding(16.dp)
            .testTag("gui_form_view")
    ) {
        // App Identity Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TechSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(themeColor.copy(alpha = 0.2f))
                        .border(1.dp, themeColor, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = IconHelper.getIcon(app?.iconKey ?: "terminal"),
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app?.name ?: "C++ Application",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = app?.description ?: "Interactive C++ App Interface",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Active Interactive Input Form Card
        AnimatedVisibility(
            visible = runnerState is RunnerState.WaitingForInput,
            enter = fadeIn() + slideInVertically()
        ) {
            val waiting = runnerState as? RunnerState.WaitingForInput
            val prompt = waiting?.promptText?.trim()?.ifEmpty { "Enter value for ${waiting.variableName}:" }
                ?: "Enter input value:"
            val expectedType = waiting?.expectedType ?: "string"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, themeColor, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = TechSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(themeColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = expectedType,
                                fontSize = 11.sp,
                                color = themeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = formInput,
                        onValueChange = { formInput = it },
                        placeholder = { Text("Enter your response...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (expectedType == "int" || expectedType == "double") KeyboardType.Number else KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (formInput.isNotEmpty()) {
                                    onSubmitInput(formInput)
                                    formInput = ""
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = TechBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gui_form_input_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (formInput.isNotEmpty()) {
                                onSubmitInput(formInput)
                                formInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = TechNavyBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("gui_form_submit_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submit to App", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Output Stream Display Cards
        Text(
            text = "App Output Stream",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val validLines = lines.filter { it.text.isNotBlank() }
            if (validLines.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TechSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("No outputs recorded yet. App is initializing...", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }

            items(validLines.reversed(), key = { it.id }) { line ->
                val cardColor = when (line.type) {
                    OutputType.InputEcho -> TerminalYellow.copy(alpha = 0.1f)
                    OutputType.Error -> TerminalRed.copy(alpha = 0.15f)
                    OutputType.SystemInfo -> CyanAccent.copy(alpha = 0.1f)
                    OutputType.Standard -> TechSurface
                }

                val borderColor = when (line.type) {
                    OutputType.InputEcho -> TerminalYellow.copy(alpha = 0.4f)
                    OutputType.Error -> TerminalRed.copy(alpha = 0.4f)
                    OutputType.SystemInfo -> CyanAccent.copy(alpha = 0.4f)
                    OutputType.Standard -> TechBorder
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (line.type) {
                            OutputType.InputEcho -> Icons.AutoMirrored.Filled.Send
                            OutputType.Error -> Icons.Default.Warning
                            OutputType.SystemInfo -> Icons.Default.Info
                            OutputType.Standard -> Icons.Default.CheckCircle
                        }
                        val iconTint = when (line.type) {
                            OutputType.InputEcho -> TerminalYellow
                            OutputType.Error -> TerminalRed
                            OutputType.SystemInfo -> CyanAccent
                            OutputType.Standard -> NeonGreen
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = line.text,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Restart button if finished
        if (runnerState is RunnerState.Finished) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = themeColor, contentColor = TechNavyBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restart Program", fontWeight = FontWeight.Bold)
            }
        }
    }
}
