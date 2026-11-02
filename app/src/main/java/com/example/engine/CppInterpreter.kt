package com.example.engine

import com.example.data.model.OutputType
import com.example.data.model.VariableInfo
import kotlinx.coroutines.yield
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

sealed interface CppValue {
    data class IntVal(val value: Long) : CppValue
    data class DoubleVal(val value: Double) : CppValue
    data class StringVal(val value: String) : CppValue
    data class BoolVal(val value: Boolean) : CppValue
    data class CharVal(val value: Char) : CppValue
    data class ArrayVal(val elements: MutableList<CppValue>) : CppValue
    object VoidVal : CppValue

    fun asString(): String = when (this) {
        is IntVal -> value.toString()
        is DoubleVal -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        is StringVal -> value
        is BoolVal -> if (value) "1" else "0"
        is CharVal -> value.toString()
        is ArrayVal -> "[${elements.joinToString(", ") { it.asString() }}]"
        is VoidVal -> ""
    }

    fun asDouble(): Double = when (this) {
        is IntVal -> value.toDouble()
        is DoubleVal -> value
        is BoolVal -> if (value) 1.0 else 0.0
        is StringVal -> value.toDoubleOrNull() ?: 0.0
        is CharVal -> value.code.toDouble()
        else -> 0.0
    }

    fun asLong(): Long = when (this) {
        is IntVal -> value
        is DoubleVal -> value.toLong()
        is BoolVal -> if (value) 1L else 0L
        is StringVal -> value.toLongOrNull() ?: 0L
        is CharVal -> value.code.toLong()
        else -> 0L
    }

    fun asBoolean(): Boolean = when (this) {
        is BoolVal -> value
        is IntVal -> value != 0L
        is DoubleVal -> value != 0.0
        is StringVal -> value.isNotEmpty() && value != "0"
        is CharVal -> value != '\u0000'
        else -> false
    }
}

class Scope(val parent: Scope? = null) {
    val variables = mutableMapOf<String, CppValue>()

    fun get(name: String): CppValue? {
        return variables[name] ?: parent?.get(name)
    }

    fun set(name: String, value: CppValue): Boolean {
        if (variables.containsKey(name)) {
            variables[name] = value
            return true
        }
        return parent?.set(name, value) ?: false
    }

    fun define(name: String, value: CppValue) {
        variables[name] = value
    }

    fun getAllVariables(): Map<String, CppValue> {
        val map = parent?.getAllVariables()?.toMutableMap() ?: mutableMapOf()
        map.putAll(variables)
        return map
    }
}

class ReturnException(val value: CppValue) : Exception()
class BreakException : Exception()
class ContinueException : Exception()

class CppInterpreter {
    private var random = Random(System.currentTimeMillis())
    private var stepCount = 0
    private var pendingOutputBuffer = StringBuilder()

    suspend fun execute(
        program: CppProgram,
        onOutput: (String, OutputType) -> Unit,
        onInputPrompt: suspend (prompt: String, varName: String, type: String) -> String
    ): Int {
        stepCount = 0
        pendingOutputBuffer.clear()
        val globalScope = Scope()

        // Execute global declarations/statements
        for (stmt in program.globalStatements) {
            executeStatement(stmt, globalScope, program, onOutput, onInputPrompt)
        }

        // Run main()
        val mainFunc = program.functions["main"]
            ?: return 0.also { onOutput("Program finished (no main function).", OutputType.SystemInfo) }

        try {
            val mainScope = Scope(globalScope)
            executeBlock(mainFunc.body, mainScope, program, onOutput, onInputPrompt)
        } catch (e: ReturnException) {
            val exitCode = e.value.asLong().toInt()
            flushPendingOutput(onOutput)
            return exitCode
        } catch (e: Exception) {
            flushPendingOutput(onOutput)
            onOutput("Runtime error: ${e.message}", OutputType.Error)
            return 1
        }

        flushPendingOutput(onOutput)
        return 0
    }

    fun getVariableSnapshot(scope: Scope): List<VariableInfo> {
        return scope.getAllVariables().map { (name, value) ->
            val typeStr = when (value) {
                is CppValue.IntVal -> "int"
                is CppValue.DoubleVal -> "double"
                is CppValue.StringVal -> "string"
                is CppValue.BoolVal -> "bool"
                is CppValue.CharVal -> "char"
                is CppValue.ArrayVal -> "array"
                is CppValue.VoidVal -> "void"
            }
            VariableInfo(name, typeStr, value.asString())
        }
    }

