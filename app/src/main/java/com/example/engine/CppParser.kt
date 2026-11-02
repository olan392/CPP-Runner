package com.example.engine

class CppParser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): CppProgram {
        val functions = mutableMapOf<String, CppFunction>()
        val globals = mutableListOf<CppStmt>()

        while (!isAtEnd()) {
            // Skip preprocessor and using directives
            if (match(TokenType.INCLUDE)) continue
            if (match(TokenType.USING)) {
                if (match(TokenType.NAMESPACE)) {
                    match(TokenType.STD)
                }
                match(TokenType.SEMICOLON)
                continue
            }

            if (checkFunction()) {
                val func = parseFunction()
                functions[func.name] = func
            } else {
                val stmt = parseStatement()
                if (stmt != null) {
                    globals.add(stmt)
                }
            }
        }

        return CppProgram(functions, globals)
    }

    private fun checkFunction(): Boolean {
        var idx = current
        // Look for [type] [identifier] '('
        if (isTypeToken(peekAt(idx).type)) {
            idx++
            if (peekAt(idx).type == TokenType.IDENTIFIER || peekAt(idx).lexeme == "main") {
                idx++
                if (peekAt(idx).type == TokenType.LPAREN) {
                    return true
                }
            }
        }
        return false
    }

    private fun isTypeToken(type: TokenType): Boolean {
        return type in listOf(
            TokenType.INT, TokenType.LONG, TokenType.FLOAT, TokenType.DOUBLE,
            TokenType.STRING, TokenType.BOOL, TokenType.CHAR, TokenType.VOID,
            TokenType.VECTOR
        )
    }

    private fun parseFunction(): CppFunction {
        val returnTypeToken = advance()
        val returnType = returnTypeToken.lexeme
        val nameToken = advance()
        val name = nameToken.lexeme

        consume(TokenType.LPAREN, "Expected '(' after function name")
        val params = mutableListOf<Pair<String, String>>()
        if (!check(TokenType.RPAREN)) {
            do {
                if (isAtEnd()) break
                val pType = advance().lexeme
                // Skip & or * if present
                if (match(TokenType.IDENTIFIER) && (previous().lexeme == "&" || previous().lexeme == "*")) {
                    // reference or pointer
                }
                val pName = if (check(TokenType.IDENTIFIER)) advance().lexeme else "param_${params.size}"
                params.add(Pair(pType, pName))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RPAREN, "Expected ')' after parameters")

        val body = parseBlock()
        return CppFunction(returnType, name, params, body)
    }

    private fun parseBlock(): CppStmt.Block {
        consume(TokenType.LBRACE, "Expected '{' to start block")
        val stmts = mutableListOf<CppStmt>()
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            val stmt = parseStatement()
            if (stmt != null) stmts.add(stmt)
        }
        consume(TokenType.RBRACE, "Expected '}' to end block")
        return CppStmt.Block(stmts)
    }

    private fun parseStatement(): CppStmt? {
        if (match(TokenType.SEMICOLON)) return null

        if (check(TokenType.LBRACE)) {
            return parseBlock()
        }

        if (match(TokenType.IF)) {
            consume(TokenType.LPAREN, "Expected '(' after if")
            val condition = parseExpression()
            consume(TokenType.RPAREN, "Expected ')' after if condition")
            val thenBranch = parseStatement() ?: CppStmt.Block(emptyList())
            val elseBranch = if (match(TokenType.ELSE)) parseStatement() else null
            return CppStmt.If(condition, thenBranch, elseBranch)
        }

        if (match(TokenType.WHILE)) {
            consume(TokenType.LPAREN, "Expected '(' after while")
            val condition = parseExpression()
            consume(TokenType.RPAREN, "Expected ')' after while condition")
            val body = parseStatement() ?: CppStmt.Block(emptyList())
            return CppStmt.While(condition, body)
        }

        if (match(TokenType.DO)) {
            val body = parseStatement() ?: CppStmt.Block(emptyList())
            consume(TokenType.WHILE, "Expected 'while' after do-block")
            consume(TokenType.LPAREN, "Expected '(' after while")
            val condition = parseExpression()
            consume(TokenType.RPAREN, "Expected ')' after condition")
            match(TokenType.SEMICOLON)
            return CppStmt.DoWhile(body, condition)
        }

        if (match(TokenType.FOR)) {
            consume(TokenType.LPAREN, "Expected '(' after for")
            val init = if (match(TokenType.SEMICOLON)) null else parseStatement()
            val condition = if (check(TokenType.SEMICOLON)) null else parseExpression()
            consume(TokenType.SEMICOLON, "Expected ';' after for condition")
            val update = if (check(TokenType.RPAREN)) null else parseUpdateStatement()
            consume(TokenType.RPAREN, "Expected ')' after for clauses")
            val body = parseStatement() ?: CppStmt.Block(emptyList())
            return CppStmt.For(init, condition, update, body)
        }

        if (match(TokenType.RETURN)) {
            val expr = if (check(TokenType.SEMICOLON)) null else parseExpression()
            consume(TokenType.SEMICOLON, "Expected ';' after return")
            return CppStmt.Return(expr)
        }

        if (match(TokenType.BREAK)) {
            consume(TokenType.SEMICOLON, "Expected ';' after break")
            return CppStmt.Break
        }

        if (match(TokenType.CONTINUE)) {
            consume(TokenType.SEMICOLON, "Expected ';' after continue")
            return CppStmt.Continue
        }

        // cout << expr << expr2 ...
        if (match(TokenType.COUT) || (check(TokenType.STD) && peekAt(current + 1).type == TokenType.COLON && peekAt(current + 3).type == TokenType.COUT)) {
            if (previous().type == TokenType.STD) {
                advance() // :
                advance() // :
                advance() // cout
            }
            val expressions = mutableListOf<CppExpr>()
            while (match(TokenType.SHL)) {
                if (match(TokenType.ENDL) || (match(TokenType.STD) && match(TokenType.COLON) && match(TokenType.COLON) && match(TokenType.ENDL))) {
                    expressions.add(CppExpr.StringLit("\n"))
                } else {
                    expressions.add(parseShiftExpression())
                }
            }
            consume(TokenType.SEMICOLON, "Expected ';' after cout")
            return CppStmt.Cout(expressions)
        }

        // cin >> var1 >> var2 ...
        if (match(TokenType.CIN) || (check(TokenType.STD) && peekAt(current + 1).type == TokenType.COLON && peekAt(current + 3).type == TokenType.CIN)) {
            if (previous().type == TokenType.STD) {
                advance() // :
                advance() // :
                advance() // cin
            }
            val targets = mutableListOf<CppExpr>()
            while (match(TokenType.SHR)) {
                targets.add(parsePrimary())
            }
            consume(TokenType.SEMICOLON, "Expected ';' after cin")
            return CppStmt.Cin(targets)
        }

        // getline(cin, s);
        if (match(TokenType.GETLINE)) {
            consume(TokenType.LPAREN, "Expected '(' after getline")
            val stream = advance().lexeme // cin
            consume(TokenType.COMMA, "Expected ',' in getline")
            val target = parsePrimary()
            consume(TokenType.RPAREN, "Expected ')' after getline target")
            consume(TokenType.SEMICOLON, "Expected ';' after getline")
            return CppStmt.Getline(stream, target)
        }

        // printf("format", ...)
        if (match(TokenType.PRINTF)) {
            consume(TokenType.LPAREN, "Expected '(' after printf")
            val fmt = if (check(TokenType.STRING_LITERAL)) advance().lexeme else ""
            val args = mutableListOf<CppExpr>()
            while (match(TokenType.COMMA)) {
                args.add(parseExpression())
            }
            consume(TokenType.RPAREN, "Expected ')' after printf")
            consume(TokenType.SEMICOLON, "Expected ';' after printf")
            return CppStmt.Printf(fmt, args)
        }

        // Variable declaration: int a = 5, b;
        if (isTypeToken(peek().type)) {
            val typeToken = advance()
            val type = typeToken.lexeme

            // Handle vector<int>
            var resolvedType = type
            if (type == "vector" && match(TokenType.LT)) {
                val innerType = advance().lexeme
                match(TokenType.GT)
                resolvedType = "vector<$innerType>"
            }

            val name = advance().lexeme
            var arraySize: CppExpr? = null
            if (match(TokenType.LBRACKET)) {
                arraySize = parseExpression()
                consume(TokenType.RBRACKET, "Expected ']' after array size")
            }

            var initializer: CppExpr? = null
            if (match(TokenType.ASSIGN)) {
                initializer = parseExpression()
            }
            consume(TokenType.SEMICOLON, "Expected ';' after variable declaration")
            return CppStmt.VarDecl(resolvedType, name, initializer, arraySize)
        }

        // Assignment or ExprStmt
        val expr = parseExpression()
        if (match(TokenType.ASSIGN) || match(TokenType.PLUS_ASSIGN) || match(TokenType.MINUS_ASSIGN) ||
            match(TokenType.STAR_ASSIGN) || match(TokenType.SLASH_ASSIGN)) {
            val op = previous().type
            val value = parseExpression()
            consume(TokenType.SEMICOLON, "Expected ';' after assignment")
            return CppStmt.Assign(expr, op, value)
        }

        consume(TokenType.SEMICOLON, "Expected ';' after statement")
        return CppStmt.ExprStmt(expr)
    }

    private fun parseUpdateStatement(): CppStmt {
        val expr = parseExpression()
        if (match(TokenType.ASSIGN) || match(TokenType.PLUS_ASSIGN) || match(TokenType.MINUS_ASSIGN) ||
            match(TokenType.STAR_ASSIGN) || match(TokenType.SLASH_ASSIGN)) {
            val op = previous().type
            val value = parseExpression()
            return CppStmt.Assign(expr, op, value)
        }
        return CppStmt.ExprStmt(expr)
    }

    // Expression parsing with precedence
    private fun parseExpression(): CppExpr {
        return parseTernary()
    }

    private fun parseTernary(): CppExpr {
        var expr = parseLogicalOr()
        if (match(TokenType.QUESTION)) {
            val thenExpr = parseExpression()
            consume(TokenType.COLON, "Expected ':' in ternary operator")
            val elseExpr = parseTernary()
            expr = CppExpr.Ternary(expr, thenExpr, elseExpr)
        }
        return expr
    }

    private fun parseLogicalOr(): CppExpr {
        var expr = parseLogicalAnd()
        while (match(TokenType.OR)) {
            val op = previous().type
            val right = parseLogicalAnd()
            expr = CppExpr.BinaryOp(expr, op, right)
        }
        return expr
    }

    private fun parseLogicalAnd(): CppExpr {
        var expr = parseEquality()
        while (match(TokenType.AND)) {
            val op = previous().type
            val right = parseEquality()
            expr = CppExpr.BinaryOp(expr, op, right)
        }
        return expr
    }

    private fun parseEquality(): CppExpr {
        var expr = parseRelational()
        while (match(TokenType.EQ) || match(TokenType.NEQ)) {
            val op = previous().type
            val right = parseRelational()
            expr = CppExpr.BinaryOp(expr, op, right)
        }
        return expr
    }

    private fun parseRelational(): CppExpr {
        var expr = parseShiftExpression()
        while (match(TokenType.LT) || match(TokenType.LTE) || match(TokenType.GT) || match(TokenType.GTE)) {
            val op = previous().type
            val right = parseShiftExpression()
            expr = CppExpr.BinaryOp(expr, op, right)
        }
        return expr
    }

    private fun parseShiftExpression(): CppExpr {
        return parseAdditive()
    }

    private fun parseAdditive(): CppExpr {
        var expr = parseMultiplicative()
        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            val op = previous().type
            val right = parseMultiplicative()
            expr = CppExpr.BinaryOp(expr, op, right)
        }
        return expr
    }

    private fun parseMultiplicative(): CppExpr {
        var expr = parseUnary()
        while (match(TokenType.STAR) || match(TokenType.SLASH) || match(TokenType.PERCENT)) {
            val op = previous().type
            val right = parseUnary()
            expr = CppExpr.BinaryOp(expr, op, right)
        }
        return expr
    }

    private fun parseUnary(): CppExpr {
        if (match(TokenType.NOT) || match(TokenType.MINUS) || match(TokenType.INC) || match(TokenType.DEC)) {
            val op = previous().type
            val right = parseUnary()
            return CppExpr.UnaryOp(op, right, isPrefix = true)
        }
        return parsePostfix()
    }

    private fun parsePostfix(): CppExpr {
        var expr = parsePrimary()
        while (true) {
            if (match(TokenType.INC)) {
                expr = CppExpr.UnaryOp(TokenType.INC, expr, isPrefix = false)
            } else if (match(TokenType.DEC)) {
                expr = CppExpr.UnaryOp(TokenType.DEC, expr, isPrefix = false)
            } else if (match(TokenType.LBRACKET)) {
                val index = parseExpression()
                consume(TokenType.RBRACKET, "Expected ']' after array index")
                expr = CppExpr.ArrayAccess(expr, index)
            } else if (match(TokenType.LPAREN)) {
                val args = mutableListOf<CppExpr>()
                if (!check(TokenType.RPAREN)) {
                    do {
                        args.add(parseExpression())
                    } while (match(TokenType.COMMA))
                }
                consume(TokenType.RPAREN, "Expected ')' after function arguments")
                val funcName = (expr as? CppExpr.VarRef)?.name ?: "anon"
                expr = CppExpr.FunctionCall(funcName, args)
            } else {
                break
            }
        }
        return expr
    }

    private fun parsePrimary(): CppExpr {
        if (match(TokenType.TRUE)) return CppExpr.BoolLit(true)
        if (match(TokenType.FALSE)) return CppExpr.BoolLit(false)
        if (match(TokenType.NUMBER)) {
            val text = previous().lexeme
            val isInt = !text.contains('.')
            return CppExpr.NumberLit(text.toDoubleOrNull() ?: 0.0, isInt)
        }
        if (match(TokenType.STRING_LITERAL)) return CppExpr.StringLit(previous().lexeme)
        if (match(TokenType.CHAR_LITERAL)) {
            val c = previous().lexeme.firstOrNull() ?: ' '
            return CppExpr.CharLit(c)
        }
        if (match(TokenType.LPAREN)) {
            val expr = parseExpression()
            consume(TokenType.RPAREN, "Expected ')' after expression")
            return expr
        }
        if (match(TokenType.IDENTIFIER)) {
            return CppExpr.VarRef(previous().lexeme)
        }

        // Fallback for keywords used in expressions
        if (!isAtEnd()) {
            val tok = advance()
            return CppExpr.VarRef(tok.lexeme)
        }

        return CppExpr.NumberLit(0.0, true)
    }

    // Helper utilities
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF

    private fun peek(): Token = tokens[current]

    private fun peekAt(index: Int): Token {
        return if (index < tokens.size) tokens[index] else tokens.last()
    }

    private fun previous(): Token = tokens[current - 1]

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        // Graceful error recovery: if next is semicolon or rbrace, don't throw hard
        return peek()
    }
}
