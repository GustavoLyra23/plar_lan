import models.Valor
import org.gustavolyra.portugolpp.PortugolPPParser.ChamadaContext

fun extrairValorParaImpressao(valor: Valor): String {
    return when (valor) {
        is Valor.Lista -> {
            val elementos = valor.elementos.map { extrairValorParaImpressao(it) }
            "[${elementos.joinToString(", ")}]"
        }

        is Valor.Mapa -> {
            val entradas = valor.elementos.map { (chave, valor) ->
                "${extrairValorParaImpressao(chave)}: ${extrairValorParaImpressao(valor)}"
            }
            "[[${entradas.joinToString(", ")}]]"
        }

        is Valor.Texto -> "\"${valor.valor}\""
        is Valor.Inteiro -> valor.valor.toString()
        is Valor.Real -> valor.valor.toString()
        is Valor.Logico -> if (valor.valor) "verdadeiro" else "falso"
        is Valor.Objeto -> "[Objeto ${valor.klass}]"
        is Valor.Funcao -> "[fun ${valor.nome}]"
        Valor.Nulo -> "nulo"
        else -> valor.toString()
    }
}

fun extrairValorString(valor: Valor): String {
    return when (valor) {
        is Valor.Inteiro -> valor.valor.toString()
        is Valor.Real -> valor.valor.toString()
        is Valor.Logico -> if (valor.valor) "verdadeiro" else "falso"
        is Valor.Nulo -> "nulo"
        is Valor.Texto -> valor.valor
        is Valor.Lista -> {
            val elementos = valor.elementos.map { extrairValorString(it) }
            "[${elementos.joinToString(", ")}]"
        }

        is Valor.Mapa -> {
            val entradas = valor.elementos.map { (chave, valor) ->
                "${extrairValorString(chave)}: ${extrairValorString(valor)}"
            }
            "[[${entradas.joinToString(", ")}]]"
        }

        else -> valor.toString()
    }
}

fun ehPonto(ctx: ChamadaContext, i: Int) =
    i < ctx.childCount && ctx.getChild(i).text == "."