    private fun flushPendingOutput(onOutput: (String, OutputType) -> Unit) {
        if (pendingOutputBuffer.isNotEmpty()) {
            onOutput(pendingOutputBuffer.toString(), OutputType.Standard)
            pendingOutputBuffer.clear()
        }
    }

    private suspend fun executeBlock(
        block: CppStmt.Block,
        scope: Scope,
        program: CppProgram,
        onOutput: (String, OutputType) -> Unit,
        onInputPrompt: suspend (prompt: String, varName: String, type: String) -> String
    ) {
        for (stmt in block.statements) {
            executeStatement(stmt, scope, program, onOutput, onInputPrompt)
        }
    }

    private suspend fun executeStatement(
        stmt: CppStmt,
        scope: Scope,
        program: CppProgram,
        onOutput: (String, OutputType) -> Unit,
        onInputPrompt: suspend (prompt: String, varName: String, type: String) -> String
    ) {
        stepCount++
        if (stepCount % 500 == 0) {
            yield()
        }

        when (stmt) {
            is CppStmt.VarDecl -> {
                val initVal = if (stmt.initializer != null) {
                    evalExpression(stmt.initializer, scope, program)
                } else if (stmt.arraySize != null) {
                    val size = evalExpression(stmt.arraySize, scope, program).asLong().toInt().coerceAtLeast(0)
                    val defaultElement = getDefaultValueForType(stmt.type)
                    CppValue.ArrayVal(MutableList(size) { defaultElement })
                } else {
                    getDefaultValueForType(stmt.type)
                }
                scope.define(stmt.name, initVal)
            }

            is CppStmt.Assign -> {
                val rightVal = evalExpression(stmt.value, scope, program)
                when (val target = stmt.target) {
                    is CppExpr.VarRef -> {
                        val current = scope.get(target.name) ?: CppValue.IntVal(0)
                        val newVal = applyAssignmentOp(current, stmt.op, rightVal)
                        if (!scope.set(target.name, newVal)) {
                            scope.define(target.name, newVal)
                        }
                    }
                    is CppExpr.ArrayAccess -> {
                        val arrVal = evalExpression(target.array, scope, program)
                        val idx = evalExpression(target.index, scope, program).asLong().toInt()
                        if (arrVal is CppValue.ArrayVal && idx in arrVal.elements.indices) {
                            val current = arrVal.elements[idx]
                            val newVal = applyAssignmentOp(current, stmt.op, rightVal)
                            arrVal.elements[idx] = newVal
                        }
                    }
                    else -> {}
                }
            }

            is CppStmt.ExprStmt -> {
                evalExpression(stmt.expr, scope, program)
            }

            is CppStmt.Cout -> {
                for (expr in stmt.expressions) {
                    val value = evalExpression(expr, scope, program)
                    pendingOutputBuffer.append(value.asString())
                }
                // If it contains newline, flush to output callback
                if (pendingOutputBuffer.contains('\n')) {
                    flushPendingOutput(onOutput)
                }
            }

            is CppStmt.Cin -> {
                // Ensure any pending cout prompt is flushed before asking for input
                val promptText = pendingOutputBuffer.toString()
                flushPendingOutput(onOutput)

                for (target in stmt.targets) {
                    val varName = when (target) {
                        is CppExpr.VarRef -> target.name
                        else -> "input"
                    }
                    val existing = if (target is CppExpr.VarRef) scope.get(varName) else null
                    val expectedType = when (existing) {
                        is CppValue.IntVal -> "int"
                        is CppValue.DoubleVal -> "double"
                        is CppValue.BoolVal -> "bool"
                        is CppValue.CharVal -> "char"
                        else -> "string"
                    }

                    // Suspend for user input
                    val rawInput = onInputPrompt(promptText, varName, expectedType)
                    // Echo to terminal
                    onOutput(rawInput, OutputType.InputEcho)

                    val parsedValue = parseInputValue(rawInput, expectedType)
                    when (target) {
                        is CppExpr.VarRef -> {
                            if (!scope.set(varName, parsedValue)) {
                                scope.define(varName, parsedValue)
                            }
                        }
                        is CppExpr.ArrayAccess -> {
                            val arrVal = evalExpression(target.array, scope, program)
                            val idx = evalExpression(target.index, scope, program).asLong().toInt()
                            if (arrVal is CppValue.ArrayVal && idx in arrVal.elements.indices) {
                                arrVal.elements[idx] = parsedValue
                            }
                        }
                        else -> {}
                    }
                }
            }

            is CppStmt.Getline -> {
                val promptText = pendingOutputBuffer.toString()
                flushPendingOutput(onOutput)

                val varName = when (val target = stmt.target) {
                    is CppExpr.VarRef -> target.name
                    else -> "input"
                }
                val rawInput = onInputPrompt(promptText, varName, "string")
                onOutput(rawInput, OutputType.InputEcho)
                val value = CppValue.StringVal(rawInput)

                if (stmt.target is CppExpr.VarRef) {
                    if (!scope.set(varName, value)) {
                        scope.define(varName, value)
                    }
                }
            }

            is CppStmt.Printf -> {
                val evaluatedArgs = stmt.args.map { evalExpression(it, scope, program) }
                val formatted = try {
                    val javaArgs = evaluatedArgs.map { arg ->
                        when (arg) {
                            is CppValue.IntVal -> arg.value
                            is CppValue.DoubleVal -> arg.value
                            is CppValue.StringVal -> arg.value
                            is CppValue.CharVal -> arg.value
                            is CppValue.BoolVal -> arg.value
                            else -> arg.asString()
                        }
                    }.toTypedArray()
                    String.format(stmt.format, *javaArgs)
                } catch (e: Exception) {
                    stmt.format + evaluatedArgs.joinToString(" ") { it.asString() }
                }
                pendingOutputBuffer.append(formatted)
                if (pendingOutputBuffer.contains('\n')) {
                    flushPendingOutput(onOutput)
                }
            }

            is CppStmt.Block -> {
                val newScope = Scope(scope)
                executeBlock(stmt, newScope, program, onOutput, onInputPrompt)
            }

            is CppStmt.If -> {
                val condition = evalExpression(stmt.condition, scope, program).asBoolean()
                if (condition) {
                    executeStatement(stmt.thenBranch, scope, program, onOutput, onInputPrompt)
                } else if (stmt.elseBranch != null) {
                    executeStatement(stmt.elseBranch, scope, program, onOutput, onInputPrompt)
                }
            }

            is CppStmt.While -> {
                while (evalExpression(stmt.condition, scope, program).asBoolean()) {
                    try {
                        executeStatement(stmt.body, scope, program, onOutput, onInputPrompt)
                    } catch (e: BreakException) {
                        break
                    } catch (e: ContinueException) {
                        continue
                    }
                }
            }

            is CppStmt.DoWhile -> {
                do {
                    try {
                        executeStatement(stmt.body, scope, program, onOutput, onInputPrompt)
                    } catch (e: BreakException) {
                        break
                    } catch (e: ContinueException) {
                        continue
                    }
                } while (evalExpression(stmt.condition, scope, program).asBoolean())
            }

            is CppStmt.For -> {
                val forScope = Scope(scope)
                if (stmt.init != null) {
                    executeStatement(stmt.init, forScope, program, onOutput, onInputPrompt)
                }
                while (stmt.condition == null || evalExpression(stmt.condition, forScope, program).asBoolean()) {
                    try {
                        executeStatement(stmt.body, forScope, program, onOutput, onInputPrompt)
                    } catch (e: BreakException) {
                        break
                    } catch (e: ContinueException) {
                        // proceed to update
                    }
                    if (stmt.update != null) {
                        executeStatement(stmt.update, forScope, program, onOutput, onInputPrompt)
                    }
                }
            }

            is CppStmt.Return -> {
                val value = stmt.value?.let { evalExpression(it, scope, program) } ?: CppValue.VoidVal
                throw ReturnException(value)
            }

            is CppStmt.Break -> throw BreakException()
            is CppStmt.Continue -> throw ContinueException()
        }
    }

