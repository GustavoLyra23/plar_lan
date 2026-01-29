package processors

import models.Value

fun compare(operador: String, esquerda: Value, direita: Value): Value {
    return when {
        esquerda is Value.Integer && direita is Value.Integer -> Value.Logic(
            when (operador) {
                "<" -> esquerda.value < direita.value
                "<=" -> esquerda.value <= direita.value
                ">" -> esquerda.value > direita.value
                ">=" -> esquerda.value >= direita.value
                else -> false
            }
        )

        esquerda is Value.Real && direita is Value.Real -> Value.Logic(
            when (operador) {
                "<" -> esquerda.value < direita.value
                "<=" -> esquerda.value <= direita.value
                ">" -> esquerda.value > direita.value
                ">=" -> esquerda.value >= direita.value
                else -> false
            }
        )

        esquerda is Value.Real && direita is Value.Integer -> Value.Logic(
            when (operador) {
                "<" -> esquerda.value < direita.value.toDouble()
                "<=" -> esquerda.value <= direita.value.toDouble()
                ">" -> esquerda.value > direita.value.toDouble()
                ">=" -> esquerda.value >= direita.value.toDouble()
                else -> false
            }
        )

        esquerda is Value.Integer && direita is Value.Real -> Value.Logic(
            when (operador) {
                "<" -> esquerda.value.toDouble() < direita.value
                "<=" -> esquerda.value.toDouble() <= direita.value
                ">" -> esquerda.value.toDouble() > direita.value
                ">=" -> esquerda.value.toDouble() >= direita.value
                else -> false
            }
        )

        esquerda is Value.Text && direita is Value.Text -> Value.Logic(
            when (operador) {
                "<" -> esquerda.value < direita.value
                "<=" -> esquerda.value <= direita.value
                ">" -> esquerda.value > direita.value
                ">=" -> esquerda.value >= direita.value
                else -> false
            }
        )

        else -> throw RuntimeException("Operador '$operador' não suportado para ${esquerda::class.simpleName} e ${direita::class.simpleName}")
    }
}

fun areEqual(esquerda: Value, direita: Value): Boolean {
    return when {
        esquerda is Value.Integer && direita is Value.Integer -> esquerda.value == direita.value
        esquerda is Value.Real && direita is Value.Real -> esquerda.value == direita.value
        esquerda is Value.Real && direita is Value.Integer -> esquerda.value == direita.value.toDouble()
        esquerda is Value.Integer && direita is Value.Real -> esquerda.value.toDouble() == direita.value
        esquerda is Value.Text && direita is Value.Text -> esquerda.value == direita.value
        esquerda is Value.Logic && direita is Value.Logic -> esquerda.value == direita.value
        esquerda is Value.Object && direita is Value.Object -> esquerda === direita
        esquerda is Value.List && direita is Value.List -> esquerda === direita
        esquerda is Value.Map && direita is Value.Map -> esquerda === direita
        else -> false
    }
}
