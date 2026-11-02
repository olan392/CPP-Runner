package com.example.data.model

sealed interface OutputType {
    object Standard : OutputType
    object Error : OutputType
    object InputEcho : OutputType
    object SystemInfo : OutputType
}

data class ConsoleLine(
    val id: Long = System.nanoTime(),
    val text: String,
    val type: OutputType = OutputType.Standard,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface RunnerState {
    object Idle : RunnerState
    object Compiling : RunnerState
    object Running : RunnerState
    data class WaitingForInput(
        val promptText: String,
        val variableName: String = "",
        val expectedType: String = "string"
    ) : RunnerState
    data class Finished(
        val exitCode: Int,
        val executionTimeMs: Long,
        val stepsCount: Int
    ) : RunnerState
    data class Error(
        val message: String,
        val lineNumber: Int? = null
    ) : RunnerState
}

data class VariableInfo(
    val name: String,
    val type: String,
    val value: String
)
