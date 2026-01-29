package processors

import models.Ambiente
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse

class FunctionProcessorTest {
    @Test
    fun `retornoFuncaoInvalido deve retornar false quando input for valido`() {
        val input = "Texto";
        val res = retornoFuncaoInvalido(input, Ambiente())
        assertFalse(res)
    }

    @Test
    fun `retornoFuncaoInvalido deve retornar true quando input for invalido`() {
        val invalidInput = "Invalid";
        val res = retornoFuncaoInvalido(invalidInput, Ambiente())
        assertTrue(res)
    }

    @Test
    fun `retornoFuncaoInvalido deve retornar false quando classe existir no Ambiente`() {
        val ambiente = Ambiente()
        val className = "classeTeste";
        ambiente.definirClasse(className, null)
        val res = retornoFuncaoInvalido(className, ambiente)
        assertFalse(res)
    }

    @Test
    fun `retornoFuncaoInvalido deve retornar true quando classe nao existir no Ambiente`() {
        val className = "classeTeste"
        val res = retornoFuncaoInvalido(className, Ambiente())
        assertTrue(res)
    }
}
