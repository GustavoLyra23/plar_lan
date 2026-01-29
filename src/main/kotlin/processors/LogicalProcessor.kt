package processors

import models.Value

fun compare(operador: String, esquerda: Value, direita: Value): Value {
    return when {
        esquerda is Value.Integer && direita is Value.Integer -> Value.Logico(
            when (operador) {
                "<" -> esquerda.valor < direita.valor
                "<=" -> esquerda.valor <= direita.valor
                ">" -> esquerda.valor > direita.valor
                ">=" -> esquerda.valor >= direita.valor
                else -> false
            }
        )

        esquerda is Value.Real && direita is Value.Real -> Value.Logico(
            when (operador) {
                "<" -> esquerda.valor < direita.valor
                "<=" -> esquerda.valor <= direita.valor
                ">" -> esquerda.valor > direita.valor
                ">=" -> esquerda.valor >= direita.valor
                else -> false
            }
        )

        esquerda is Value.Real && direita is Value.Integer -> Value.Logico(
            when (operador) {
                "<" -> esquerda.valor < direita.valor.toDouble()
                "<=" -> esquerda.valor <= direita.valor.toDouble()
                ">" -> esquerda.valor > direita.valor.toDouble()
                ">=" -> esquerda.valor >= direita.valor.toDouble()
                else -> false
            }
        )

        esquerda is Value.Integer && direita is Value.Real -> Value.Logico(
            when (operador) {
                "<" -> esquerda.valor.toDouble() < direita.valor
                "<=" -> esquerda.valor.toDouble() <= direita.valor
                ">" -> esquerda.valor.toDouble() > direita.valor
                ">=" -> esquerda.valor.toDouble() >= direita.valor
                else -> false
            }
        )

        esquerda is Value.Text && direita is Value.Text -> Value.Logico(
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

fun areEqual(esquerda: Value, direita: Value): Boolean {
    return when {
        esquerda is Value.Integer && direita is Value.Integer -> esquerda.valor == direita.valor
        esquerda is Value.Real && direita is Value.Real -> esquerda.valor == direita.valor
        esquerda is Value.Real && direita is Value.Integer -> esquerda.valor == direita.valor.toDouble()
        esquerda is Value.Integer && direita is Value.Real -> esquerda.valor.toDouble() == direita.valor
        esquerda is Value.Text && direita is Value.Text -> esquerda.valor == direita.valor
        esquerda is Value.Logico && direita is Value.Logico -> esquerda.valor == direita.valor
        esquerda is Value.Object && direita is Value.Object -> esquerda === direita
        esquerda is Value.List && direita is Value.List -> esquerda === direita
        esquerda is Value.Map && direita is Value.Map -> esquerda === direita
        else -> false
    }
}