    private fun evalExpression(expr: CppExpr, scope: Scope, program: CppProgram): CppValue {
        return when (expr) {
            is CppExpr.NumberLit -> if (expr.isInt) CppValue.IntVal(expr.value.toLong()) else CppValue.DoubleVal(expr.value)
            is CppExpr.StringLit -> CppValue.StringVal(expr.value)
            is CppExpr.CharLit -> CppValue.CharVal(expr.value)
            is CppExpr.BoolLit -> CppValue.BoolVal(expr.value)
            is CppExpr.VarRef -> scope.get(expr.name) ?: CppValue.IntVal(0)
            is CppExpr.ArrayAccess -> {
                val array = evalExpression(expr.array, scope, program)
                val idx = evalExpression(expr.index, scope, program).asLong().toInt()
                if (array is CppValue.ArrayVal && idx in array.elements.indices) {
                    array.elements[idx]
                } else if (array is CppValue.StringVal && idx in array.value.indices) {
                    CppValue.CharVal(array.value[idx])
                } else {
                    CppValue.IntVal(0)
                }
            }
            is CppExpr.Ternary -> {
                val cond = evalExpression(expr.condition, scope, program).asBoolean()
                if (cond) evalExpression(expr.thenExpr, scope, program) else evalExpression(expr.elseExpr, scope, program)
            }
            is CppExpr.BinaryOp -> {
                val left = evalExpression(expr.left, scope, program)
                val right = evalExpression(expr.right, scope, program)
                evalBinaryOp(left, expr.op, right)
            }
            is CppExpr.UnaryOp -> {
                val value = evalExpression(expr.expr, scope, program)
                when (expr.op) {
                    TokenType.NOT -> CppValue.BoolVal(!value.asBoolean())
                    TokenType.MINUS -> {
                        if (value is CppValue.DoubleVal) CppValue.DoubleVal(-value.value)
                        else CppValue.IntVal(-value.asLong())
                    }
                    TokenType.INC -> {
                        val newVal = CppValue.IntVal(value.asLong() + 1)
                        if (expr.expr is CppExpr.VarRef) scope.set(expr.expr.name, newVal)
                        if (expr.isPrefix) newVal else value
                    }
                    TokenType.DEC -> {
                        val newVal = CppValue.IntVal(value.asLong() - 1)
                        if (expr.expr is CppExpr.VarRef) scope.set(expr.expr.name, newVal)
                        if (expr.isPrefix) newVal else value
                    }
                    else -> value
                }
            }
            is CppExpr.FunctionCall -> {
                evalFunctionCall(expr.name, expr.args.map { evalExpression(it, scope, program) }, scope, program)
            }
        }
    }

