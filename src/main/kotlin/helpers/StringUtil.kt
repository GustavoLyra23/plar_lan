import models.Value
import org.gustavolyra.portugolpp.PortugolPPParser.ChamadaContext

fun extractValueToPrint(value: Value): String {
    return when (value) {
        is Value.List -> {
            val elementos = value.elements.map { extractValueToPrint(it) }
            "[${elementos.joinToString(", ")}]"
        }

        is Value.Map -> {
            val entradas = value.elements.map { (chave, valor) ->
                "${extractValueToPrint(chave)}: ${extractValueToPrint(valor)}"
            }
            "[[${entradas.joinToString(", ")}]]"
        }

        is Value.Text -> "\"${value.value}\""
        is Value.Integer -> value.value.toString()
        is Value.Real -> value.value.toString()
        is Value.Logic -> if (value.value) "verdadeiro" else "falso"
        is Value.Object -> "[Objeto ${value.klass}]"
        is Value.Fun -> "[fun ${value.name}]"
        Value.Null -> "nulo"
        else -> value.toString()
    }
}

fun extractValueToString(value: Value): String {
    return when (value) {
        is Value.Integer -> value.value.toString()
        is Value.Real -> value.value.toString()
        is Value.Logic -> if (value.value) "verdadeiro" else "falso"
        is Value.Null -> "nulo"
        is Value.Text -> value.value
        is Value.List -> {
            val elementos = value.elements.map { extractValueToString(it) }
            "[${elementos.joinToString(", ")}]"
        }

        is Value.Map -> {
            val entradas = value.elements.map { (chave, valor) ->
                "${extractValueToString(chave)}: ${extractValueToString(valor)}"
            }
            "[[${entradas.joinToString(", ")}]]"
        }

        else -> value.toString()
    }
}

fun isDot(ctx: ChamadaContext, i: Int) =
    i < ctx.childCount && ctx.getChild(i).text == "."
