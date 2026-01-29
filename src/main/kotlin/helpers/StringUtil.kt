import models.Value
import org.gustavolyra.portugolpp.PortugolPPParser.ChamadaContext

fun extractValueToPrint(value: Value): String {
    return when (value) {
        is Value.List -> {
            val elementos = value.elementos.map { extractValueToPrint(it) }
            "[${elementos.joinToString(", ")}]"
        }

        is Value.Map -> {
            val entradas = value.elementos.map { (chave, valor) ->
                "${extractValueToPrint(chave)}: ${extractValueToPrint(valor)}"
            }
            "[[${entradas.joinToString(", ")}]]"
        }

        is Value.Text -> "\"${value.valor}\""
        is Value.Integer -> value.valor.toString()
        is Value.Real -> value.valor.toString()
        is Value.Logico -> if (value.valor) "verdadeiro" else "falso"
        is Value.Object -> "[Objeto ${value.klass}]"
        is Value.Funcao -> "[fun ${value.nome}]"
        Value.Null -> "nulo"
        else -> value.toString()
    }
}

fun extractValueToString(value: Value): String {
    return when (value) {
        is Value.Integer -> value.valor.toString()
        is Value.Real -> value.valor.toString()
        is Value.Logico -> if (value.valor) "verdadeiro" else "falso"
        is Value.Null -> "nulo"
        is Value.Text -> value.valor
        is Value.List -> {
            val elementos = value.elementos.map { extractValueToString(it) }
            "[${elementos.joinToString(", ")}]"
        }

        is Value.Map -> {
            val entradas = value.elementos.map { (chave, valor) ->
                "${extractValueToString(chave)}: ${extractValueToString(valor)}"
            }
            "[[${entradas.joinToString(", ")}]]"
        }

        else -> value.toString()
    }
}

fun isDot(ctx: ChamadaContext, i: Int) =
    i < ctx.childCount && ctx.getChild(i).text == "."
