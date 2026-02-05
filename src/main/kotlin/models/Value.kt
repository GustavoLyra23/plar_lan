package models

import models.enums.BasicTypes
import org.gustavolyra.PlarParser

sealed class Value {
    data class Integer(val value: Int) : Value()

    data class Real(val value: Double) : Value()

    data class Text(val value: String) : Value()

    data class Logic(val value: Boolean) : Value()

    data class List(
        val elements: MutableList<Value> = mutableListOf(),
        var size: Int
    ) : Value()

    data class Map(val elements: MutableMap<Value, Value> = mutableMapOf()) : Value()

    data class Object(
        val klass: String,
        val fields: MutableMap<String, Value>,
        val superClass: String? = null,
        val interfaces: kotlin.collections.List<String> = listOf()
    ) : Value()


    data class Param(val name: String, val type: String)

    data class Interface(val name: String, val signatures: kotlin.collections.Map<String, Method>) : Value()

    class Method(
        val name: String,
        val parameters: kotlin.collections.List<Param>,
        val returnType: String? = null
    )

    data class Fun(
        val name: String,
        //TODO: find a better use case for this field...
        val declaration: PlarParser.DeclaracaoFuncaoContext? = null,
        var returnType: String? = null,
        val closure: Environment,
        var implementation: ((kotlin.collections.List<Value>) -> Value)? = null,
    ) : Value()

    object Null : Value()

    fun typeString(): String = when (this) {
        is Integer -> BasicTypes.INTEIRO.tipo
        is Real -> BasicTypes.REAL.tipo
        is Text -> BasicTypes.TEXTO.tipo
        is Logic -> BasicTypes.LOGICO.tipo
        is Object -> klass
        is Fun -> name
        is Interface -> name
        is Map -> "Mapa"
        is List -> "Lista"
        else -> BasicTypes.Nulo.tipo
    }

    override fun toString(): String = when (this) {
        is Integer -> value.toString()
        is Real -> value.toString()
        is Text -> value
        is Logic -> if (value) "verdadeiro" else "falso"
        is Object -> "[Objeto $klass]"
        is Fun -> "[funcao $name]"
        Null -> "nulo"
        is Interface -> "[Interface]"
        else -> super.toString()
    }
}
