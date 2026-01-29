package processors

import models.Environment
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse

class FunctionProcessorTest {
    @Test
    fun `retornoFuncaoInvalido deve retornar false quando input for valido`() {
        val input = "Texto";
        val res = isReturnInvalid(input, Environment())
        assertFalse(res)
    }

    @Test
    fun `retornoFuncaoInvalido deve retornar true quando input for invalido`() {
        val invalidInput = "Invalid";
        val res = isReturnInvalid(invalidInput, Environment())
        assertTrue(res)
    }

    @Test
    fun `retornoFuncaoInvalido deve retornar false quando classe existir no Ambiente`() {
        val environment = Environment()
        val className = "classeTeste";
        environment.defineClass(className, null)
        val res = isReturnInvalid(className, environment)
        assertFalse(res)
    }

    @Test
    fun `retornoFuncaoInvalido deve retornar true quando classe nao existir no Ambiente`() {
        val className = "classeTeste"
        val res = isReturnInvalid(className, Environment())
        assertTrue(res)
    }
}
