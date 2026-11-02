package com.example.ui.runner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConsoleLine
import com.example.data.model.OutputType
import com.example.data.model.RunnerState
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TechBorder
import com.example.ui.theme.TechNavyBg
import com.example.ui.theme.TechSurface
import com.example.ui.theme.TechSurfaceVariant
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalText
import com.example.ui.theme.TerminalYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TerminalView(
    lines: List<ConsoleLine>,
    runnerState: RunnerState,
    onSubmitInput: (String) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Auto-scroll on new lines
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    // Auto-focus input when waiting for input
    LaunchedEffect(runnerState) {
        if (runnerState is RunnerState.WaitingForInput) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBg)
            .testTag("terminal_view")
    ) {
        // Terminal Window Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TechSurface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Colored terminal window dots
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(TerminalRed))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(TerminalYellow))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(NeonGreen))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "cpp_stdout_console",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Status indicator
            when (runnerState) {
                is RunnerState.Running -> {
                    Text(
                        text = "● RUNNING",
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                is RunnerState.WaitingForInput -> {
                    val infiniteTransition = rememberInfiniteTransition()
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Text(
                        text = "● AWAITING INPUT (std::cin)",
                        color = TerminalYellow,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alpha(alpha)
                    )
                }
                is RunnerState.Finished -> {
                    Text(
                        text = "EXIT: ${runnerState.exitCode}",
                        color = if (runnerState.exitCode == 0) NeonGreen else TerminalRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                is RunnerState.Error -> {
                    Text(
                        text = "● ERROR",
                        color = TerminalRed,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                else -> {}
            }
        }

        // Terminal Console Log Lines
        SelectionContainer(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (lines.isEmpty()) {
                    item {
                        Text(
                            text = "// Terminal ready. Tap Run or launch an app.",
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                }

                items(lines, key = { it.id }) { line ->
                    val color = when (line.type) {
                        OutputType.Standard -> TerminalText
                        OutputType.InputEcho -> TerminalYellow
                        OutputType.SystemInfo -> TerminalCyan
                        OutputType.Error -> TerminalRed
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (line.type == OutputType.InputEcho) {
                            Text(
                                text = ">> ",
                                color = TerminalYellow,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = line.text,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Interactive Input Area (std::cin prompt)
        AnimatedVisibility(
            visible = runnerState is RunnerState.WaitingForInput,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
        ) {
            val waitingState = runnerState as? RunnerState.WaitingForInput
            val varPrompt = waitingState?.promptText?.trim()?.ifEmpty { "std::cin >> ${waitingState.variableName}" }
                ?: "std::cin >> input"
            val expectedType = waitingState?.expectedType ?: "string"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .border(1.dp, TerminalYellow.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = TechSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Prompt label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = TerminalYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = varPrompt,
                                color = TerminalYellow,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "type: $expectedType",
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Field & Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Type input and press Enter...",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (expectedType == "int" || expectedType == "long" || expectedType == "double")
                                    KeyboardType.Number else KeyboardType.Text,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotEmpty()) {
                                        onSubmitInput(inputText)
                                        inputText = ""
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TerminalYellow,
                                unfocusedBorderColor = TechBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = TerminalYellow
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("terminal_input_field")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotEmpty()) {
                                    onSubmitInput(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TerminalYellow)
                                .testTag("terminal_submit_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Input",
                                tint = TechNavyBg,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Quick number suggestions for numerical inputs
                    if (expectedType == "int" || expectedType == "double") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("1", "2", "3", "4", "5", "0", "10", "100").forEach { num ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TechSurfaceVariant)
                                        .clickable {
                                            onSubmitInput(num)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = num,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = CyanAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Process Finished banner
        if (runnerState is RunnerState.Finished) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TechSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Execution completed (${runnerState.executionTimeMs}ms)",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = TechNavyBg),
                    modifier = Modifier.height(34.dp).testTag("terminal_restart_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restart App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
