import models.Value
import org.gustavolyra.PlarParser

fun extractValueToPrint(value: Value): String {
    return when (value) {
        is Value.List -> {
            val elements = value.elements.map { extractValueToPrint(it) }
            "[${elements.joinToString(", ")}]"
        }

        is Value.Map -> {
            val entries = value.elements.map { (key, value) ->
                "${extractValueToPrint(key)}: ${extractValueToPrint(value)}"
            }
            "[[${entries.joinToString(", ")}]]"
        }

        is Value.Text -> value.value
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
            val elements = value.elements.map { extractValueToString(it) }
            "[${elements.joinToString(", ")}]"
        }

        is Value.Map -> {
            val entries = value.elements.map { (chave, valor) ->
                "${extractValueToString(chave)}: ${extractValueToString(valor)}"
            }
            "[[${entries.joinToString(", ")}]]"
        }

        else -> value.toString()
    }
}

fun isDot(ctx: PlarParser.ChamadaContext, i: Int) = i < ctx.childCount && ctx.getChild(i).text == "."
