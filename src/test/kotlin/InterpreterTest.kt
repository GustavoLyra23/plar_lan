import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.gustavolyra.portugolpp.Interpreter
import org.gustavolyra.portugolpp.PortugolPPLexer
import org.gustavolyra.portugolpp.PortugolPPParser
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InterpreterTest {

    private lateinit var interpreter: Interpreter
    private val originalOut = System.out

    @BeforeTest
    fun setUp() {
        interpreter = Interpreter()
    }

    @AfterTest
    fun tearDown() {
        System.setOut(originalOut)
    }

    @Test
    fun `escrever function should print the correct message`() {
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        val code = """
            escrever("teste");
        """.trimIndent()

        val lexer = PortugolPPLexer(CharStreams.fromString(code))
        val parser = PortugolPPParser(CommonTokenStream(lexer))
        val tree = parser.programa()

        val result = interpreter.visit(tree)

        val output = outputStream.toString().trim()

        assertEquals("teste", output)
        assertEquals(null, result)
    }
}
