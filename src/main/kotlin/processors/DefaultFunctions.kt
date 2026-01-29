package processors

import extractValueToPrint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import models.Environment
import models.Value
import models.errors.ArquivoException
import processors.FileIOProcessor.writeFile
import processors.FileIOProcessor.readFile
import java.util.*

fun defineDefaultFunctions(global: Environment) {
    registerIOFunctions(global)
    registerThreadFunctions(global)
    registerExceptionsFunctions(global)
    registerCollectionsFunctions(global)
}

fun registerIOFunctions(global: Environment) {
    global.define("ler", Value.Funcao("ler", null, "Texto", global) { args ->
        Scanner(System.`in`).nextLine().let { Value.Text(it) }
    })
    global.define("readFile", Value.Funcao("readFile", null, "Texto", global) { args ->
        if (args.isEmpty()) throw RuntimeException("Função readFile requer um argumento (caminho do arquivo)")
        if (args.size > 1) throw RuntimeException("Função readFile aceita apenas um argumento")

        val argVal = args[0]
        if (argVal !is Value.Text) {
            throw RuntimeException("Argumento deve ser um texto (caminho do arquivo)")
        }

        try {
            Value.Text(readFile(argVal.valor))
        } catch (e: Exception) {
            throw ArquivoException("Erro ao ler arquivo '${argVal.valor}': ${e.message}")
        }
    })
    global.define("writeFile", Value.Funcao("writeFile", null, null, global) { args ->
        require(args.size in 2..3) { "Função writeFile requer 2 ou 3 argumentos" }
        val (path, data) = args.take(2)
        val append = args.getOrNull(2)

        require(path is Value.Text && data is Value.Text) {
            "Os dois primeiros argumentos devem ser do tipo Texto"
        }
        when (append) {
            null -> writeFile(path.valor, data.valor)
            is Value.Logico -> writeFile(
                path.valor, data.valor, append.valor
            )

            else -> throw RuntimeException("O terceiro argumento deve ser do tipo Logico")
        }
        Value.Null
    })
    global.define("escrever", Value.Funcao("escrever", null, null, global) { args ->
        val valores = args.map { extractValueToPrint(it) }
        println(valores.joinToString(" "))
        Value.Null
    })
    global.define("imprimir", Value.Funcao("imprimir", null, null, global) { args ->
        val valores = args.map { extractValueToPrint(it) }
        println(valores.joinToString(" "))
        Value.Null
    })
}

fun registerThreadFunctions(global: Environment) {
    global.define("executar", Value.Funcao("executar", null, null, global) { args ->
        if (args.isEmpty() || args[0] !is Value.Funcao) throw RuntimeException("Argumento invalido para a funcao.")
        val funcaoParaExecutar = args[0] as Value.Funcao
        val argumentosReais = args.drop(1)
        //run sincrono...
        runBlocking {
            launch {
                try {
                    funcaoParaExecutar.implementacao!!.invoke(argumentosReais)
                } catch (e: Exception) {
                    println("Erro na execucao da thread: ${e.message}")
                }
            }.join()
        }
        Value.Null
    })
    global.define("dormir", Value.Funcao("aguardar", null, null, global) { args ->
        if (args.isEmpty()) throw RuntimeException("Função aguardar requer um argumento (milissegundos)")
        val tempo = args[0]
        if (tempo !is Value.Integer) throw RuntimeException("Argumento deve ser um número inteiro (milissegundos)")
        runBlocking {
            delay(tempo.valor.toLong())
        }
        Value.Null
    })
}

fun registerExceptionsFunctions(global: Environment) {
    global.define("jogarError", Value.Funcao("jogarError", null, null, global) { args ->
        if (args.isEmpty()) {
            throw RuntimeException("Função jogarError requer um argumento (mensagem de erro)")
        }
        val mensagem = args[0]
        if (mensagem !is Value.Text) {
            throw RuntimeException("Argumento deve ser um texto (mensagem de erro)")
        }
        throw RuntimeException(mensagem.valor)
    })
}

fun registerCollectionsFunctions(global: Environment) {
    global.define("tamanho", Value.Funcao("tamanho", null, "Inteiro", global) { args ->
        if (args.isEmpty()) {
            throw RuntimeException("Função tamanho requer um argumento (lista, mapa ou texto)")
        }

        when (val arg = args[0]) {
            is Value.List -> Value.Integer(arg.elementos.size)
            is Value.Map -> Value.Integer(arg.elementos.size)
            is Value.Text -> Value.Integer(arg.valor.length)
            else -> throw RuntimeException("Função tamanho só funciona com listas, mapas ou textos")
        }
    })
}



