package models

import models.enums.BasicTypes
import org.gustavolyra.portugolpp.PortugolPPParser

sealed class Value {

    data class Integer(val valor: Int) : Value()

    data class Real(val valor: Double) : Value()

    data class Text(val valor: String) : Value()

    data class Logico(val valor: Boolean) : Value()

    data class List(
        val elementos: MutableList<Value> = mutableListOf(),
        var tamanho: Int
    ) : Value()

    data class Map(val elementos: MutableMap<Value, Value> = mutableMapOf()) : Value()

    data class Object(
        val klass: String,
        val campos: MutableMap<String, Value>,
        val superClasse: String? = null,
        val interfaces: kotlin.collections.List<String> = listOf()
    ) : Value()


    data class Param(val nome: String, val tipo: String)

    data class Interface(val nome: String, val assinaturas: kotlin.collections.Map<String, Method>) : Value()

    class Method(val nome: String, val parametros: kotlin.collections.List<Param>, val tipoRetorno: String? = null)

    data class Funcao(
        val nome: String,
        //TODO: repensar estrategia de uso desta variavel...
        val declaracao: PortugolPPParser.DeclaracaoFuncaoContext? = null,
        val tipoRetorno: String? = null,
        val closure: Environment,
        val implementacao: ((kotlin.collections.List<Value>) -> Value)? = null,
    ) : Value()

    object Null : Value()

    fun typeString(): String = when (this) {
        is Integer -> BasicTypes.INTEIRO.tipo
        is Real -> BasicTypes.REAL.tipo
        is Text -> BasicTypes.TEXTO.tipo
        is Logico -> BasicTypes.LOGICO.tipo
        is Object -> klass
        is Funcao -> nome
        is Interface -> nome
        is List -> "Lista"
        is Map -> "Mapa"
        //TODO: rever else case
        else -> BasicTypes.Nulo.tipo
    }

    override fun toString(): String = when (this) {
        is Integer -> valor.toString()
        is Real -> valor.toString()
        is Text -> valor
        is Logico -> if (valor) "verdadeiro" else "falso"
        is Object -> "[Objeto $klass]"
        is Funcao -> "[função $nome]"
        Null -> "nulo"
        is Interface -> "[Interface]"
        else -> super.toString()
    }
}
