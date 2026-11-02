package com.example.engine

enum class TokenType {
    // Keywords
    INT, LONG, FLOAT, DOUBLE, STRING, BOOL, CHAR, VOID, VECTOR, STRUCT,
    IF, ELSE, WHILE, FOR, DO, SWITCH, CASE, DEFAULT, BREAK, CONTINUE, RETURN,
    USING, NAMESPACE, STD, INCLUDE, TRUE, FALSE,

    // I/O & Libs
    CIN, COUT, ENDL, GETLINE, PRINTF,

    // Identifiers & Literals
    IDENTIFIER, NUMBER, STRING_LITERAL, CHAR_LITERAL,

    // Operators
    PLUS, MINUS, STAR, SLASH, PERCENT,
    ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, STAR_ASSIGN, SLASH_ASSIGN,
    INC, DEC,
    EQ, NEQ, LT, LTE, GT, GTE,
    AND, OR, NOT,
    SHL, SHR, // << and >>
    QUESTION, COLON,

    // Punctuation
    SEMICOLON, COMMA, DOT, ARROW,
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
    HASH,

    EOF
}

data class Token(
    val type: TokenType,
    val lexeme: String,
    val line: Int,
    val column: Int
)

class CppTokenizer(private val source: String) {
    private var pos = 0
    private var line = 1
    private var col = 1
    private val length = source.length

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (pos < length) {
            val c = source[pos]

            if (c.isWhitespace()) {
                if (c == '\n') {
                    line++
                    col = 1
                } else {
                    col++
                }
                pos++
                continue
            }

            // Comments
            if (c == '/' && peek() == '/') {
                // Single line comment
                while (pos < length && source[pos] != '\n') {
                    pos++
                }
                continue
            }

            if (c == '/' && peek() == '*') {
                // Multi line comment
                pos += 2
                col += 2
                while (pos < length - 1 && !(source[pos] == '*' && source[pos + 1] == '/')) {
                    if (source[pos] == '\n') {
                        line++
                        col = 1
                    } else {
                        col++
                    }
                    pos++
                }
                if (pos < length - 1) {
                    pos += 2
                    col += 2
                }
                continue
            }

            // Preprocessor #include or #define
            if (c == '#') {
                val startCol = col
                pos++
                col++
                val word = readIdentifier()
                if (word == "include") {
                    skipWhitespaceInline()
                    val header = readHeaderOrString()
                    tokens.add(Token(TokenType.INCLUDE, header, line, startCol))
                    continue
                } else {
                    // skip preprocessor line
                    while (pos < length && source[pos] != '\n') {
                        pos++
                    }
                    continue
                }
            }

            // String literal
            if (c == '"') {
                tokens.add(readStringLiteral())
                continue
            }

            // Char literal
            if (c == '\'') {
                tokens.add(readCharLiteral())
                continue
            }

            // Numbers
            if (c.isDigit()) {
                tokens.add(readNumber())
                continue
            }

            // Identifiers or Keywords
            if (c.isLetter() || c == '_') {
                tokens.add(readWord())
                continue
            }

            // Operators & Punctuation
            val startCol = col
            val token = when (c) {
                '+' -> when (peek()) {
                    '+' -> { advance(2); Token(TokenType.INC, "++", line, startCol) }
                    '=' -> { advance(2); Token(TokenType.PLUS_ASSIGN, "+=", line, startCol) }
                    else -> { advance(1); Token(TokenType.PLUS, "+", line, startCol) }
                }
                '-' -> when (peek()) {
                    '-' -> { advance(2); Token(TokenType.DEC, "--", line, startCol) }
                    '=' -> { advance(2); Token(TokenType.MINUS_ASSIGN, "-=", line, startCol) }
                    '>' -> { advance(2); Token(TokenType.ARROW, "->", line, startCol) }
                    else -> { advance(1); Token(TokenType.MINUS, "-", line, startCol) }
                }
                '*' -> when (peek()) {
                    '=' -> { advance(2); Token(TokenType.STAR_ASSIGN, "*=", line, startCol) }
                    else -> { advance(1); Token(TokenType.STAR, "*", line, startCol) }
                }
                '/' -> when (peek()) {
                    '=' -> { advance(2); Token(TokenType.SLASH_ASSIGN, "/=", line, startCol) }
                    else -> { advance(1); Token(TokenType.SLASH, "/", line, startCol) }
                }
                '%' -> { advance(1); Token(TokenType.PERCENT, "%", line, startCol) }
                '=' -> when (peek()) {
                    '=' -> { advance(2); Token(TokenType.EQ, "==", line, startCol) }
                    else -> { advance(1); Token(TokenType.ASSIGN, "=", line, startCol) }
                }
                '!' -> when (peek()) {
                    '=' -> { advance(2); Token(TokenType.NEQ, "!=", line, startCol) }
                    else -> { advance(1); Token(TokenType.NOT, "!", line, startCol) }
                }
                '<' -> when (peek()) {
                    '<' -> { advance(2); Token(TokenType.SHL, "<<", line, startCol) }
                    '=' -> { advance(2); Token(TokenType.LTE, "<=", line, startCol) }
                    else -> { advance(1); Token(TokenType.LT, "<", line, startCol) }
                }
                '>' -> when (peek()) {
                    '>' -> { advance(2); Token(TokenType.SHR, ">>", line, startCol) }
                    '=' -> { advance(2); Token(TokenType.GTE, ">=", line, startCol) }
                    else -> { advance(1); Token(TokenType.GT, ">", line, startCol) }
                }
                '&' -> when (peek()) {
                    '&' -> { advance(2); Token(TokenType.AND, "&&", line, startCol) }
                    else -> { advance(1); Token(TokenType.IDENTIFIER, "&", line, startCol) }
                }
                '|' -> when (peek()) {
                    '|' -> { advance(2); Token(TokenType.OR, "||", line, startCol) }
                    else -> { advance(1); Token(TokenType.IDENTIFIER, "|", line, startCol) }
                }
                '?' -> { advance(1); Token(TokenType.QUESTION, "?", line, startCol) }
                ':' -> { advance(1); Token(TokenType.COLON, ":", line, startCol) }
                ';' -> { advance(1); Token(TokenType.SEMICOLON, ";", line, startCol) }
                ',' -> { advance(1); Token(TokenType.COMMA, ",", line, startCol) }
                '.' -> { advance(1); Token(TokenType.DOT, ".", line, startCol) }
                '(' -> { advance(1); Token(TokenType.LPAREN, "(", line, startCol) }
                ')' -> { advance(1); Token(TokenType.RPAREN, ")", line, startCol) }
                '{' -> { advance(1); Token(TokenType.LBRACE, "{", line, startCol) }
                '}' -> { advance(1); Token(TokenType.RBRACE, "}", line, startCol) }
                '[' -> { advance(1); Token(TokenType.LBRACKET, "[", line, startCol) }
                ']' -> { advance(1); Token(TokenType.RBRACKET, "]", line, startCol) }
                else -> {
                    advance(1)
                    Token(TokenType.IDENTIFIER, c.toString(), line, startCol)
                }
            }
            tokens.add(token)
        }
        tokens.add(Token(TokenType.EOF, "", line, col))
        return tokens
    }

    private fun peek(): Char = if (pos + 1 < length) source[pos + 1] else '\u0000'

    private fun advance(count: Int) {
        pos += count
        col += count
    }

    private fun skipWhitespaceInline() {
        while (pos < length && (source[pos] == ' ' || source[pos] == '\t')) {
            pos++
            col++
        }
    }

    private fun readHeaderOrString(): String {
        if (pos >= length) return ""
        val delimiter = if (source[pos] == '<') '>' else '"'
        pos++
        col++
        val sb = StringBuilder()
        while (pos < length && source[pos] != delimiter && source[pos] != '\n') {
            sb.append(source[pos])
            pos++
            col++
        }
        if (pos < length && source[pos] == delimiter) {
            pos++
            col++
        }
        return sb.toString()
    }

    private fun readStringLiteral(): Token {
        val startCol = col
        pos++ // skip opening "
        col++
        val sb = StringBuilder()
        while (pos < length && source[pos] != '"') {
            if (source[pos] == '\\' && pos + 1 < length) {
                pos++
                col++
                when (source[pos]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    else -> sb.append(source[pos])
                }
            } else {
                if (source[pos] == '\n') {
                    line++
                    col = 0
                }
                sb.append(source[pos])
            }
            pos++
            col++
        }
        if (pos < length && source[pos] == '"') {
            pos++
            col++
        }
        return Token(TokenType.STRING_LITERAL, sb.toString(), line, startCol)
    }

    private fun readCharLiteral(): Token {
        val startCol = col
        pos++
        col++
        val c = if (source[pos] == '\\' && pos + 1 < length) {
            pos += 2
            col += 2
            when (source[pos - 1]) {
                'n' -> '\n'
                't' -> '\t'
                '0' -> '\u0000'
                else -> source[pos - 1]
            }
        } else {
            val char = source[pos]
            pos++
            col++
            char
        }
        if (pos < length && source[pos] == '\'') {
            pos++
            col++
        }
        return Token(TokenType.CHAR_LITERAL, c.toString(), line, startCol)
    }

    private fun readNumber(): Token {
        val startCol = col
        val sb = StringBuilder()
        var hasDot = false
        while (pos < length && (source[pos].isDigit() || (!hasDot && source[pos] == '.' && peek().isDigit()))) {
            if (source[pos] == '.') hasDot = true
            sb.append(source[pos])
            pos++
            col++
        }
        return Token(TokenType.NUMBER, sb.toString(), line, startCol)
    }

    private fun readIdentifier(): String {
        val sb = StringBuilder()
        while (pos < length && (source[pos].isLetterOrDigit() || source[pos] == '_')) {
            sb.append(source[pos])
            pos++
            col++
        }
        return sb.toString()
    }

    private fun readWord(): Token {
        val startCol = col
        val word = readIdentifier()
        val type = when (word) {
            "int" -> TokenType.INT
            "long" -> TokenType.LONG
            "float" -> TokenType.FLOAT
            "double" -> TokenType.DOUBLE
            "string" -> TokenType.STRING
            "bool" -> TokenType.BOOL
            "char" -> TokenType.CHAR
            "void" -> TokenType.VOID
            "vector" -> TokenType.VECTOR
            "struct" -> TokenType.STRUCT
            "if" -> TokenType.IF
            "else" -> TokenType.ELSE
            "while" -> TokenType.WHILE
            "for" -> TokenType.FOR
            "do" -> TokenType.DO
            "switch" -> TokenType.SWITCH
            "case" -> TokenType.CASE
            "default" -> TokenType.DEFAULT
            "break" -> TokenType.BREAK
            "continue" -> TokenType.CONTINUE
            "return" -> TokenType.RETURN
            "using" -> TokenType.USING
            "namespace" -> TokenType.NAMESPACE
            "std" -> TokenType.STD
            "true" -> TokenType.TRUE
            "false" -> TokenType.FALSE
            "cin" -> TokenType.CIN
            "cout" -> TokenType.COUT
            "endl" -> TokenType.ENDL
            "getline" -> TokenType.GETLINE
            "printf" -> TokenType.PRINTF
            else -> TokenType.IDENTIFIER
        }
        return Token(type, word, line, startCol)
    }
}