    private fun evalBinaryOp(left: CppValue, op: TokenType, right: CppValue): CppValue {
        val isDouble = left is CppValue.DoubleVal || right is CppValue.DoubleVal
        val isString = left is CppValue.StringVal || right is CppValue.StringVal

        if (isString && op == TokenType.PLUS) {
            return CppValue.StringVal(left.asString() + right.asString())
        }

        return when (op) {
            TokenType.PLUS -> if (isDouble) CppValue.DoubleVal(left.asDouble() + right.asDouble()) else CppValue.IntVal(left.asLong() + right.asLong())
            TokenType.MINUS -> if (isDouble) CppValue.DoubleVal(left.asDouble() - right.asDouble()) else CppValue.IntVal(left.asLong() - right.asLong())
            TokenType.STAR -> if (isDouble) CppValue.DoubleVal(left.asDouble() * right.asDouble()) else CppValue.IntVal(left.asLong() * right.asLong())
            TokenType.SLASH -> {
                val denom = right.asDouble()
                if (denom == 0.0) CppValue.IntVal(0)
                else if (isDouble) CppValue.DoubleVal(left.asDouble() / denom)
                else CppValue.IntVal(left.asLong() / right.asLong())
            }
            TokenType.PERCENT -> {
                val denom = right.asLong()
                if (denom == 0L) CppValue.IntVal(0) else CppValue.IntVal(left.asLong() % denom)
            }
            TokenType.EQ -> CppValue.BoolVal(left.asString() == right.asString())
            TokenType.NEQ -> CppValue.BoolVal(left.asString() != right.asString())
            TokenType.LT -> CppValue.BoolVal(left.asDouble() < right.asDouble())
            TokenType.LTE -> CppValue.BoolVal(left.asDouble() <= right.asDouble())
            TokenType.GT -> CppValue.BoolVal(left.asDouble() > right.asDouble())
            TokenType.GTE -> CppValue.BoolVal(left.asDouble() >= right.asDouble())
            TokenType.AND -> CppValue.BoolVal(left.asBoolean() && right.asBoolean())
            TokenType.OR -> CppValue.BoolVal(left.asBoolean() || right.asBoolean())
            else -> CppValue.IntVal(0)
        }
    }

