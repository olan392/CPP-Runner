package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.BuiltAppEntity
import com.example.data.model.ConsoleLine
import com.example.data.model.OutputType
import com.example.data.model.RunnerState
import com.example.data.model.VariableInfo
import com.example.engine.CppInterpreter
import com.example.engine.CppOnlineEngine
import com.example.engine.CppParser
import com.example.engine.CppTemplate
import com.example.engine.CppTemplates
import com.example.engine.CppTokenizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EngineType {
    ON_DEVICE,
    CLOUD_GCC
}

sealed interface Screen {
    object Library : Screen
    object Editor : Screen
    object Runner : Screen
    object ApkBuilder : Screen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val appDao = db.builtAppDao()
    private val interpreter = CppInterpreter()
    private val onlineEngine = CppOnlineEngine()

    // Apps in Library
    val builtApps: StateFlow<List<BuiltAppEntity>> = appDao.getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Library)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Editor state
    val sourceCode = MutableStateFlow(CppTemplates.templates.first().code)
    val editingAppName = MutableStateFlow("Number Guessing Game")
    val editingAppDescription = MutableStateFlow("Interactive secret number game in C++")
    val editingCategory = MutableStateFlow("Game")
    val editingIconKey = MutableStateFlow("sports_esports")
    val editingColorHex = MutableStateFlow("#00D4FF")
    val editingDisplayMode = MutableStateFlow("TERMINAL")
    val currentEditingAppId = MutableStateFlow<Long?>(null)

    // Runner state
    private val _terminalLines = MutableStateFlow<List<ConsoleLine>>(emptyList())
    val terminalLines: StateFlow<List<ConsoleLine>> = _terminalLines.asStateFlow()

    private val _runnerState = MutableStateFlow<RunnerState>(RunnerState.Idle)
    val runnerState: StateFlow<RunnerState> = _runnerState.asStateFlow()

    private val _activeDisplayMode = MutableStateFlow("TERMINAL")
    val activeDisplayMode: StateFlow<String> = _activeDisplayMode.asStateFlow()

    private val _activeEngine = MutableStateFlow(EngineType.ON_DEVICE)
    val activeEngine: StateFlow<EngineType> = _activeEngine.asStateFlow()

    private val _activeApp = MutableStateFlow<BuiltAppEntity?>(null)
    val activeApp: StateFlow<BuiltAppEntity?> = _activeApp.asStateFlow()

    private val _variables = MutableStateFlow<List<VariableInfo>>(emptyList())
    val variables: StateFlow<List<VariableInfo>> = _variables.asStateFlow()

    // Async input continuation
    private var inputDeferred: CompletableDeferred<String>? = null
    private var executionJob: Job? = null

    init {
        // Seed database with starter built apps if empty
        viewModelScope.launch {
            builtApps.collect { list ->
                if (list.isEmpty()) {
                    CppTemplates.templates.take(3).forEach { tmpl ->
                        appDao.insertApp(
                            BuiltAppEntity(
                                name = tmpl.title,
                                description = tmpl.description,
                                category = tmpl.category,
                                iconKey = tmpl.iconKey,
                                colorHex = tmpl.colorHex,
                                sourceCode = tmpl.code,
                                displayMode = tmpl.defaultDisplayMode
                            )
                        )
                    }
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun loadTemplate(template: CppTemplate) {
        sourceCode.value = template.code
        editingAppName.value = template.title
        editingAppDescription.value = template.description
        editingCategory.value = template.category
        editingIconKey.value = template.iconKey
        editingColorHex.value = template.colorHex
        editingDisplayMode.value = template.defaultDisplayMode
        currentEditingAppId.value = null
        navigateTo(Screen.Editor)
    }

    fun editExistingApp(app: BuiltAppEntity) {
        currentEditingAppId.value = app.id
        sourceCode.value = app.sourceCode
        editingAppName.value = app.name
        editingAppDescription.value = app.description
        editingCategory.value = app.category
        editingIconKey.value = app.iconKey
        editingColorHex.value = app.colorHex
        editingDisplayMode.value = app.displayMode
        navigateTo(Screen.Editor)
    }

    fun launchApp(app: BuiltAppEntity) {
        _activeApp.value = app
        _activeDisplayMode.value = app.displayMode
        navigateTo(Screen.Runner)
        viewModelScope.launch {
            appDao.incrementRunCount(app.id)
        }
        startExecution(app.sourceCode)
    }

    fun testRunFromEditor() {
        val app = BuiltAppEntity(
            name = editingAppName.value.ifBlank { "Untitled C++ App" },
            description = editingAppDescription.value,
            category = editingCategory.value,
            iconKey = editingIconKey.value,
            colorHex = editingColorHex.value,
            sourceCode = sourceCode.value,
            displayMode = editingDisplayMode.value
        )
        _activeApp.value = app
        _activeDisplayMode.value = editingDisplayMode.value
        navigateTo(Screen.Runner)
        startExecution(sourceCode.value)
    }

    fun buildCurrentCodeIntoApp(
        name: String,
        description: String,
        category: String,
        iconKey: String,
        colorHex: String,
        displayMode: String,
        launchImmediately: Boolean
    ) {
        viewModelScope.launch {
            val existingId = currentEditingAppId.value
            val entity = BuiltAppEntity(
                id = existingId ?: 0,
                name = name,
                description = description,
                category = category,
                iconKey = iconKey,
                colorHex = colorHex,
                sourceCode = sourceCode.value,
                displayMode = displayMode
            )
            val newId = if (existingId != null) {
                appDao.updateApp(entity)
                existingId
            } else {
                appDao.insertApp(entity)
            }
            currentEditingAppId.value = newId

            if (launchImmediately) {
                launchApp(entity.copy(id = newId))
            } else {
                navigateTo(Screen.Library)
            }
        }
    }

    fun deleteApp(app: BuiltAppEntity) {
        viewModelScope.launch {
            appDao.deleteApp(app)
        }
    }

    fun setEngineType(engine: EngineType) {
        _activeEngine.value = engine
    }

    fun setDisplayMode(mode: String) {
        _activeDisplayMode.value = mode
    }

    fun restartExecution() {
        val code = _activeApp.value?.sourceCode ?: sourceCode.value
        startExecution(code)
    }

    fun stopExecution() {
        executionJob?.cancel()
        inputDeferred?.cancel()
        inputDeferred = null
        _runnerState.value = RunnerState.Finished(exitCode = -1, executionTimeMs = 0, stepsCount = 0)
        appendLine("[Process terminated by user]", OutputType.SystemInfo)
    }

    fun clearTerminal() {
        _terminalLines.value = emptyList()
    }

    fun submitInput(input: String) {
        val deferred = inputDeferred
        if (deferred != null && deferred.isActive) {
            deferred.complete(input)
            inputDeferred = null
            _runnerState.value = RunnerState.Running
        }
    }

    private fun startExecution(code: String) {
        executionJob?.cancel()
        inputDeferred?.cancel()
        inputDeferred = null

        _terminalLines.value = listOf(
            ConsoleLine(text = "=== Starting ${_activeApp.value?.name ?: "C++ App"} ===", type = OutputType.SystemInfo)
        )
        _runnerState.value = RunnerState.Compiling

        executionJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            if (_activeEngine.value == EngineType.CLOUD_GCC) {
                appendLine("[Compiling with Cloud GCC G++ Engine...]", OutputType.SystemInfo)
                val result = onlineEngine.executeOnline(code, "")
                if (result.isSuccess) {
                    appendLine(result.output, OutputType.Standard)
                    val elapsed = System.currentTimeMillis() - startTime
                    _runnerState.value = RunnerState.Finished(result.exitCode, elapsed, 1)
                    appendLine("[Process exited with code ${result.exitCode} in ${elapsed}ms]", OutputType.SystemInfo)
                } else {
                    appendLine(result.error ?: "Compilation failed", OutputType.Error)
                    _runnerState.value = RunnerState.Error(result.error ?: "Error")
                }
                return@launch
            }

            // On-device interpreter
            try {
                val tokenizer = CppTokenizer(code)
                val tokens = tokenizer.tokenize()
                val parser = CppParser(tokens)
                val program = parser.parse()

                _runnerState.value = RunnerState.Running

                val exitCode = interpreter.execute(
                    program = program,
                    onOutput = { text, type ->
                        appendLine(text, type)
                    },
                    onInputPrompt = { prompt, varName, expectedType ->
                        _runnerState.value = RunnerState.WaitingForInput(
                            promptText = prompt,
                            variableName = varName,
                            expectedType = expectedType
                        )
                        val deferred = CompletableDeferred<String>()
                        inputDeferred = deferred
                        deferred.await()
                    }
                )

                val elapsed = System.currentTimeMillis() - startTime
                _runnerState.value = RunnerState.Finished(exitCode, elapsed, 0)
                appendLine("=== Process exited with code $exitCode (${elapsed}ms) ===", OutputType.SystemInfo)
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown execution error"
                appendLine("Execution error: $msg", OutputType.Error)
                _runnerState.value = RunnerState.Error(msg)
            }
        }
    }

    private fun appendLine(text: String, type: OutputType) {
        val splitLines = text.split("\n")
        val newLines = splitLines.map { ConsoleLine(text = it, type = type) }
        _terminalLines.value = _terminalLines.value + newLines
    }
}
