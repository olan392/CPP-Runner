package com.example

import com.example.data.model.OutputType
import com.example.engine.CppInterpreter
import com.example.engine.CppParser
import com.example.engine.CppTokenizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCppOutputAndInput() = runBlocking {
        val code = """
            #include <iostream>
            using namespace std;
            
            int main() {
                cout << "Enter number: ";
                int x;
                cin >> x;
                int result = x * 2;
                cout << "Double is: " << result << endl;
                return 0;
            }
        """.trimIndent()

        val tokens = CppTokenizer(code).tokenize()
        val program = CppParser(tokens).parse()

        val outputList = mutableListOf<String>()
        val interpreter = CppInterpreter()

        val exitCode = interpreter.execute(
            program = program,
            onOutput = { text, type ->
                outputList.add(text)
            },
            onInputPrompt = { prompt, varName, expectedType ->
                "21"
            }
        )

        assertEquals(0, exitCode)
        val fullOutput = outputList.joinToString("")
        assertTrue("Output should contain 'Double is: 42'", fullOutput.contains("Double is: 42"))
    }
}