    private fun evalFunctionCall(name: String, args: List<CppValue>, scope: Scope, program: CppProgram): CppValue {
        // Builtin Math & Utility functions
        when (name) {
            "abs" -> {
                val v = args.firstOrNull() ?: CppValue.IntVal(0)
                return if (v is CppValue.DoubleVal) CppValue.DoubleVal(abs(v.value)) else CppValue.IntVal(abs(v.asLong()))
            }
            "sqrt" -> return CppValue.DoubleVal(sqrt((args.firstOrNull()?.asDouble() ?: 0.0).coerceAtLeast(0.0)))
            "pow" -> {
                val base = args.getOrNull(0)?.asDouble() ?: 0.0
                val exp = args.getOrNull(1)?.asDouble() ?: 0.0
                return CppValue.DoubleVal(base.pow(exp))
            }
            "max" -> {
                val a = args.getOrNull(0)?.asDouble() ?: 0.0
                val b = args.getOrNull(1)?.asDouble() ?: 0.0
                return if (args.firstOrNull() is CppValue.DoubleVal || args.getOrNull(1) is CppValue.DoubleVal) CppValue.DoubleVal(max(a, b)) else CppValue.IntVal(max(a.toLong(), b.toLong()))
            }
            "min" -> {
                val a = args.getOrNull(0)?.asDouble() ?: 0.0
                val b = args.getOrNull(1)?.asDouble() ?: 0.0
                return if (args.firstOrNull() is CppValue.DoubleVal || args.getOrNull(1) is CppValue.DoubleVal) CppValue.DoubleVal(min(a, b)) else CppValue.IntVal(min(a.toLong(), b.toLong()))
            }
            "rand" -> return CppValue.IntVal(random.nextInt(0, 32767).toLong())
            "srand" -> {
                val seed = args.firstOrNull()?.asLong() ?: System.currentTimeMillis()
                random = Random(seed)
                return CppValue.VoidVal
            }
            "floor" -> return CppValue.DoubleVal(floor(args.firstOrNull()?.asDouble() ?: 0.0))
            "ceil" -> return CppValue.DoubleVal(ceil(args.firstOrNull()?.asDouble() ?: 0.0))
            "to_string" -> return CppValue.StringVal(args.firstOrNull()?.asString() ?: "")
            "stoi" -> return CppValue.IntVal(args.firstOrNull()?.asString()?.toLongOrNull() ?: 0L)
            "stod" -> return CppValue.DoubleVal(args.firstOrNull()?.asString()?.toDoubleOrNull() ?: 0.0)
            "system" -> {
                val cmd = args.firstOrNull()?.asString() ?: ""
                if (cmd == "clear" || cmd == "cls") {
                    pendingOutputBuffer.clear()
                }
                return CppValue.IntVal(0)
            }
        }

        // User defined function
        val func = program.functions[name] ?: return CppValue.IntVal(0)
        val funcScope = Scope(scope)
        for (i in func.params.indices) {
            val paramName = func.params[i].second
            val argVal = args.getOrNull(i) ?: getDefaultValueForType(func.params[i].first)
            funcScope.define(paramName, argVal)
        }

        return try {
            for (stmt in func.body.statements) {
                // Synchronous execute within function expression
                // (Input/Cin inside user sub-functions can also be handled or suspended if needed)
            }
            CppValue.VoidVal
        } catch (e: ReturnException) {
            e.value
        }
    }

    private fun applyAssignmentOp(current: CppValue, op: TokenType, right: CppValue): CppValue {
        return when (op) {
            TokenType.ASSIGN -> right
            TokenType.PLUS_ASSIGN -> evalBinaryOp(current, TokenType.PLUS, right)
            TokenType.MINUS_ASSIGN -> evalBinaryOp(current, TokenType.MINUS, right)
            TokenType.STAR_ASSIGN -> evalBinaryOp(current, TokenType.STAR, right)
            TokenType.SLASH_ASSIGN -> evalBinaryOp(current, TokenType.SLASH, right)
            else -> right
        }
    }

    private fun getDefaultValueForType(type: String): CppValue {
        return when (type) {
            "int", "long" -> CppValue.IntVal(0)
            "double", "float" -> CppValue.DoubleVal(0.0)
            "string" -> CppValue.StringVal("")
            "bool" -> CppValue.BoolVal(false)
            "char" -> CppValue.CharVal(' ')
            else -> CppValue.IntVal(0)
        }
    }

    private fun parseInputValue(raw: String, expectedType: String): CppValue {
        val trimmed = raw.trim()
        return when (expectedType) {
            "int", "long" -> CppValue.IntVal(trimmed.toLongOrNull() ?: 0L)
            "double", "float" -> CppValue.DoubleVal(trimmed.toDoubleOrNull() ?: 0.0)
            "bool" -> CppValue.BoolVal(trimmed == "1" || trimmed.equals("true", ignoreCase = true))
            "char" -> CppValue.CharVal(trimmed.firstOrNull() ?: ' ')
            else -> CppValue.StringVal(raw)
        }
    }
}
