package com.example.engine

sealed interface CppExpr {
    data class NumberLit(val value: Double, val isInt: Boolean) : CppExpr
    data class StringLit(val value: String) : CppExpr
    data class CharLit(val value: Char) : CppExpr
    data class BoolLit(val value: Boolean) : CppExpr
    data class VarRef(val name: String) : CppExpr
    data class BinaryOp(val left: CppExpr, val op: TokenType, val right: CppExpr) : CppExpr
    data class UnaryOp(val op: TokenType, val expr: CppExpr, val isPrefix: Boolean) : CppExpr
    data class FunctionCall(val name: String, val args: List<CppExpr>) : CppExpr
    data class ArrayAccess(val array: CppExpr, val index: CppExpr) : CppExpr
    data class Ternary(val condition: CppExpr, val thenExpr: CppExpr, val elseExpr: CppExpr) : CppExpr
}

sealed interface CppStmt {
    data class VarDecl(
        val type: String,
        val name: String,
        val initializer: CppExpr? = null,
        val arraySize: CppExpr? = null
    ) : CppStmt

    data class Assign(
        val target: CppExpr,
        val op: TokenType,
        val value: CppExpr
    ) : CppStmt

    data class ExprStmt(val expr: CppExpr) : CppStmt

    data class Cout(val expressions: List<CppExpr>) : CppStmt

    data class Cin(val targets: List<CppExpr>) : CppStmt

    data class Getline(val stream: String, val target: CppExpr) : CppStmt

    data class Block(val statements: List<CppStmt>) : CppStmt

    data class If(
        val condition: CppExpr,
        val thenBranch: CppStmt,
        val elseBranch: CppStmt?
    ) : CppStmt

    data class While(
        val condition: CppExpr,
        val body: CppStmt
    ) : CppStmt

    data class DoWhile(
        val body: CppStmt,
        val condition: CppExpr
    ) : CppStmt

    data class For(
        val init: CppStmt?,
        val condition: CppExpr?,
        val update: CppStmt?,
        val body: CppStmt
    ) : CppStmt

    data class Return(val value: CppExpr? = null) : CppStmt
    object Break : CppStmt
    object Continue : CppStmt
    data class Printf(val format: String, val args: List<CppExpr>) : CppStmt
}

data class CppFunction(
    val returnType: String,
    val name: String,
    val params: List<Pair<String, String>>, // (type, name)
    val body: CppStmt.Block
)

data class CppProgram(
    val functions: Map<String, CppFunction>,
    val globalStatements: List<CppStmt> = emptyList()
)
