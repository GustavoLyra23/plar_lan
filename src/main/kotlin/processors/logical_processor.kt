package processors

import models.Valor

fun comparar(operador: String, esquerda: Valor, direita: Valor): Valor {
    return when {
        esquerda is Valor.Inteiro && direita is Valor.Inteiro -> Valor.Logico(
            when (operador) {
                "<" -> esquerda.valor < direita.valor
                "<=" -> esquerda.valor <= direita.valor
                ">" -> esquerda.valor > direita.valor
                ">=" -> esquerda.valor >= direita.valor
                else -> false
            }
        )

        esquerda is Valor.Real && direita is Valor.Real -> Valor.Logico(
            when (operador) {
                "<" -> esquerda.valor < direita.valor
                "<=" -> esquerda.valor <= direita.valor
                ">" -> esquerda.valor > direita.valor
                ">=" -> esquerda.valor >= direita.valor
                else -> false
            }
        )

        esquerda is Valor.Real && direita is Valor.Inteiro -> Valor.Logico(
            when (operador) {
                "<" -> esquerda.valor < direita.valor.toDouble()
                "<=" -> esquerda.valor <= direita.valor.toDouble()
                ">" -> esquerda.valor > direita.valor.toDouble()
                ">=" -> esquerda.valor >= direita.valor.toDouble()
                else -> false
            }
        )

        esquerda is Valor.Inteiro && direita is Valor.Real -> Valor.Logico(
            when (operador) {
                "<" -> esquerda.valor.toDouble() < direita.valor
                "<=" -> esquerda.valor.toDouble() <= direita.valor
                ">" -> esquerda.valor.toDouble() > direita.valor
                ">=" -> esquerda.valor.toDouble() >= direita.valor
                else -> false
            }
        )

        esquerda is Valor.Texto && direita is Valor.Texto -> Valor.Logico(
            when (operador) {
                "<" -> esquerda.valor < direita.valor
                "<=" -> esquerda.valor <= direita.valor
                ">" -> esquerda.valor > direita.valor
                ">=" -> esquerda.valor >= direita.valor
                else -> false
            }
        )

        else -> throw RuntimeException("Operador '$operador' não suportado para ${esquerda::class.simpleName} e ${direita::class.simpleName}")
    }
}

fun saoIguais(esquerda: Valor, direita: Valor): Boolean {
    return when {
        esquerda is Valor.Inteiro && direita is Valor.Inteiro -> esquerda.valor == direita.valor
        esquerda is Valor.Real && direita is Valor.Real -> esquerda.valor == direita.valor
        esquerda is Valor.Real && direita is Valor.Inteiro -> esquerda.valor == direita.valor.toDouble()
        esquerda is Valor.Inteiro && direita is Valor.Real -> esquerda.valor.toDouble() == direita.valor
        esquerda is Valor.Texto && direita is Valor.Texto -> esquerda.valor == direita.valor
        esquerda is Valor.Logico && direita is Valor.Logico -> esquerda.valor == direita.valor
        esquerda is Valor.Objeto && direita is Valor.Objeto -> esquerda === direita
        esquerda is Valor.Lista && direita is Valor.Lista -> esquerda === direita
        esquerda is Valor.Mapa && direita is Valor.Mapa -> esquerda === direita
        else -> false
    }
}
