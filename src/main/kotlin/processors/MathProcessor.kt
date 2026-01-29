package processors

import extractValueToString
import models.Value

fun processAdd(operator: String?, left: Value, right: Value): Value {
    return when (operator) {
        "+" -> resolveAddOperator(left, right)
        "-" -> solveSubOperator(left, right)
        else -> {
            throw RuntimeException("Operador desconhecido: $operator")
        }
    }
}

fun resolveAddOperator(left: Value, right: Value): Value {
    return when {
        left is Value.Text || right is Value.Text -> {
            val leftStr = extractValueToString(left)
            val rightStr = extractValueToString(right)
            Value.Text(leftStr + rightStr)
        }

        left is Value.Integer && right is Value.Integer -> {
            val resultado = Value.Integer(left.valor + right.valor)
            resultado
        }

        left is Value.Real && right is Value.Real -> {
            val resultado = Value.Real(left.valor + right.valor)
            resultado
        }

        left is Value.Integer && right is Value.Real -> {
            val resultado = Value.Real(left.valor.toDouble() + right.valor)
            resultado
        }

        left is Value.Real && right is Value.Integer -> {
            val resultado = Value.Real(left.valor + right.valor.toDouble())
            resultado
        }

        else -> {
            throw RuntimeException("Operador '+' não suportado para ${left::class.simpleName} e ${right::class.simpleName}")
        }
    }
}

fun solveSubOperator(left: Value, right: Value): Value {
    return when {
        left is Value.Integer && right is Value.Integer -> {
            val resultado = Value.Integer(left.valor - right.valor)
            resultado
        }

        left is Value.Real && right is Value.Real -> {
            val resultado = Value.Real(left.valor - right.valor)
            resultado
        }

        left is Value.Integer && right is Value.Real -> {
            val resultado = Value.Real(left.valor.toDouble() - right.valor)
            resultado
        }

        left is Value.Real && right is Value.Integer -> {
            val resultado = Value.Real(left.valor - right.valor.toDouble())
            resultado
        }

        else -> {
            throw RuntimeException("Operador '-' não suportado para ${left::class.simpleName} e ${right::class.simpleName}")
        }
    }
}

fun processMultiplication(
    operador: String?, left: Value, right: Value
): Value {
    return when (operador) {
        "*" -> solveMultiplicationOperator(left, right)
        "/" -> solveDivisionOperator(left, right)
        "%" -> solveModuleOperator(left, right)
        else -> {
            throw RuntimeException("Operador desconhecido: $operador")
        }
    }
}

fun solveMultiplicationOperator(left: Value, right: Value): Value {
    return when {
        left is Value.Integer && right is Value.Integer -> Value.Integer(left.valor * right.valor)
        left is Value.Real && right is Value.Real -> Value.Real(left.valor * right.valor)
        left is Value.Integer && right is Value.Real -> Value.Real(left.valor.toDouble() * right.valor)
        left is Value.Real && right is Value.Integer -> Value.Real(left.valor * right.valor.toDouble())
        else -> throw RuntimeException("Operador '*' não suportado para ${left::class.simpleName} e ${right::class.simpleName}")
    }
}

fun solveModuleOperator(left: Value, right: Value): Value {
    return when {
        left is Value.Integer && right is Value.Integer -> {
            if (right.valor == 0) throw RuntimeException("Módulo por zero")
            Value.Integer(left.valor % right.valor)
        }

        left is Value.Real && right is Value.Real -> {
            if (right.valor == 0.0) throw RuntimeException("Módulo por zero")
            Value.Real(left.valor % right.valor)
        }

        left is Value.Integer && right is Value.Real -> {
            if (right.valor == 0.0) throw RuntimeException("Módulo por zero")
            Value.Real(left.valor.toDouble() % right.valor)
        }

        left is Value.Real && right is Value.Integer -> {
            if (right.valor == 0) throw RuntimeException("Módulo por zero")
            Value.Real(left.valor % right.valor.toDouble())
        }

        else -> throw RuntimeException("Operador '%' não suportado para ${left::class.simpleName} e ${right::class.simpleName}")
    }
}

fun solveDivisionOperator(left: Value, right: Value): Value {
    return when {
        (right is Value.Integer && right.valor == 0) || (right is Value.Real && right.valor == 0.0) -> throw RuntimeException(
            "Divisão por zero"
        )

        left is Value.Integer && right is Value.Integer -> if (left.valor % right.valor == 0) Value.Integer(
            left.valor / right.valor
        ) else Value.Real(left.valor.toDouble() / right.valor)

        left is Value.Real && right is Value.Real -> Value.Real(left.valor / right.valor)
        left is Value.Integer && right is Value.Real -> Value.Real(left.valor.toDouble() / right.valor)
        left is Value.Real && right is Value.Integer -> Value.Real(left.valor / right.valor.toDouble())
        else -> throw RuntimeException("Operador '/' não suportado para ${left::class.simpleName} e ${right::class.simpleName}")
    }
}
